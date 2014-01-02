package nl.ordina.java8.control.providers;

import nl.ordina.java8.control.LinkParser;
import nl.ordina.java8.control.SearchProvider;
import nl.ordina.java8.control.SearchProviderService;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

public class SearchProviderFactory {
    public static final String CLASS_PATH_PROPERTIES = "/etc/providers.properties";
    public static final String LINKPARSER_CLASS_KEY = "linkparser.class";

    private static final Logger LOG = getLogger(lookup().lookupClass().getName());

    private Set<SearchProvider> searchProviders;

    public Set<SearchProvider> getSearchProviders() {
        return searchProviders;
    }

    @PostConstruct
    private void createProviders() {
        final Properties properties = new Properties();
        try (InputStream inputStream = SearchProviderService.class.getResourceAsStream(CLASS_PATH_PROPERTIES)) {
            properties.load(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        createProviders(properties);
    }

    private void createProviders(final Properties properties) {
        // convert stupid properties to String-typed key Map
        Map<String, Object> entries = properties.entrySet().stream()
                        .collect(toMap(entry -> (String) entry.getKey(), entry -> entry.getValue()));

        searchProviders = properties.stringPropertyNames().stream()
                .map(s -> s.substring(0, s.indexOf('.')))
                .distinct()
                .map(id -> createProvider(entries, id))
                .collect(toSet());
        LOG.log(Level.INFO, "Aantal SearchProviders: {0}", searchProviders.size());
    }

    private SearchProvider createProvider(Map<String, Object> entries, String providerId) {
        Map<String, Object> providerProperties = filterAndStripProviderId(entries, providerId);

        addLinkParserToProviderProperties(providerProperties);

        return createProvider(providerProperties);
    }

    private Map<String, Object> filterAndStripProviderId(Map<String, Object> entries, String providerId) {
        return entries.entrySet().stream()
                .filter(e -> e.getKey().startsWith(providerId))
                .collect(toMap(entry -> entry.getKey().substring(providerId.length() + 1),
                        entry -> entry.getValue()));
    }

    /**
     * Add a new created LinkParser to the provider properties.
     *
     * @param providerProperties properties of the search provider
     */
    private void addLinkParserToProviderProperties(Map<String, Object> providerProperties) {
        String linkparserClass = (String) providerProperties.get(LINKPARSER_CLASS_KEY);
        Objects.requireNonNull(linkparserClass, "Fully qualified name of link parser (key = " + LINKPARSER_CLASS_KEY + ") is required.");

        Class<?> linkparser = classForName(linkparserClass);
        LinkParser parser = (LinkParser) createNewInstance(linkparser.getConstructors()[0]);
        providerProperties.put("linkParser", parser);
    }

    /**
     * Read all parameters of the constructor and match them with the configured provider properties
     * @param providerProperties
     * @return the created search provider
     */
    private SearchProvider createProvider(Map<String, Object> providerProperties) {
        String fqName = (String) providerProperties.get("class");
        Class<?> clazz = classForName(fqName);
        // TODO lift restriction for only one constructor

        Parameter[] parameters = clazz.getConstructors()[0].getParameters();
        Object[] arguments = new Object[clazz.getConstructors()[0].getParameterCount()];
        for (int i = 0; i < parameters.length; i++) {
            if (!parameters[i].isNamePresent())
                throw new IllegalStateException("Sources not compiled with compiler flag '-parameters'!! Please fix...");
            arguments[i] = providerProperties.get(parameters[i].getName());
        }

        SearchProvider newInstance = (SearchProvider) createNewInstance(clazz.getConstructors()[0], arguments);

        LOG.log(FINE, "Created SearchProvider: {0}", newInstance);
        return newInstance;
    }

    private Object createNewInstance(Constructor<?> constructor, Object... arguments) {
        try {
            return constructor.newInstance(arguments);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            LOG.log(WARNING, "Error bij constructie van " + constructor.getDeclaringClass().getName(), e);
            throw new IllegalStateException(e);
        }
    }

    private Class<?> classForName(String fqName) {
        try {
            return Class.forName(fqName);
        } catch (ClassNotFoundException e) {
            LOG.log(WARNING, "ClassNotFoundException bij laden " + fqName, e);
            throw new IllegalStateException(e);
        }
    }
}
