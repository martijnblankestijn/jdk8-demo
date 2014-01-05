package nl.ordina.java8.presentation.configuration;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import nl.ordina.java8.control.SearchProvider;
import nl.ordina.java8.control.providers.BingSearchProvider;
import nl.ordina.java8.control.providers.SearchProviderFactory;
import nl.ordina.java8.control.providers.SimpleUrlSearchProvider;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.logging.Level.FINEST;
import static java.util.logging.Logger.getLogger;
import static java.util.stream.Collectors.toList;

public class ConfigurationPresenter {
  private static final Logger LOG = getLogger(lookup().lookupClass().getName());

  @FXML
  private TabPane tabPane;

  @Inject private SearchProviderFactory factory;

  @FXML
  void initialize() {
    Set<SearchProvider> searchProviders = factory.getSearchProviders();
    List<Tab> tabs = searchProviders.stream()
      .map(sp -> createTab(sp))
      .collect(toList());
    tabPane.getTabs().addAll(tabs);
  }

  private Tab createTab(SearchProvider provider) {
    LOG.log(FINEST, "Nieuwe tab voor ''{0}''", provider.getName());
    Tab tab = new Tab();
    tab.textProperty().setValue(provider.getName());

    Label providerLabel = new Label("Provider klasse");

    ObservableList<String> options = FXCollections.observableArrayList(
      SimpleUrlSearchProvider.class.getName(),
      BingSearchProvider.class.getName()
    );

    ComboBox<String> providerImplementation = new ComboBox<>(options);


    TableView<SearchProviderConfigurationEntry> table = createTableView();

    providerImplementation.getSelectionModel().select(provider.getClass().getName());
    refreshTable(provider.getId(), table, provider.getClass().getName());

    providerImplementation.setOnAction(evt -> providerChanged(provider.getId(), providerImplementation, table));



    HBox provderBox = new HBox(providerLabel, providerImplementation);
    provderBox.setPadding(new Insets(10));

    HBox entriesBox = new HBox(table);
    entriesBox.setPadding(new Insets(10));

    BorderPane borderPane = new BorderPane();
    borderPane.setTop(provderBox);
    borderPane.setCenter(entriesBox);

    tab.setContent(borderPane);
    return tab;
  }

  private void providerChanged(String providerId, ComboBox<String> providerImplementation, TableView<SearchProviderConfigurationEntry> table) {
    String fqNameProvider = providerImplementation.getSelectionModel().getSelectedItem();
    refreshTable(providerId, table, fqNameProvider);
  }

  private TableView<SearchProviderConfigurationEntry> createTableView() {
    TableView<SearchProviderConfigurationEntry> table = new TableView<>();
    table.setMaxWidth(1000);
    table.setEditable(true);

    TableColumn<SearchProviderConfigurationEntry,String> naamKolom = new TableColumn<>("Naam");
    naamKolom.setCellValueFactory(p -> p.getValue().getConfiguration().getName());
    naamKolom.setEditable(false);
    naamKolom.setPrefWidth(150);

    TableColumn<SearchProviderConfigurationEntry,String> waardeKolom = new TableColumn<>("Waarde");
    waardeKolom.setCellValueFactory(p -> p.getValue().getValue());
    waardeKolom.setPrefWidth(750);

    table.getColumns().addAll(naamKolom, waardeKolom);
    return table;
  }

  private void refreshTable(String providerId, TableView<SearchProviderConfigurationEntry> table, String fqNameProvider) {
    LOG.log(FINEST, "Refreshing table voor {0}", fqNameProvider);
    Map<String, Class> configuratieVoorKlasse = factory.getConfiguratieVoorKlasse(fqNameProvider);
    Map<String, String> configuratieProvider = factory.getConfiguratieVoor(providerId);

    List<SearchProviderConfigurationEntry> entries = configuratieVoorKlasse.entrySet().stream()
      .map((t) -> map(t, configuratieProvider))
      .collect(toList());
    table.setItems(FXCollections.observableList(entries));
  }

  private SearchProviderConfigurationEntry map(Map.Entry<String, Class> e, Map<String, String> configuratieProvider) {
    SearchProviderConfiguration configuration = new SearchProviderConfiguration(e.getKey(), e.getValue().getName());
    return new SearchProviderConfigurationEntry(configuration, configuratieProvider.get(e.getKey()));
  }
}
class SearchProviderConfiguration {
  private final StringProperty name;
  private final StringProperty clazz;

  SearchProviderConfiguration(String name, String clazz) {
    this.name = new SimpleStringProperty(name);
    this.clazz = new SimpleStringProperty(clazz);
  }

  public StringProperty getName() {
    return name;
  }

  public StringProperty getClazz() {
    return clazz;
  }
}

class SearchProviderConfigurationEntry {
  private final SearchProviderConfiguration configuration;
  private final StringProperty value;

  SearchProviderConfigurationEntry(SearchProviderConfiguration configuration, String value) {
    this.configuration = configuration;
    this.value = new SimpleStringProperty(value);
  }

  public SearchProviderConfiguration getConfiguration() {
    return configuration;
  }

  public StringProperty getValue() {
    return value;
  }
}