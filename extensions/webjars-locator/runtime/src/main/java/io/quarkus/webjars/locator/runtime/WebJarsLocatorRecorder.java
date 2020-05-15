package io.quarkus.webjars.locator.runtime;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
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
                    // Fall back for Java 8 and Windows
                    event.vertx().fileSystem().readDir(webjarsFileSystemPath + webjar.toString(), readDir -> {
                        if (readDir.succeeded()) {
                            List<String> versionList = readDir.result();
                            // There should be exactly one version (should be a build error)
                            if (versionList.size() != 1) {
                                event.fail(404);
                            } else {
                                String webjarVersion = Paths.get(versionList.get(0)).getFileName().toString();
                                String resolvedPath = webjarsRootUrl + webjar.toString() + "/" +
                                        webjarVersion + convertToRelativeUrl(webjar, rest);
                                event.put(HANDLED_EVENT, true);
                                event.reroute(resolvedPath);
                            }
                        } else {
                            event.fail(404);
                        }
                    });
                } else {
                    String resolvedPath = webjarsRootUrl + webjar.toString() + "/" +
                            version + convertToRelativeUrl(webjar, rest);
                    event.put(HANDLED_EVENT, true);
                    event.reroute(resolvedPath);
                }

            } else {
                event.next();
            }
        };
    }

    private static String convertToRelativeUrl(Path root, Path rest) {
        Path relativePath = root.relativize(rest);
        StringBuilder url = new StringBuilder();
        for (Path part : relativePath) {
            url.append("/");
            try {
                url.append(URLEncoder.encode(part.toString(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                // Shouldn't happen, "UTF-8 should be available in all JVM's
                throw new IllegalStateException(e);
            }
        }
        return url.toString();
    }

}
