package sample;

import java.net.URL;
import java.util.List;

interface LinkParser {

    List<URL> parseForLinks(String message);
}
