package nl.ordina.java8.control.parsers;

import nl.ordina.java8.control.LinkParser;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.logging.Logger;

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.logging.Level.FINEST;
import static java.util.logging.Logger.getLogger;
import static java.util.stream.Collectors.toList;

public class BingJsonParser implements LinkParser {
    private static final Logger LOG = getLogger(lookup().lookupClass().getName());

    @Override
    public List<URL> parseForLinks(String response) {
        if(LOG.isLoggable(FINEST)) LOG.log(FINEST, "Response: {0}", response);

        try (JsonReader parser = Json.createReader(new StringReader(response))) {
            JsonArray results = parser.readObject()
                    .getJsonObject("d")
                    .getJsonArray("results");

            return results.stream()
                    .map(json -> createUrlFromJsonValue((JsonObject) json))
                    .collect(toList());
        }
    }

    private URL createUrlFromJsonValue(JsonObject json) {
        try {
            return new URL(json.getString("Url"));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
