package io.quarkus.devui.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.IntStream;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler to load mvnpm jars
 */
public class MvnpmHandler implements Handler<RoutingContext> {

    private final URLClassLoader mvnpmLoader;
    private final String root;

    public MvnpmHandler(String root, Set<URL> mvnpmJars) {
        this.root = root;
        this.mvnpmLoader = new URLClassLoader(mvnpmJars.toArray(URL[]::new));
    }

    @Override
    public void handle(RoutingContext event) {
        String fullPath = event.normalizedPath().replaceFirst(root, SLASH);
        // Find the "filename" and see if it has a file extension
        String parts[] = fullPath.split(SLASH);
        String fileName = parts[parts.length - 1];

        if (!fileName.contains(DOT)) {
            fullPath = fullPath + DOT_JS;// Default to js. Some modules reference other module without the extension
        }

        try {
            URL url = getUrl(fullPath);
            if (url != null) {
                URLConnection openConnection = url.openConnection();
                long lastModified = openConnection.getLastModified();
                try (InputStream is = openConnection.getInputStream()) {
                    if (is != null) {
                        byte[] contents = is.readAllBytes();
                        event.response()
                                .putHeader(HttpHeaders.CONTENT_TYPE, getContentType(fileName))
                                .putHeader(HttpHeaders.CACHE_CONTROL, "public, immutable, max-age=31536000")
                                .putHeader(HttpHeaders.LAST_MODIFIED, formatDate(lastModified))
                                .putHeader("date", formatDate(LocalDateTime.now()))
                                .end(Buffer.buffer(contents));
                        return;
                    }
                }
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        event.next();
    }

    private URL getUrl(String fullPath) {
        URL url = mvnpmLoader.getResource(BASE_DIR + fullPath);
        if (url != null)
            return url;

        // HACK: If url is null, it might be that the content is actually available under a package folder, ex
        // /_static/lang-java/6.0.1/dist/index.js vs
        // /_static/lang-java/6.0.1/package/dist/index.js
        // This is due to a bug in mvnpm that at the time synced a few libraries packaged incorrectly
        // This will eventually never run anymore, if newer versions of js packages in question updates
        fullPath = insertPackageSegment(fullPath);
        return mvnpmLoader.getResource(BASE_DIR + fullPath);
    }

    private String insertPackageSegment(String path) {
        String[] parts = path.split(SLASH);
        List<String> modifiedParts = new ArrayList<>();

        IntStream.range(0, parts.length)
                .forEach(i -> {
                    modifiedParts.add(parts[i]);
                    if (i == 3) { // After the version segment
                        modifiedParts.add("package");
                    }
                });

        return String.join(SLASH, modifiedParts);
    }

    private String formatDate(long m) {
        Instant i = Instant.ofEpochMilli(m);
        return formatDate(i);
    }

    private String formatDate(TemporalAccessor t) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH)
                .withZone(ZoneId.of("GMT"));
        return formatter.format(t);
    }

    private String getContentType(String filename) {
        String f = filename.toLowerCase();
        if (f.endsWith(DOT_JS) || f.endsWith(DOT_MJS)) {
            return CONTENT_TYPE_JAVASCRIPT;
        } else if (f.endsWith(DOT_JSON)) {
            return CONTENT_TYPE_JSON;
        } else if (f.endsWith(DOT_HTML) || f.endsWith(DOT_HTM)) {
            return CONTENT_TYPE_HTML;
        } else if (f.endsWith(DOT_XHTML)) {
            return CONTENT_TYPE_XHTML;
        } else if (f.endsWith(DOT_CSS)) {
            return CONTENT_TYPE_CSS;
        } else if (f.endsWith(DOT_XML)) {
            return CONTENT_TYPE_XML;
        }
        // .csv 	Comma-separated values (CSV) 	text/csv
        // .gif 	Graphics Interchange Format (GIF) 	image/gif
        // .ico 	Icon format 	image/vnd.microsoft.icon
        // .jpeg, .jpg 	JPEG images 	image/jpeg
        // .png 	Portable Network Graphics 	image/png
        // .svg 	Scalable Vector Graphics (SVG) 	image/svg+xml
        // .ttf 	TrueType Font 	font/ttf
        // .woff 	Web Open Font Format (WOFF) 	font/woff
        // .woff2 	Web Open Font Format (WOFF) 	font/woff2

        return CONTENT_TYPE_JAVASCRIPT; // default

    }

    private static final String SLASH = "/";
    private static final String BASE_DIR = "META-INF/resources";
    private static final String DOT = ".";
    private static final String DOT_JS = ".js";
    private static final String DOT_MJS = ".mjs";
    private static final String DOT_JSON = ".json";
    private static final String DOT_HTML = ".html";
    private static final String DOT_HTM = ".htm";
    private static final String DOT_XHTML = ".xhtml";
    private static final String DOT_CSS = ".css";
    private static final String DOT_XML = ".xml";

    private static final String CONTENT_TYPE_JAVASCRIPT = "application/javascript";
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String CONTENT_TYPE_HTML = "text/html; charset=utf-8";
    private static final String CONTENT_TYPE_XHTML = "application/xhtml+xml; charset=utf-8";
    private static final String CONTENT_TYPE_XML = "application/xml; charset=utf-8";
    private static final String CONTENT_TYPE_CSS = "text/css; charset=utf-8";
    private static final String CONTENT_TYPE_TEXT = "text/plain; charset=utf-8";

}
