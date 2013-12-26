package nl.ordina.java8.composable;

import java.net.URL;
import java.util.List;

public interface LinkParser {
    /**
     * @param response
     * @return list of links
     */
    List<URL> parseForLinks(String response);
}