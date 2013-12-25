package nl.ordina.java8.composable;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Main extends Application {
    private static final Logger LOG = Logger.getLogger(Main.class.getName());

    @Override
    public void start(Stage primaryStage) throws Exception{
        LOG.log(Level.INFO, "Starting application");
        Parent root = FXMLLoader.load(getClass().getResource("websearcher.fxml"));
        primaryStage.setTitle("Java 8 Web Searcher");
        primaryStage.setScene(new Scene(root, 1280, 800));
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
