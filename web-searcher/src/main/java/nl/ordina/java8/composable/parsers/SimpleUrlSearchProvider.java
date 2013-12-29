package nl.ordina.java8.composable.parsers;

import javafx.scene.image.Image;
import nl.ordina.java8.composable.LinkParser;
import nl.ordina.java8.composable.SearchProvider;
import nl.ordina.java8.composable.http.HttpUtil;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.logging.Logger;

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.logging.Level.FINE;
import static java.util.logging.Logger.getLogger;

public class SimpleUrlSearchProvider implements SearchProvider {
    private static final Logger LOG = getLogger(lookup().lookupClass().getName());

    private final String name;
    private final String siteUrl;
    private final String searchUrl;
    private final LinkParser parser;
    private final Image image;

    public SimpleUrlSearchProvider(String name, String siteUrl, String searchUrl, LinkParser parser, Image image) {
        this.name = name;
        this.siteUrl = siteUrl;
        this.searchUrl = searchUrl;
        this.parser = parser;
        this.image = image;
    }

    public Image getImage() {
        return image;
    }

    @Override
    public List<URL> retrieveResults(String zoekterm) {
        URL url = buildUrl(zoekterm);
        LOG.log(FINE, "Retrieve search rersults from {0}", url);
        List<URL> links = parseLinksForSite(url);
        LOG.log(FINE, "Lijst van {0}={1}", new Object[]{url, links});

        return links;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getSiteUrl() {
        return siteUrl;
    }

    URL buildUrl(String zoekterm) {
        final String spec;
        try {
            spec = searchUrl + URLEncoder.encode(zoekterm, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UnsupportedEncodingException voor " + zoekterm, e);
        }
        try {
            return new URL(spec);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Ongeldige searchUrl:" + spec, e);
        }
    }

    private List<URL> parseLinksForSite(final URL searchPage) {
        String response = retrievePage(searchPage);
        return parser.parseForLinks(response);
    }

    protected String retrievePage(URL searchPage) {
        return HttpUtil.getPage(searchPage);
    }

    @Override
    public String toString() {
        return "SearchProvider{" +
                "name='" + name + '\'' +
                ", searchUrl='" + searchUrl + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SearchProvider)) return false;

        SearchProvider that = (SearchProvider) o;

        return name.equals(that.getName());

    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
