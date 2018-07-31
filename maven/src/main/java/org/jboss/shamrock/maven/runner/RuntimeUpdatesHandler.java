package org.jboss.shamrock.maven.runner;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.jboss.shamrock.undertow.runtime.UndertowDeploymentTemplate;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

public class RuntimeUpdatesHandler implements HttpHandler {

    private static final long TWO_SECONDS = 2000;

    private final HttpHandler next;
    private final Path classesDir;
    private final Path sourcesDir;
    private volatile long nextUpdate;
    private volatile long lastChange = System.currentTimeMillis();

    public RuntimeUpdatesHandler(HttpHandler next, Path classesDir, Path sourcesDir) {
        this.next = next;
        this.classesDir = classesDir;
        this.sourcesDir = sourcesDir;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {

        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
        }
        if (nextUpdate > System.currentTimeMillis()) {
            next.handleRequest(exchange);
            return;
        }
        synchronized (this) {
            if (nextUpdate < System.currentTimeMillis()) {
                try {
                    if(doScan()) {
                        //TODO: super hack alert
                        UndertowDeploymentTemplate.ROOT_HANDLER.handleRequest(exchange);
                        return;
                    }
                    //we update at most once every 2s
                    nextUpdate = System.currentTimeMillis() + TWO_SECONDS;

                } catch (Throwable e) {
                    displayErrorPage(exchange, e);
                    return;
                }
            }
        }
        next.handleRequest(exchange);
    }

    private boolean doScan() throws IOException {
        //TODO: this is super simple at the moment, if there are changes
        //we just restart the app which will drop the class loader
        //this will change considerably with Fakereplace
        final AtomicBoolean done = new AtomicBoolean();
        Files.walk(classesDir).forEach(new Consumer<Path>() {
            @Override
            public void accept(Path path) {
                if(done.get()) {
                    return;
                }
                try {
                    long lastModified = Files.getLastModifiedTime(path).toMillis();
                    if(lastModified > lastChange) {
                        done.set(true);
                        lastChange = System.currentTimeMillis();
                        RunMojoMain.restartApp();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        return done.get();
    }

    public static void displayErrorPage(HttpServerExchange exchange, final Throwable exception) throws IOException {
        StringBuilder sb = new StringBuilder();
        //todo: make this good
        sb.append("<html><head><title>ERROR</title>");
        sb.append("</head><body><div class=\"header\"><div class=\"error-div\"></div><div class=\"error-text-div\">Hot Class Change Error</div></div>");
        writeLabel(sb, "Stack Trace", "");

        sb.append("<pre>");
        StringWriter stringWriter = new StringWriter();
        exception.printStackTrace(new PrintWriter(stringWriter));
        sb.append(escapeBodyText(stringWriter.toString()));
        sb.append("</pre></body></html>");
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html; charset=UTF-8;");
        exchange.getResponseSender().send(sb.toString());
    }

    private static void writeLabel(StringBuilder sb, String label, String value) {
        sb.append("<div class=\"label\">");
        sb.append(escapeBodyText(label));
        sb.append(":</div><div class=\"value\">");
        sb.append(escapeBodyText(value));
        sb.append("</div><br/>");
    }

    public static String escapeBodyText(final String bodyText) {
        if (bodyText == null) {
            return "null";
        }
        return bodyText.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

}
