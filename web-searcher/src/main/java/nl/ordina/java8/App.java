package nl.ordina.java8;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import nl.ordina.java8.presentation.searcher.SearcherView;

import java.util.logging.Level;
import java.util.logging.Logger;

public class App extends Application {
    private static final Logger LOG = Logger.getLogger(App.class.getName());

    @Override
    public void start(Stage stage) throws Exception{
        LOG.log(Level.INFO, "Starting application");

        SearcherView searcherView = new SearcherView();
        Scene scene = new Scene(searcherView.getView());

        stage.setTitle("Java 8 Web Searcher");
        stage.setScene(scene);
        stage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
