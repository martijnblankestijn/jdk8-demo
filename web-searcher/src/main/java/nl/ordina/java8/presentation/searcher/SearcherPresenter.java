package nl.ordina.java8.presentation.searcher;

import com.airhacks.afterburner.views.FXMLView;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import nl.ordina.java8.control.Page;
import nl.ordina.java8.control.SearchProvider;
import nl.ordina.java8.control.SearchProviderService;
import nl.ordina.java8.presentation.about.AboutView;
import nl.ordina.java8.presentation.configuration.ConfigurationView;

import javax.inject.Inject;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.logging.Level.*;
import static java.util.logging.Logger.getLogger;
import static java.util.stream.Collectors.toList;
import static javafx.application.Platform.runLater;
import static javafx.stage.Modality.WINDOW_MODAL;
import static javafx.stage.StageStyle.UTILITY;
import static nl.ordina.java8.control.http.HttpUtil.getPage;

public class SearcherPresenter {
  private static final Logger LOG = getLogger(lookup().lookupClass().getName());

  @FXML
  private TextField zoekterm;
  @FXML
  private TreeView<Page> searches;
  @FXML
  private WebView page;

  @FXML
  private MenuItem closeItem;

  @FXML
  private MenuItem aboutItem;

  @FXML
  private MenuItem settingsItem;

  @Inject
  private SearchProviderService searchProviderService;


  @FXML
  void initialize() {


    closeItem.setOnAction(evt -> close());
    aboutItem.setOnAction(evt ->  showDialog(new AboutView()) );
    settingsItem.setOnAction(evt -> showDialog(new ConfigurationView()));

    zoekterm.textProperty()
      .addListener((observable, oud, nieuw) -> {
        if (!Objects.equals(oud, nieuw)) search(nieuw);
      });

    TreeItem<Page> rootItem = new TreeItem<>(null);
    rootItem.setExpanded(true);
    searches.setRoot(rootItem);
    searches.setShowRoot(false);

    for (SearchProvider searchProvider : searchProviderService.getProviders()) {
      final URL site = searchProvider.getSiteUrl();
      Page page = new Page(site, supplyAsync(() -> getPage(site)), getClass().getResource("/ico/" + searchProvider.getId() + ".png"));
      rootItem.getChildren().add(new PageTreeItem(page));
    }

    searches.setCellFactory(treeView -> new PageCell());

    searches.setOnMouseClicked(evt ->
      ofNullable(searches.getSelectionModel().getSelectedItem())
        .ifPresent(ti -> displayPageContent(ti.getValue())));

    Platform.runLater(zoekterm::requestFocus);
  }

  private void showDialog(FXMLView view) {
    final Stage dialog = new Stage();
    dialog.initModality(WINDOW_MODAL);
    dialog.initStyle(UTILITY);
    dialog.setResizable(false);
    dialog.initOwner(searches.getScene().getWindow());
    dialog.setScene(new Scene(view.getView()));
    dialog.showAndWait();
  }

  private void close() {
    LOG.info("Closing application");
    Platform.exit();
  }

  private void displayPageContent(Page item) {
      page.getEngine().loadContent(item.getContent());
  }

  private void search(String zoekterm) {
    LOG.log(FINE, "New search for {0}", zoekterm);
    if (zoekterm.length() < 2) return;

    // als de async de site van de search provider bevraagd heeft,
    // kunnen de url's worden toegevoegd aan de tree.
    // Zie ook het gebruik van Optional als alternatief van de null-check
    BiConsumer<SearchProvider, ? super List<URL>> searchFunction = (provider, lijst) ->
      searches.getRoot()
        .getChildren()
        .filtered(p -> provider.getSiteUrl().equals(p.getValue().getUrl()))
        .forEach(ti -> runLater(() -> refreshList(lijst, ti)));

    searchProviderService.search(zoekterm, searchFunction);
  }


  private void refreshList(List<URL> lijst, TreeItem<Page> ti) {
    ti.getChildren().removeAll(ti.getChildren());

    LOG.log(FINEST, "Adding children to {0} from {1}", new Object[]{ti.getValue(), lijst});
    try {
      List<TreeItem<Page>> treeItems = lijst
        .stream()
        .map(this::fetchAndCreatePageItem)
        .collect(toList());

      ti.getChildren().addAll(treeItems);
    } catch (Exception e) {
      LOG.log(WARNING, "TreeItem {0} met lijst {1}.", new Object[]{ti, lijst});
      e.printStackTrace();
    }
  }

  private PageTreeItem fetchAndCreatePageItem(URL url) {
    return new PageTreeItem(searchProviderService.retrieve(url));
  }

}

