package nl.ordina.java8.presentation.searcher;

import javafx.scene.control.TreeCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import nl.ordina.java8.control.Page;
import nl.ordina.java8.control.SearchProvider;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.logging.Level;
import java.util.logging.Logger;

import static javafx.scene.paint.Color.BLACK;
import static javafx.scene.paint.Color.LIGHTGRAY;

public class PageCell extends TreeCell<Object> {
    private static final Logger LOG = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());

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

            graphic = new ImageView(createImage("/ico/" + searchProvider.getId() + ".png"));

        } else {
            background = null;
            text = String.valueOf(object);
            graphic = null;
        }
        setText(text);
        setGraphic(graphic);
        setTextFill(background);
    }

    // TODO remove dependency on JavaFX
    private Image createImage(String icoPath) {
        try (InputStream is = getClass().getResourceAsStream(icoPath)) {
            if(is == null) {
                LOG.log(Level.WARNING, "Image not found for path " + icoPath);
            }
            return new Image(is);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
