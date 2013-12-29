package nl.ordina.java8.composable;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.web.WebView;
import nl.ordina.java8.composable.http.HttpUtil;
import nl.ordina.java8.composable.parsers.SearchProviderService;

import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.Optional.ofNullable;
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
    private TreeView<Object> searches;
    @FXML
    private WebView page;

    private List<SearchProvider> providers;

    @FXML
    void initialize() {
        // TODO inject
        providers = new SearchProviderService().getProviders();
        zoekterm.textProperty()
                .addListener((observable, oud, nieuw) -> {
                    if (!Objects.equals(oud, nieuw)) search(nieuw);
                });

        TreeItem<Object> rootItem = new TreeItem<>("Search Providers");
        searches.setRoot(rootItem);
        searches.setShowRoot(false);
        rootItem.setExpanded(true);
        for (SearchProvider searchProvider : providers) {
            TreeItem item = new SearchProviderTreeItem(searchProvider);
            rootItem.getChildren().add(item);
        }
        searches.setOnMouseClicked(evt -> {
            TreeItem<Object> item = searches.getSelectionModel().getSelectedItem();
            Optional.ofNullable(item)
                    .ifPresent(ti -> {
                        if (item.isLeaf() && !rootItem.equals(item.getParent())) displayPageContent(item);
                    });
        });
        searches.setCellFactory(treeView -> new PageCell());
        Platform.runLater(zoekterm::requestFocus);
    }

    private void displayPageContent(TreeItem item) {
        // Alternative, load the url
        // page.getEngine().load(item.getValue());
        if (item instanceof PageTreeItem) {
            PageTreeItem pageTreeItem = ((PageTreeItem) item);
            String thePage = pageTreeItem.getValue().getContent();
            page.getEngine().loadContent(thePage);
        }
        else if (item instanceof SearchProviderTreeItem) {
            SearchProviderTreeItem searchProvider = (SearchProviderTreeItem) item;
            page.getEngine().loadContent(searchProvider.getUrl());
        }
    }

    private void search(String zoekterm) {
        LOG.log(FINE, "New search for {0}", zoekterm);
        if (zoekterm.length() < 2) return;


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
                                                    .filtered(p -> provider.equals(p.getValue()))
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

    private void refreshList(List<URL> lijst, TreeItem ti) {
        ti.setExpanded(false);
        LOG.log(FINEST, "Removing children from {0}", ti.getValue());
        ti.getChildren().removeAll(ti.getChildren());

        LOG.log(FINEST, "Adding children to {0} from {1}", new Object[]{ti.getValue(), lijst});
        try {
            List<TreeItem<? extends Object>> treeItems = lijst
                    .stream()
                    .map(url -> new PageTreeItem(new Page(url, supplyAsync(() -> HttpUtil.getPage(url)))))
                    .collect(toList());
            ti.getChildren().addAll(treeItems);
        } catch (Exception e) {
            LOG.log(WARNING, "TreeItem {0} met lijst {1}.", new Object[] {ti, lijst});
            e.printStackTrace();
        }
//        ti.setExpanded(true);
    }

}
class PageTreeItem extends TreeItem<Page> {
    private static final Logger LOG = getLogger(lookup().lookupClass().getName());

    public PageTreeItem(Page page) {
        super(page);
        page.handeResponse(
                (s) -> {
                    LOG.log(FINEST, "SUCCESS retrieving {0}", page);
                    runLater(() -> {
                    });
                },
                (exc) -> LOG.log(FINEST, "FAILURE retrieving {0}", page)
        );
    }
}

class SearchProviderTreeItem extends TreeItem<SearchProvider> {
    public SearchProviderTreeItem(SearchProvider provider) {
        super(provider);
    }

    public String getUrl() {
        return "http://www.google.com";
    }

    @Override
    public ObservableList<TreeItem<SearchProvider>> getChildren() {
        return super.getChildren();
    }
}

class Page {
    private final URL url;
    private final CompletableFuture<String> page;
    private String content;

    Page(URL url, CompletableFuture<String> page) {
        this.content = "Not retrieved yet";
        this.url = url;
        this.page = page;
        page.whenComplete((s, exception) -> {
            Optional<String> optionalContent = Optional.ofNullable(s);
            content = optionalContent.isPresent() ? optionalContent.get() : exception.toString();
        });
    }

    public void handeResponse(Consumer<String> success, Consumer<Throwable> failure) {
        page.whenComplete((s, exc) -> {
            ofNullable(s).ifPresent(success::accept);
            ofNullable(exc).ifPresent(failure::accept);
        });
    }

    public String toString() {
        return url.toString();
    }

    public String getContent() {
        return content;
    }

    public boolean isRetrieved() { return page.isDone();}


}


class UrlContent {
    private final URL url;
    private final String content;

    UrlContent(URL url, String content) {
        this.url = url;
        this.content = content;
    }

}

