package nl.ordina.java8.composable;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.ImageView;
import javafx.scene.web.WebView;
import nl.ordina.java8.composable.http.HttpUtil;
import nl.ordina.java8.composable.parsers.SearchProviderService;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.logging.Level.*;
import static java.util.logging.Logger.getLogger;
import static java.util.stream.Collectors.toList;
import static javafx.application.Platform.runLater;

public class Controller {
    private static final Logger LOG = getLogger(lookup().lookupClass().getName());

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
        // TODO inject
        providers = new SearchProviderService().getProviders();
        zoekterm.textProperty()
                .addListener((observable, oud, nieuw) -> {
                    if (!Objects.equals(oud, nieuw)) search(nieuw);
                });

        TreeItem<String> rootItem = new TreeItem<>("Search Providers");
        rootItem.setExpanded(true);
        for (SearchProvider searchProvider : providers) {
            TreeItem<String> item = new TreeItem<>(searchProvider.getName(), new ImageView(searchProvider.getImage()));
            rootItem.getChildren().add(item);
        }
        searches.setRoot(rootItem);
        searches.setShowRoot(false);
        searches.setOnMouseClicked(evt -> {
            TreeItem<String> item = searches.getSelectionModel().getSelectedItem();
            Optional.ofNullable(item)
                    .ifPresent(ti -> {
                        if (item.isLeaf() && !rootItem.equals(item.getParent())) displayPageContent(item);
                    });
        });

        Platform.runLater(() -> zoekterm.requestFocus());
    }

    private void displayPageContent(TreeItem<String> item) {
        // Alternative, load the url
        // page.getEngine().load(item.getValue());
        try {
            String thePage = content.getOrDefault(item.getValue(), completedFuture("EMPTY"))
                    .get(1, TimeUnit.SECONDS);
            LOG.log(FINEST, "The PAGE: {0}", thePage);
            page.getEngine().loadContent(thePage);
        } catch (Exception e) {
            page.getEngine().loadContent("Fout tijdens ophalen " + e.getMessage());
        }
    }

    private void search(String zoekterm) {
        LOG.log(FINE, "New search for {0}", zoekterm);
        if(zoekterm.length() < 2) return;


        for (final SearchProvider provider : providers) {
            LOG.log(FINEST, "Dealing with provider {0}", provider.getName());
            CompletableFuture<List<UrlContent>> listCompletableFuture
                    = supplyAsync(() -> provider.retrieveResults(zoekterm))
                    // als de async de site van de search provider bevraagd heeft,
                    // kunnen de url's worden toegevoegd aan de tree.
                    // Zie ook het gebruik van Optional als alternatief van de null-check
                    .whenComplete((List<URL> nullableLijst, Throwable exception) ->
                            ofNullable(nullableLijst)
                                    .ifPresent(lijst ->
                                            searches.getRoot()
                                                    .getChildren()
                                                    .filtered(p -> p.getValue().equals(provider.getName()))
                                                    .forEach(ti -> runLater(() -> refreshList(lijst, ti)))

                                    ))
                            // strikt genomen overbodig
                            // wel aardig om te laten zien hoe theCompose werkt
                    .thenCompose(lijst ->
                            supplyAsync(() -> lijst.stream()
                                    .parallel()
                                    .map(link -> new UrlContent(link, HttpUtil.getPage(link)))
                                    .collect(toList())));

        }


    }

    private void refreshList(List<URL> lijst, TreeItem<String> ti) {
        try {
            LOG.log(FINEST, "Removing children from {0}", ti.getValue());
            ti.getChildren().removeAll(ti.getChildren());

            LOG.log(FINEST, "Adding children to {0} from {1}", new Object[] {ti.getValue(), lijst});
            lijst.forEach(url -> addUrl(ti, url));
        }catch (Exception e) {
            System.err.println(ti + ": " + lijst);
            e.printStackTrace();
        }
    }

    private void addUrl(TreeItem<String> ti, URL url) {
        LOG.log(FINER, "Add {0} to item {1}", new Object[]{url, ti});

        ti.getChildren().add(new TreeItem<>(url.toString()));
        content.putIfAbsent(url.toString(), supplyAsync(() -> HttpUtil.getPage(url)));
    }

}

class UrlContent {
    private final URL url;
    private final String content;

    UrlContent(URL url, String content) {
        this.url = url;
        this.content = content;
    }

}

