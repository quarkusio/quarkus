package io.quarkus.vertx.http.runtime;

import java.nio.file.Path;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface HttpStaticDirConfig {

    /**
     * Whether serving static resources from a local directory is enabled.
     * <p>
     * When disabled, the configured {@code endpoint} and {@code path} values are ignored
     * and no local filesystem content will be exposed over HTTP.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * The base HTTP endpoint under which the local directory is exposed.
     * <p>
     * /**
     * The base endpoint under which the local directory is exposed,
     * <p>
     * e.g. {@code /static} with hello.txt will be serve at <code>/static/hello.txt</code>.
     */
    @WithDefault("/static")
    String endpoint();

    /**
     * The local directory from which static files will be served,
     * relative to the application's working directory.
     * <p>
     * For example, if set to {@code static} and the directory contains
     * {@code index.html}, it will be available at the configured
     * {@code quarkus.http.static-dir.endpoint}/index.html.
     */
    @WithDefault("static")
    String path();

    /**
     * Normalizes and validates the local path from which files are served.
     *
     * @return a normalized directory string
     * @throws IllegalArgumentException if the directory contains {@code ..}
     */
    default String normalizedPath() {
        String d = path().trim();

        if (d.contains("..")) {
            throw new IllegalArgumentException(
                    "quarkus.http.static-dir.path must not contain '..': " + d);
        }

        Path p = Path.of(d);
        if (p.isAbsolute()) {
            throw new IllegalArgumentException(
                    "quarkus.http.static-dir.path must be relative, but was: " + d);
        }

        return d;
    }

    /**
     * Normalizes the configured path for serving endpoint.
     *
     * @return a normalized, safe base path for static resources
     * @throws IllegalArgumentException if the path contains {@code *} or {@code ..}
     */
    default String normalizedEndpoint() {

        String p = endpoint().trim();

        if (p.contains("*")) {
            throw new IllegalArgumentException(
                    "quarkus.http.static-dir.path must not contain '*': " + p);
        }

        if (p.contains("..")) {
            throw new IllegalArgumentException(
                    "quarkus.http.static-dir.path must not contain '..': " + p);
        }

        if (p.indexOf(' ') >= 0) {
            throw new IllegalArgumentException(
                    "quarkus.http.static-dir.path must not contain spaces: " + p);
        }

        if (p.contains("\\")) {
            throw new IllegalArgumentException(
                    "quarkus.http.static-dir.path must use '/' as separator: " + p);
        }

        if (p.isEmpty()) {
            return "/";
        }

        if (!p.startsWith("/")) {
            p = "/" + p;
        }

        while (p.endsWith("/") && p.length() > 1) {
            p = p.substring(0, p.length() - 1);
        }

        return p;
    }

}
