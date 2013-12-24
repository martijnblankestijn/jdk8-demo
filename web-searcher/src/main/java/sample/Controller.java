package sample;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyEvent;
import javafx.scene.web.WebView;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.currentThread;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.stream.Collectors.toList;

public class Controller {
    @FXML
    private TextField zoekterm;
    @FXML
    private TreeView<String> searches;
    @FXML
    private WebView page;

    private Map<String, CompletableFuture<String>> content = new ConcurrentHashMap<>();

    @FXML
    void initialize() {
        List<String> searchProviders = Arrays.asList("Google", "Wikipedia");
        List<SearchProvider> providers = Arrays.asList(
                new SimpleUrlSearchProvider("Google", "https://ajax.googleapis.com/ajax/services/search/web?v=1.0&q=", new GoogleJsonParser()),
                new SimpleUrlSearchProvider("Wikipedia", "", new GoogleJsonParser())
        );

        zoekterm.setText("Computer");
        zoekterm.setOnKeyPressed(evt -> handleEvent(evt));


        TreeItem<String> rootItem = new TreeItem<String>("Search Providers");
        rootItem.setExpanded(true);
        for (String searchProvider : searchProviders) {
            TreeItem<String> item = new TreeItem<String>(searchProvider);
            rootItem.getChildren().add(item);
        }
        searches.setRoot(rootItem);
        searches.setShowRoot(false);
        searches.setOnMouseClicked(evt -> {
            TreeItem<String> item = searches.getSelectionModel().getSelectedItem();
            if (item != null && item.isLeaf() && !rootItem.equals(item.getParent())) {
// LOAD THE URL                page.getEngine().load(item.getValue());
                try {
                    String thePage = content.getOrDefault(item.getValue(), CompletableFuture.completedFuture("EMPTY"))
                            .get(1, TimeUnit.SECONDS);
                    System.out.println("The PAGE: " + thePage);
                    page.getEngine().loadContent(thePage);
                } catch (Exception e) {
                    page.getEngine().loadContent("Fout tijdens ophalen " + e.getMessage());
                }
            }
        });
    }

    private void handleEvent(KeyEvent evt) {

        String text = zoekterm.getText();
        System.out.println("Search for: " + text);

        TreeItem<String> provider = searches.getRoot().getChildren().get(0);
        boolean verwijderd = provider.getChildren().removeAll(provider.getChildren());
        log("Verwijderd: " + verwijderd);

        SimpleUrlSearchProvider google = new SimpleUrlSearchProvider("Google", "https://ajax.googleapis.com/ajax/services/search/web?v=1.0&q=", new GoogleJsonParser());
        try {

            CompletableFuture<List<UrlContent>> listCompletableFuture
                    = supplyAsync(() -> parseLinksForSite(google.buildUrl(text), google))
                    // als de async de site van de search provider bevraagd heeft,
                    // kunnen de url's worden toegevoegd aan de tree.
                    // Zie ook het gebruik van Optional als alternatief van de null-check
                    .whenComplete((List<URL> nullableLijst, Throwable exception) ->
                            ofNullable(nullableLijst)
                                    .ifPresent(lijst ->
                                            searches.getRoot()
                                                    .getChildren()
                                                    .filtered(p -> p.getValue().equals(google.getName()))
                                                    .forEach(ti -> lijst.forEach(
                                                            url -> {
                                                                addUrlToTreeItem(ti, url);
                                                                content.putIfAbsent(url.toString(),
                                                                        supplyAsync(() -> getPage(url)));
                                                            }
                                                    ))

                                    ))
                    .thenCompose(lijst ->
                            supplyAsync(() -> lijst.stream()
                                    .parallel()
                                    .map(link -> new UrlContent(link, getPage(link)))
                                    .collect(toList())));


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean addUrlToTreeItem(TreeItem<String> ti, URL url) {
        return ti.getChildren().add(new TreeItem<>(url.toString()));
    }

    private List<URL> parseLinksForSite(final URL searchPage, final SimpleUrlSearchProvider searchProvider) {
        return searchProvider.parse(getPage(searchPage));
    }

    private String getPage(final URL site) {

        log("Getting site " + site);

        StringBuilder builder = new StringBuilder();
        URLConnection yc;
        try {
            yc = site.openConnection();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()))) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    builder.append(inputLine);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        log("Content of " + site + " = [" + builder.length() + "]");
        return builder.toString();
    }


    private void log(String msg) {
        System.out.println(currentThread().hashCode() + "(" + currentTimeMillis() % 1000000 + "): " + msg);
    }

}

class GoogleJsonParser implements LinkParser {

    @Override
    public List<URL> parseForLinks(String message) {
        try (JsonReader parser = Json.createReader(new StringReader(message))) {
            JsonArray results = parser.readObject()
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

    private URL createUrl(String unescapedUrl) {
        try {
            return new URL(unescapedUrl);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Ongeldige url: " + unescapedUrl, e);
        }
    }

}

class UrlContent {
    private final URL url;
    private final String content;

    UrlContent(URL url, String content) {
        this.url = url;
        this.content = content;
    }

    public URL getUrl() {
        return url;
    }

    public String getContent() {
        return content;
    }
}

class SimpleUrlSearchProvider implements SearchProvider {
    private final String name;
    private final String url;
    private final LinkParser parser;

    SimpleUrlSearchProvider(String name, String url, LinkParser parser) {
        this.name = name;
        this.url = url;
        this.parser = parser;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public URL buildUrl(String zoekterm) {
        final String spec;
        try {
            spec = url + URLEncoder.encode(zoekterm, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UnsupportedEncodingException voor " + zoekterm, e);
        }
        try {
            return new URL(spec);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Ongeldige url:" + spec, e);
        }
    }

    @Override
    public List<URL> parse(String page) {
        return parser.parseForLinks(page);
    }
}