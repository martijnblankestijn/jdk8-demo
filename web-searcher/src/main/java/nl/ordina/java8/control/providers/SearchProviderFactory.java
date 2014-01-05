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
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.Arrays.stream;
import static java.util.Collections.unmodifiableMap;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.FINEST;
import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

public class SearchProviderFactory {
  public static final String CLASS_PATH_PROPERTIES = "/etc/providers.properties";
  public static final String LINKPARSER_CLASS_KEY = "linkparser.class";

  private static final Logger LOG = getLogger(lookup().lookupClass().getName());

  private Set<SearchProvider> searchProviders;
  private Map<String, String> properties;

  public Set<SearchProvider> getSearchProviders() {
    return searchProviders;
  }

  @PostConstruct
  private void postConstruct() {
    properties =  unmodifiableMap(loadProperties());
    createProviders();
  }

  private Map<String, String> loadProperties() {
    final Properties properties = new Properties();
    try (InputStream inputStream = SearchProviderService.class.getResourceAsStream(CLASS_PATH_PROPERTIES)) {
      properties.load(inputStream);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
    return // convert stupid properties to String-typed key Map
      properties.entrySet().stream()
        .collect(toMap(entry -> (String) entry.getKey(), entry -> (String) entry.getValue()));
  }

  private void createProviders() {

    searchProviders = properties.entrySet().stream()
      .map(s -> s.getKey().substring(0, s.getKey().indexOf('.')))
      .distinct()
      .map(id -> createProvider(id))
      .collect(toSet());
    LOG.log(Level.INFO, "Aantal SearchProviders: {0}", searchProviders.size());
  }

  private SearchProvider createProvider(String providerId) {
    Map<String, String> providerProperties = filterAndStripProviderId(providerId);

    return createProvider(createProviderConfiguration(providerProperties));
  }

  private Map<String, String> filterAndStripProviderId(String providerId) {
    LOG.log(FINEST, "Properties opgevraagd voor providerId ''{0}'' uit {1}", new Object[] {providerId, properties});
    return properties.entrySet().stream()
      .filter(e -> e.getKey().startsWith(providerId))
      .collect(toMap(entry -> entry.getKey().substring(providerId.length() + 1),
        entry -> entry.getValue()));
  }


  /**
   * Add a new created LinkParser to the provider properties.
   *
   * @param providerProperties properties of the search provider
   */
  private Map<String, Object> createProviderConfiguration(Map<String, String> providerProperties) {
    String linkparserClass = providerProperties.get(LINKPARSER_CLASS_KEY);
    Objects.requireNonNull(linkparserClass, "Fully qualified name of link parser (key = " + LINKPARSER_CLASS_KEY + ") is required.");

    Class<?> linkparser = classForName(linkparserClass);
    LinkParser parser = (LinkParser) createNewInstance(linkparser.getConstructors()[0]);
    Map<String, Object> provProp = new HashMap<>(providerProperties);
    provProp.put("linkParser", parser);
    return provProp;
  }

  /**
   * Read all parameters of the constructor and match them with the configured provider properties.
   *
   * @param providerProperties properties voor de provider
   * @return the created search provider
   */
  private SearchProvider createProvider(Map<String, Object> providerProperties) {
    String fqName = (String) providerProperties.get("class");
    Class<?> clazz = classForName(fqName);

    Object[] arguments = new Object[clazz.getConstructors()[0].getParameterCount()];

    Parameter[] parameters = clazz.getConstructors()[0].getParameters();
    for (int i = 0; i < parameters.length; i++) {
      if (!parameters[i].isNamePresent())
        throw new IllegalStateException("Sources not compiled with compiler flag '-parameters'!! Please fix...");
      arguments[i] = providerProperties.get(parameters[i].getName());
    }

    SearchProvider newInstance = (SearchProvider) createNewInstance(clazz.getConstructors()[0], arguments);

    LOG.log(FINE, "Created SearchProvider: {0}", newInstance);
    return newInstance;
  }

  public Map<String, Class> getConfiguratieVoorKlasse(String fqName) {
    Class<?> clazz = classForName(fqName);

    Parameter[] constructorParameters = clazz.getConstructors()[0].getParameters();
    Map<String, Class> configuratieTypes = stream(constructorParameters)
      .collect(toMap(Parameter::getName, Parameter::getType));

    LOG.log(FINEST, "Configuratie voor klasse {0} = {1}", new Object[] {fqName, configuratieTypes});
    return configuratieTypes;
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

  public Map<String, String> getConfiguratieVoor(String providerId) {
    Map<String, String> configuration = filterAndStripProviderId(providerId);
    LOG.log(FINEST, "Configuratie voor ''{0}'' = {1}", new Object[] {providerId, configuration});
    return configuration;
  }
}
