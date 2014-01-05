package nl.ordina.java8.presentation.searcher;

import javafx.event.Event;
import javafx.scene.control.TreeItem;
import nl.ordina.java8.control.Page;

import java.util.logging.Logger;

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.logging.Level.FINEST;
import static java.util.logging.Logger.getLogger;

/**
 * Created by martijn on 1/5/14.
 */
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
