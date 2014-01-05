package nl.ordina.java8.control;

import java.net.URL;
import java.util.List;

public interface LinkParser {
    /**
     * @param response html-pagina
     * @return list of links
     */
    List<URL> parseForLinks(String response);
}
