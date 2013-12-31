package nl.ordina.java8.control;

import javafx.scene.image.Image;

import java.net.URL;
import java.util.List;

public interface SearchProvider {
    String getName();

    Image getImage();

    List<URL> retrieveResults(String zoekterm);

    String getSiteUrl();
}
