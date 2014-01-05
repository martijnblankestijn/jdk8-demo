package nl.ordina.java8.control;

import nl.ordina.java8.control.http.HttpUtil;
import nl.ordina.java8.control.providers.SearchProviderFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.logging.Level.FINEST;
import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;

public class SearchProviderService {
    private static final Logger LOG = getLogger(lookup().lookupClass().getName());

    private Set<SearchProvider> searchProviders;
    @Inject
    SearchProviderFactory factory;

    @PostConstruct
    private void construct() {
        searchProviders = factory.getSearchProviders();
    }

    public Set<SearchProvider> getProviders() {
        return searchProviders;
    }


    public void search(String zoekterm, BiConsumer<SearchProvider, ? super List<URL>> callback) {
        for (final SearchProvider provider : factory.getSearchProviders()) {
            LOG.log(FINEST, "Dealing with provider {0}", provider.getName());
            supplyAsync(() -> provider.parseLinks(zoekterm))
                    .whenComplete((List<URL> nullableLijst, Throwable exception) -> {
                        ofNullable(nullableLijst).ifPresent(lijst -> callback.accept(provider, lijst));
                        ofNullable(exception).ifPresent(exc -> LOG.log(WARNING, "Exception caught when querying provder " + provider.getName(), exc));
                    });
        }
    }

    public Page retrieve(URL url) {
        return new Page(url, supplyAsync(() -> HttpUtil.getPage(url)), null);
    }
}
