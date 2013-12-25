package nl.ordina.java8.composable.parsers;

import javafx.scene.image.Image;
import nl.ordina.java8.composable.LinkParser;
import nl.ordina.java8.composable.SearchProvider;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class SearchProviderService {
    private List<SearchProvider> providers;

    public List<SearchProvider> getProviders() {
        Properties properties = loadProperties();
        providers = Arrays.asList(
                buildProvider("Google",
                        "https://ajax.googleapis.com/ajax/services/search/web?v=1.0&q=", new GoogleJsonParser(), "/ico/google.png"),
                buildProvider("Wikipedia",
                        "http://en.wikipedia.org/w/api.php?action=opensearch&format=json&limit=15&search=", new WikipediaJsonParser("http://en.wikipedia.org/wiki/"), "/ico/wikipedia.png"),
                new BingSearchProvider("Bing",
                        properties.getProperty("bing_url"), new BingJsonParser(), createImage("/ico/bing.png"),
                        properties.getProperty("bing_key"))
        );
        return providers;
    }

    private Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream inputStream = SearchProviderService.class.getResourceAsStream("/etc/configuration.properties")) {
            properties.load(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return properties;
    }

    private SimpleUrlSearchProvider buildProvider(String name, String url, LinkParser linkParser, String icoPath) {
        Image image = createImage(icoPath);
        return new SimpleUrlSearchProvider(name, url, linkParser, image);
    }

    private Image createImage(String icoPath) {
        return new Image(getClass().getResourceAsStream(icoPath));
    }


}
