package io.quarkus.vertx.http.deployment.webjar;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * WebJarResourcesFilter which combines several other filters. Each filter gets called with the inputstream of the
 * previous filter, or the original one if it is the first filter. Filters are processed in order.
 */
public class CombinedWebJarResourcesFilter implements WebJarResourcesFilter {
    private final List<WebJarResourcesFilter> filters;

    public CombinedWebJarResourcesFilter(List<WebJarResourcesFilter> filters) {
        this.filters = filters;
    }

    @Override
    public FilterResult apply(String fileName, InputStream stream) throws IOException {
        FilterResult lastResult = null;
        boolean changed = false;
        for (WebJarResourcesFilter filter : filters) {

            if (lastResult != null) {
                lastResult = filter.apply(fileName, lastResult.getStream());
            } else {
                lastResult = filter.apply(fileName, stream);
            }

            if (lastResult.isChanged() && !changed) {
                changed = true;
            }
        }

        return new FilterResult(lastResult.getStream(), changed);
    }
}
