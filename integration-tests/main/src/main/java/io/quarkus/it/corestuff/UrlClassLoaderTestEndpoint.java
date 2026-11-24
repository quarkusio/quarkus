package io.quarkus.it.corestuff;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Reads a resource from a jar through a {@link URLClassLoader} created at run time. In a native image this goes
 * through the JDK's {@code URLClassPath.JarLoader}, which Quarkus used to substitute away.
 * <p>
 * The answer is the content of the resource when the class loader finds it, and otherwise a report of what each
 * lookup returned, so that a failure in a native image explains itself.
 */
@WebServlet(name = "CoreUrlClassLoaderTestEndpoint", urlPatterns = "/core/url-class-loader")
public class UrlClassLoaderTestEndpoint extends HttpServlet {

    private static final String ENTRY = "greeting.txt";
    private static final String CONTENT = "hello from the jar";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Path jar = Files.createTempFile("url-class-loader", ".jar");
        try {
            writeJar(jar);
            URL jarUrl = jar.toUri().toURL();
            try (URLClassLoader loader = new URLClassLoader(new URL[] { jarUrl }, null)) {
                InputStream stream = loader.getResourceAsStream(ENTRY);
                if (stream != null) {
                    try (stream) {
                        resp.getWriter().write(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
                    }
                    return;
                }
                resp.getWriter().write(report(loader, jar, jarUrl));
            }
        } finally {
            Files.deleteIfExists(jar);
        }
    }

    private static String report(URLClassLoader loader, Path jar, URL jarUrl) {
        StringBuilder report = new StringBuilder("MISSING");
        report.append(" urls=").append(Arrays.toString(loader.getURLs()));
        report.append(" findResource=").append(loader.findResource(ENTRY));
        report.append(" getResource=").append(loader.getResource(ENTRY));
        try (JarFile jarFile = new JarFile(jar.toFile())) {
            report.append(" jarFileEntry=").append(jarFile.getJarEntry(ENTRY) != null);
        } catch (IOException e) {
            report.append(" jarFile=").append(e);
        }
        try (InputStream stream = URI.create("jar:" + jarUrl + "!/" + ENTRY).toURL().openStream()) {
            report.append(" jarUrl=").append(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            report.append(" jarUrl=").append(e);
        }
        return report.toString();
    }

    private static void writeJar(Path jar) throws IOException {
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
            out.putNextEntry(new JarEntry(ENTRY));
            out.write(CONTENT.getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
    }
}
