package nl.ordina.java8.presentation.about;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.util.logging.Logger;

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.logging.Logger.getLogger;

public class AboutPresenter {
  private static final Logger LOG = getLogger(lookup().lookupClass().getName());

  @FXML
  Button okButton;

  @FXML
  Label aboutText;

  @FXML
  void initialize() {
    aboutText.setText("Demo van M. Blankestijn voor de Java Development Kit 8 presentatie");
    okButton.setOnAction(evt -> close());
  }

  private void close() {
    LOG.info("Closing About window");
    okButton.getScene().getWindow().hide();
  }
}
