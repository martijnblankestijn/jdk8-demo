package nl.ordina.java8.control.parsers;

import nl.ordina.java8.control.LinkParser;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.net.URL;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static nl.ordina.java8.control.http.HttpUtil.createUrl;

public class GoogleJsonParser implements LinkParser {

    @Override
    public List<URL> parseForLinks(String message) {
        try (JsonReader parser = Json.createReader(new StringReader(message))) {
            JsonArray results = parser
                    .readObject()
                    .getJsonObject("responseData")
                    .getJsonArray("results");

            return results.stream()
                    .map(json -> createUrlFromJsonValue((JsonObject) json))
                    .collect(toList());
        }
    }

    private URL createUrlFromJsonValue(JsonObject jsonValue) {
        return createUrl(jsonValue.getString("unescapedUrl"));
    }
}
