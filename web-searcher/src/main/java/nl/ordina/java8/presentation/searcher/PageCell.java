package nl.ordina.java8.presentation.searcher;

import javafx.scene.control.TreeCell;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import nl.ordina.java8.control.Page;
import nl.ordina.java8.control.SearchProvider;

import static javafx.scene.paint.Color.BLACK;
import static javafx.scene.paint.Color.LIGHTGRAY;

public class PageCell extends TreeCell<Object> {
    @Override
    protected void updateItem(Object object, boolean empty) {
        super.updateItem(object, empty);

        final ImageView graphic;
        final String text;
        final Color background;

        if (empty) {
            graphic = null;
            text = null;
            background = null;
        } else if (object instanceof Page) {

            background = ((Page) object).isRetrieved() ? BLACK : LIGHTGRAY;
            text = object.toString();
            graphic = null;
        } else if (object instanceof SearchProvider) {
            SearchProvider searchProvider = ((SearchProvider) object);

            background = null;
            text = searchProvider.getName();
            graphic = new ImageView(searchProvider.getImage());

        } else {
            background = null;
            text = String.valueOf(object);
            graphic = null;
        }
        setText(text);
        setGraphic(graphic);
        setTextFill(background);
    }
}
