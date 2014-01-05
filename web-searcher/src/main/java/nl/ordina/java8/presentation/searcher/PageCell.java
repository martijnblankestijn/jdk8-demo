package nl.ordina.java8.presentation.searcher;

import javafx.scene.control.TreeCell;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import nl.ordina.java8.control.Page;

import static javafx.scene.paint.Color.BLACK;
import static javafx.scene.paint.Color.LIGHTGRAY;

public class PageCell extends TreeCell<Page> {

  @Override
    protected void updateItem(Page page, boolean empty) {
        super.updateItem(page, empty);

        final ImageView graphic;
        final String text;
        final Color background;

        if (empty) {
            graphic = null;
            text = null;
            background = null;
        } else {
          background = page.isRetrieved() ? BLACK : LIGHTGRAY;
            text = page.toString();
            graphic = page.getImageUrl() == null ? null : new ImageView(page.getImageUrl().toExternalForm());
        }
        setText(text);
        setGraphic(graphic);
        setTextFill(background);
    }
}
