package nl.ordina.java8.control.providers;

import nl.ordina.java8.control.LinkParser;
import nl.ordina.java8.control.http.HttpUtil;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Base64;

public class BingSearchProvider extends SimpleUrlSearchProvider {
    private final String key;

    public BingSearchProvider(String name, String siteUrl, String searchUrl, LinkParser linkParser, String id, String key) {
        super(name, siteUrl, searchUrl, linkParser, id);
        this.key = key;
    }

    @Override
    public URL buildUrl(String zoekterm) {
        return super.buildUrl("'" + zoekterm + "'");
    }

    @Override
    protected String retrievePage(URL searchPage) {
        URLConnection urlConnection;
        try {
            urlConnection = searchPage.openConnection();
            urlConnection.setRequestProperty("Authorization", "Basic " + Base64.getEncoder().encodeToString((key + ":" + key).getBytes()));
            return HttpUtil.readCompleteResponse(urlConnection);

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }
}
