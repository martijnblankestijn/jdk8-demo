package nl.ordina.java8.control.parsers;

import nl.ordina.java8.control.LinkParser;
import nl.ordina.java8.control.http.HttpUtil;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonReader;
import javax.json.JsonValue;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class WikipediaJsonParser implements LinkParser {
    private final static String WIKIPEDIA_URL = "http://en.wikipedia.org/wiki/";

    @Override
    public List<URL> parseForLinks(String message) {
        try (JsonReader reader = Json.createReader(new StringReader(message))) {
            JsonArray array = reader.readArray();
            JsonArray alternatives = array.getJsonArray(1);
            if (alternatives.isEmpty()) return Collections.emptyList();
            else return alternatives.stream()
                    .map(this::createUrl)
                    .collect(toList());
        }
    }

    private URL createUrl(JsonValue jsv) {
        String encode;
        try {
            encode = URLEncoder.encode(jsv.toString().replace(' ', '_'), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Ongeldige url: " + jsv.toString(), e);
        }
      return HttpUtil.createUrl((WIKIPEDIA_URL + encode));
    }
}
