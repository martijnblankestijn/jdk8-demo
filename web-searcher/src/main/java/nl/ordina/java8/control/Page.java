package nl.ordina.java8.control;

import java.net.URL;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static java.util.Optional.ofNullable;

/**
 * Page.
 */
public class Page {
  private final URL url;
  private final URL imageUrl;
  private final CompletableFuture<String> page;

  private String content;

  public Page(URL url, CompletableFuture<String> page, URL imageUrl) {
    this.imageUrl = imageUrl;
    this.content = "Not retrieved yet";
    this.url = url;
    this.page = page;
    page.whenComplete((s, exception) -> {
      Optional<String> optionalContent = Optional.ofNullable(s);
      content = optionalContent.isPresent() ? optionalContent.get() : exception.toString();
    });
  }

  public void handeResponse(Consumer<String> success, Consumer<Throwable> failure) {
    page.whenComplete((s, exc) -> {
      ofNullable(s).ifPresent(success::accept);
      ofNullable(exc).ifPresent(failure::accept);
    });
  }

  public String toString() {
    return url.toString();
  }

  public String getContent() {
    return content;
  }

  public boolean isRetrieved() {
    return page.isDone();
  }

  public URL getImageUrl() {
    return imageUrl;
  }

  public URL getUrl() {
    return url;
  }
}
