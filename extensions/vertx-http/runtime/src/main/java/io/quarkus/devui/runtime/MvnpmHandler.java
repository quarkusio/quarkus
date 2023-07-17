package io.quarkus.devui.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;

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
        this.mvnpmLoader = new URLClassLoader(mvnpmJars.toArray(new URL[] {}));
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
            InputStream is = mvnpmLoader.getResourceAsStream(BASE_DIR + fullPath);
            if (is != null) {
                byte[] contents = is.readAllBytes();
                event.response()
                        .putHeader(HttpHeaders.CONTENT_TYPE, getContentType(fileName))
                        .end(Buffer.buffer(contents));
                return;
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        event.next();
    }

    private String getContentType(String filename) {
        String f = filename.toLowerCase();
        if (f.endsWith(DOT_JS)) {
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

        return CONTENT_TYPE_TEXT; // default

    }

    private static final String SLASH = "/";
    private static final String BASE_DIR = "META-INF/resources";
    private static final String DOT = ".";
    private static final String DOT_JS = ".js";
    private static final String DOT_JSON = ".json";
    private static final String DOT_HTML = ".html";
    private static final String DOT_HTM = ".htm";
    private static final String DOT_XHTML = ".xhtml";
    private static final String DOT_CSS = ".css";
    private static final String DOT_XML = ".xml";

    private static final String CONTENT_TYPE_JAVASCRIPT = "application/javascript";
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String CONTENT_TYPE_HTML = "text/html";
    private static final String CONTENT_TYPE_XHTML = "application/xhtml+xml";
    private static final String CONTENT_TYPE_XML = "application/xml";
    private static final String CONTENT_TYPE_CSS = "text/css";
    private static final String CONTENT_TYPE_TEXT = "text/plain";

}
