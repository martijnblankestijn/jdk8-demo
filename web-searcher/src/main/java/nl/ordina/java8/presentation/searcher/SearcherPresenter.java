package nl.ordina.java8.presentation.searcher;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.web.WebView;
import nl.ordina.java8.control.Page;
import nl.ordina.java8.control.SearchProvider;
import nl.ordina.java8.control.SearchProviderService;

import javax.inject.Inject;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.Optional.ofNullable;
import static java.util.logging.Level.*;
import static java.util.logging.Logger.getLogger;
import static java.util.stream.Collectors.toList;
import static javafx.application.Platform.runLater;

public class SearcherPresenter {
    private static final Logger LOG = getLogger(lookup().lookupClass().getName());

    @FXML
    private TextField zoekterm;
    @FXML
    private TreeView<Object> searches;
    @FXML
    private WebView page;

    @Inject
    private SearchProviderService searchProviderService;

    @FXML
    void initialize() {
        zoekterm.textProperty()
                .addListener((observable, oud, nieuw) -> {
                    if (!Objects.equals(oud, nieuw)) search(nieuw);
                });

        TreeItem<Object> rootItem = new TreeItem<>("Search Providers");
        rootItem.setExpanded(true);
        searches.setRoot(rootItem);
        searches.setShowRoot(false);
        for (SearchProvider searchProvider : searchProviderService.getProviders()) {
            TreeItem item = new SearchProviderTreeItem(searchProvider);
            rootItem.getChildren().add(item);
        }

        searches.setCellFactory(treeView -> new PageCell());
        searches.setOnMouseClicked(evt -> {
            TreeItem<Object> item = searches.getSelectionModel().getSelectedItem();
            ofNullable(item)
                    .ifPresent(ti -> displayPageContent(item));
        });
        Platform.runLater(zoekterm::requestFocus);
    }

    private void displayPageContent(TreeItem item) {
        if (item instanceof PageTreeItem) {
            PageTreeItem pageTreeItem = ((PageTreeItem) item);
            String thePage = pageTreeItem.getValue().getContent();
            page.getEngine().loadContent(thePage);
        }
        else if (item instanceof SearchProviderTreeItem) {
            SearchProviderTreeItem searchProvider = (SearchProviderTreeItem) item;
            page.getEngine().load(searchProvider.getUrl());
        }
    }

    private void search(String zoekterm) {
        LOG.log(FINE, "New search for {0}", zoekterm);
        if (zoekterm.length() < 2) return;

        // als de async de site van de search provider bevraagd heeft,
        // kunnen de url's worden toegevoegd aan de tree.
        // Zie ook het gebruik van Optional als alternatief van de null-check
        BiConsumer<SearchProvider,? super List<URL>> searchFunction = (provider, lijst) ->
                searches.getRoot()
                        .getChildren()
                        .filtered(p -> provider.equals(p.getValue()))
                        .forEach(ti -> runLater(() -> refreshList(lijst, ti)));


        searchProviderService.search(zoekterm, searchFunction);
    }


    private void refreshList(List<URL> lijst, TreeItem ti) {
        LOG.log(FINEST, "Removing children from {0}", ti.getValue());
        ti.getChildren().removeAll(ti.getChildren());

        LOG.log(FINEST, "Adding children to {0} from {1}", new Object[]{ti.getValue(), lijst});
        try {
            List<TreeItem<?>> treeItems = lijst
                    .stream()
                    .map(this::fetchAndCreatePageItem)
                    .collect(toList());
            ti.getChildren().addAll(treeItems);
        } catch (Exception e) {
            LOG.log(WARNING, "TreeItem {0} met lijst {1}.", new Object[] {ti, lijst});
            e.printStackTrace();
        }
    }

    private PageTreeItem fetchAndCreatePageItem(URL url) {
        return new PageTreeItem(searchProviderService.retrieve(url));
    }

}
class PageTreeItem extends TreeItem<Page> {
    private static final Logger LOG = getLogger(lookup().lookupClass().getName());

    public PageTreeItem(Page page) {
        super(page);
        page.handeResponse(
                (s) -> {
                    LOG.log(FINEST, "SUCCESS retrieving {0}", page);
                    Event.fireEvent(this, new TreeModificationEvent<>(TreeItem.valueChangedEvent(), this, page));
                },
                (exc) -> {
                    LOG.log(FINEST, "FAILURE retrieving {0}", page);
                    Event.fireEvent(this, new TreeModificationEvent<>(TreeItem.valueChangedEvent(), this, page));
                }
        );
    }
}

class SearchProviderTreeItem extends TreeItem<SearchProvider> {
    public SearchProviderTreeItem(SearchProvider provider) {
        super(provider);
    }

    public String getUrl() {
        return getValue().getSiteUrl();
    }

}
