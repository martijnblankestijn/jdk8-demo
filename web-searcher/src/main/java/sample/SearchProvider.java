package sample;

import java.net.URL;
import java.util.List;

/**
 * Created by martijn on 12/24/13.
 */
public interface SearchProvider {
    String getName();

    URL buildUrl(String zoekterm);

    List<URL> parse(String page);
}
