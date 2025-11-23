package io.quarkus.vertx.http.runtime;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.runtime.annotations.IgnoreProperty;
import io.quarkus.runtime.annotations.RecordableConstructor;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.LRUCache;

public class HttpStaticDirHandler implements Handler<RoutingContext> {

    public Map<String, String> staticFiles;
    @IgnoreProperty
    private LRUCache<String, byte[]> cache;

    @RecordableConstructor
    public HttpStaticDirHandler(Map<String, String> staticFiles) {
        this.staticFiles = (staticFiles != null) ? staticFiles : Map.of();
        this.cache = new LRUCache<>(resolveCacheSize());
    }

    private int resolveCacheSize() {
        Config config = ConfigProvider.getConfig();
        return config.getOptionalValue(
                "quarkus.http.static-dir.cache-size",
                Integer.class).orElse(100);
    }

    @Override
    public void handle(RoutingContext ctx) {
        String rawPath = ctx.request().path();
        String lowerRaw = rawPath.toLowerCase();

        if (rawPath.contains("..") || lowerRaw.contains("%2e%2e")) {
            ctx.response().setStatusCode(404).end();
            return;
        }

        String decodedPath = ctx.normalizedPath();

        if (staticFiles == null || staticFiles.isEmpty()) {
            ctx.response().setStatusCode(404).end();
            return;
        }

        String filePath = staticFiles.get(decodedPath);
        if (filePath == null) {
            ctx.response().setStatusCode(404).end();
            return;
        }

        if (cache == null) {
            cache = new LRUCache<>(resolveCacheSize());
        }

        byte[] content = cache.get(decodedPath);
        if (content == null) {
            try {
                Path p = Path.of(filePath);
                content = Files.readAllBytes(p);
                cache.put(decodedPath, content);
            } catch (IOException e) {
                ctx.response().setStatusCode(500).end("Failed to read file");
                return;
            }
        }

        ctx.response()
                .putHeader("content-type", detectMime(Path.of(filePath)))
                .end(Buffer.buffer(content));
    }

    private String detectMime(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        if (name.endsWith(".txt"))
            return "text/plain;charset=UTF-8";
        if (name.endsWith(".json"))
            return "application/json";
        if (name.endsWith(".html"))
            return "text/html;charset=UTF-8";
        if (name.endsWith(".js"))
            return "application/javascript";
        if (name.endsWith(".css"))
            return "text/css";
        return "application/octet-stream";
    }
}