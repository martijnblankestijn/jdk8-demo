package nl.ordina.java8.control;

import javafx.scene.image.Image;
import nl.ordina.java8.control.http.HttpUtil;
import nl.ordina.java8.control.parsers.*;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.logging.Level.FINEST;
import static java.util.logging.Logger.getLogger;

public class SearchProviderService {
    private static final Logger LOG = getLogger(lookup().lookupClass().getName());
    public static final String CLASS_PATH_PROPERTIES = "/etc/configuration.properties";
    private final Properties properties = new Properties();

    @PostConstruct
    private void loadProperties() {
        try (InputStream inputStream = SearchProviderService.class.getResourceAsStream(CLASS_PATH_PROPERTIES)) {
            properties.load(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public List<SearchProvider> getProviders() {
        List<SearchProvider> providers = Arrays.asList(
                buildProvider("Google", "http://www.google.com",
                        "https://ajax.googleapis.com/ajax/services/search/web?v=1.0&q=", new GoogleJsonParser(), "/ico/google.png"),
                buildProvider("Wikipedia", "http://www.wikipedia.org",
                        "http://en.wikipedia.org/w/api.php?action=opensearch&format=json&limit=15&search=", new WikipediaJsonParser("http://en.wikipedia.org/wiki/"), "/ico/wikipedia.png"),
                new BingSearchProvider("Bing", "http://www.bing.com",
                        properties.getProperty("bing_url"), new BingJsonParser(), createImage("/ico/bing.png"),
                        properties.getProperty("bing_key"))
        );
        return providers;
    }


    public void search(String zoekterm, BiConsumer<SearchProvider, ? super List<URL>> callback) {
        for (final SearchProvider provider : getProviders()) {
            LOG.log(FINEST, "Dealing with provider {0}", provider.getName());
            supplyAsync(() -> provider.retrieveResults(zoekterm))
                    .whenComplete((List<URL> nullableLijst, Throwable exception) ->
                            ofNullable(nullableLijst)
                                    .ifPresent(lijst -> callback.accept(provider, lijst)));
        }
    }

    private SimpleUrlSearchProvider buildProvider(String name, String siteUrl, String seachUrl, LinkParser linkParser, String icoPath) {
        Image image = createImage(icoPath);
        return new SimpleUrlSearchProvider(name, siteUrl, seachUrl, linkParser, image);
    }

    // TODO remove dependency on JavaFX
    private Image createImage(String icoPath) {
        return new Image(getClass().getResourceAsStream(icoPath));
    }

    public Page retrieve(URL url) {
        return new Page(url, supplyAsync(() -> HttpUtil.getPage(url)));
    }
}
