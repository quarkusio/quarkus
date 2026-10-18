package io.quarkus.webdependency.locator.runtime;

import java.util.Map;

import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class WebDependencyLocatorRecorder {

    private static final Logger LOG = Logger.getLogger(WebDependencyLocatorRecorder.class.getName());

    public Handler<RoutingContext> getHandler(String webDependenciesRootUrl,
            Map<String, String> webDependencyNameToVersionMap) {
        return (event) -> {
            String path = event.normalizedPath();
            if (path.startsWith(webDependenciesRootUrl)) {
                try {
                    String rest = path.substring(webDependenciesRootUrl.length());
                    String webdep = rest.substring(0, rest.indexOf('/'));
                    if (webDependencyNameToVersionMap.containsKey(webdep)) {
                        // Check this is not the actual path (ex: /webjars/jquery/${jquery.version}/...
                        int endOfVersion = rest.indexOf('/', rest.indexOf('/') + 1);
                        if (endOfVersion == -1) {
                            endOfVersion = rest.length();
                        }
                        String nextPathEntry = rest.substring(rest.indexOf('/') + 1, endOfVersion);
                        if (webDependencyNameToVersionMap.get(webdep) == null
                                || nextPathEntry.equals(webDependencyNameToVersionMap.get(webdep))) {
                            // go to the next handler (which should be the static resource handler, if one exists)
                            event.next();
                        } else {
                            // reroute to the real resource
                            event.reroute(webDependenciesRootUrl + webdep + "/"
                                    + webDependencyNameToVersionMap.get(webdep) + rest.substring(rest.indexOf('/')));
                        }
                    } else {
                        event.next();
                    }
                } catch (Throwable t) {
                    LOG.debug("Error while locating web jar " + path);
                    // See if someone else can handle this.
                    event.next();
                }
            } else {
                // should not happen if route is set up correctly
                event.next();
            }
        };
    }

    public Handler<RoutingContext> getImportMapHandler(String expectedPath, String importmap) {
        return new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext event) {
                String path = event.normalizedPath();
                if (path.equals(expectedPath)) {
                    HttpServerResponse response = event.response();
                    response.headers().set(HttpHeaders.CONTENT_TYPE, "text/javascript");
                    response.end(JAVASCRIPT_CODE.formatted(importmap));
                } else {
                    // should not happen if route is set up correctly
                    event.next();
                }
            }
        };
    }

    private static final String JAVASCRIPT_CODE = """
            const im = document.createElement('script');
            im.type = 'importmap';
            im.textContent = JSON.stringify(%s);
            document.currentScript.after(im);
            """;
}
