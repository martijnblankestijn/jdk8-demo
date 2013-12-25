package sample;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.web.WebView;

import javax.json.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;
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
    private List<SearchProvider> providers;

    @FXML
    void initialize() {

        providers = Arrays.asList(
                buildProvider("Google", "https://ajax.googleapis.com/ajax/services/search/web?v=1.0&q=", new GoogleJsonParser(), "/ico/google.png"),
                buildProvider("Wikipedia", "http://en.wikipedia.org/w/api.php?action=opensearch&format=json&limit=15&search=", new WikipediaJsonParser("http://en.wikipedia.org/wiki/"), "/ico/wikipedia.png")
        );

        zoekterm.setText("Computer");
        zoekterm.setOnKeyPressed(evt -> handleEvent(evt));


        TreeItem<String> rootItem = new TreeItem<String>("Search Providers");
        rootItem.setExpanded(true);
        for (SearchProvider searchProvider : providers) {
            TreeItem<String> item = new TreeItem<String>(searchProvider.getName(), new ImageView(searchProvider.getImage()));
//            item.s
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

    private SimpleUrlSearchProvider buildProvider(String name, String url, LinkParser linkParser, String icoPath) {
        Image image = new Image(getClass().getResourceAsStream(icoPath));
        if(image ==null) {
            log("Image " + icoPath + " kon niet gevonden worden!");
        }
        return new SimpleUrlSearchProvider(name, url, linkParser, image);
    }

    private void handleEvent(KeyEvent evt) {

        String text = zoekterm.getText();
        System.out.println("Search for: " + text);

        for (TreeItem<String> item : searches.getRoot().getChildren()) {
            boolean verwijderd = item.getChildren().removeAll(item.getChildren());
            log("Verwijderd voor " + item.getValue() + ": " + verwijderd);
        }

        for (final SearchProvider searchProvider : providers) {
            System.out.println("Dealing with provider " + searchProvider.getName());
            CompletableFuture<List<UrlContent>> listCompletableFuture
                    = supplyAsync(() -> parseLinksForSite(searchProvider.buildUrl(text), searchProvider))
                    // als de async de site van de search provider bevraagd heeft,
                    // kunnen de url's worden toegevoegd aan de tree.
                    // Zie ook het gebruik van Optional als alternatief van de null-check
                    .whenComplete((List<URL> nullableLijst, Throwable exception) ->
                            ofNullable(nullableLijst)
                                    .ifPresent(lijst ->
                                            searches.getRoot()
                                                    .getChildren()
                                                    .filtered(p -> {
                                                        System.out.println(p + ":" + searchProvider.getName() + " --- " + p.getValue().equals(searchProvider.getName()));
                                                        return p.getValue().equals(searchProvider.getName());
                                                    })
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

        }


    }

    private boolean addUrlToTreeItem(TreeItem<String> ti, URL url) {
        return ti.getChildren().add(new TreeItem<>(url.toString()));
    }

    private List<URL> parseLinksForSite(final URL searchPage, final SearchProvider searchProvider) {
        List<URL> links = searchProvider.parse(getPage(searchPage));
        log("Lijst van " + searchPage + "=" + links);
        return links;
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

class WikipediaJsonParser implements LinkParser {
    private final String url;

    WikipediaJsonParser(String url) {
        this.url = url;
    }

    @Override
    public List<URL> parseForLinks(String message) {
        try (JsonReader reader = Json.createReader(new StringReader(message))) {
            JsonArray array = reader.readArray();
            JsonArray alternatives = array.getJsonArray(1);
            System.out.println("Empty: " + alternatives.isEmpty());
            if (alternatives.isEmpty()) return Collections.emptyList();
            else {
                List<URL> list = alternatives.stream()
                        .map(jsv -> createUrl(jsv))
                        .collect(toList());
                System.out.println("Links Wikipedia: " + list);
                return list;
            }
        }
    }

    private URL createUrl(JsonValue jsv) {
        String encode;
        try {
            encode = URLEncoder.encode(jsv.toString().replace('+', '_'), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Ongeldige url: " + jsv.toString(), e);
        }
        try {
            URL theUrl = new URL(url + encode);
            System.out.println("Wiki url: " + theUrl.toString());
            return theUrl;
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Ongeldige url: " + url +encode, e);
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
    private final Image image;

    SimpleUrlSearchProvider(String name, String url, LinkParser parser, Image image) {
        this.name = name;
        this.url = url;
        this.parser = parser;
        this.image = image;
    }

    public Image getImage() {
        return image;
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
