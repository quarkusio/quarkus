package io.quarkus.webjars.locator.runtime;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class WebJarsLocatorRecorder {

    public Handler<RoutingContext> getHandler(String webjarsRootUrl, Map<String, String> versionMap) {
        final Path webjarsRootUrlPath = Paths.get(webjarsRootUrl);
        final String webjarsFileSystemPath = "META-INF/resources/webjars/";
        final String HANDLED_EVENT = "quarkus-webjar-locator-location-handled";
        return (event) -> {
            Path path = Paths.get(event.normalisedPath());
            if (event.get(HANDLED_EVENT) != null) {
                event.next();
            } else if (path.startsWith(webjarsRootUrlPath)) {
                Path rest = webjarsRootUrlPath.relativize(path);
                Path webjar = rest.getName(0);
                String version = versionMap.get(webjar.toString());
                if (version == null) {
                    event.fail(404);
                } else {
                    String resolvedPath = webjarsRootUrl + webjar.toString() + "/" +
                            version + "/" + webjar.relativize(rest).toString();
                    event.put(HANDLED_EVENT, true);
                    event.reroute(resolvedPath);
                }

            } else {
                event.next();
            }
        };
    }

}
