package nl.ordina.java8.control;

import java.net.URL;
import java.util.List;

public interface SearchProvider {
    String getId();
    String getName();
    List<URL> parseLinks(String zoekterm);
    URL getSiteUrl();

}
