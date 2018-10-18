package org.jboss.shamrock.maven.runner;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.instrument.ClassDefinition;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.shamrock.maven.CopyUtils;

import org.fakereplace.core.Fakereplace;
import org.fakereplace.replacement.AddedClass;

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
    private final ClassLoaderCompiler compiler;

    static final UpdateHandler FAKEREPLACE_HANDLER;

    static {
        UpdateHandler fr;
        try {
            Class.forName("org.fakereplace.core.Fakereplace");
            fr = new FakereplaceHandler();
        } catch (Exception e) {
            fr = null;
        }
        FAKEREPLACE_HANDLER = fr;
    }

    public RuntimeUpdatesHandler(HttpHandler next, Path classesDir, Path sourcesDir, ClassLoaderCompiler compiler) {
        this.next = next;
        this.classesDir = classesDir;
        this.sourcesDir = sourcesDir;
        this.compiler = compiler;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {

        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
        }
        if (nextUpdate > System.currentTimeMillis()) {
            if (RunMojoMain.deploymentProblem != null) {
                ReplacementDebugPage.handleRequest(exchange, RunMojoMain.deploymentProblem);
                return;
            }
            next.handleRequest(exchange);
            return;
        }
        synchronized (this) {
            if (nextUpdate < System.currentTimeMillis()) {
                doScan();
                //we update at most once every 2s
                nextUpdate = System.currentTimeMillis() + TWO_SECONDS;

            }
        }
        if (RunMojoMain.deploymentProblem != null) {
            ReplacementDebugPage.handleRequest(exchange, RunMojoMain.deploymentProblem);
            return;
        }
        next.handleRequest(exchange);
    }

    private void doScan() throws IOException {
        final Set<File> changedSourceFiles;
        final long start = System.currentTimeMillis();
        if (sourcesDir != null) {
            try (final Stream<Path> sourcesStream = Files.walk(sourcesDir)) {
                changedSourceFiles = sourcesStream
                        .parallel()
                        .filter(p -> p.toString().endsWith(".java"))
                        .filter(p -> wasRecentlyModified(p))
                        .map(Path::toFile)
                        //Needing a concurrent Set, not many standard options:
                        .collect(Collectors.toCollection(ConcurrentSkipListSet::new));
            }
        } else {
            changedSourceFiles = Collections.EMPTY_SET;
        }
        if (!changedSourceFiles.isEmpty()) {
            try {
                compiler.compile(changedSourceFiles);
            } catch (Exception e) {
                RunMojoMain.deploymentProblem = e;
                return;
            }
        }
        final ConcurrentMap<String, byte[]> changedClasses;
        try (final Stream<Path> classesStream = Files.walk(classesDir)) {
            changedClasses = classesStream
                    .parallel()
                    .filter(p -> p.toString().endsWith(".class"))
                    .filter(p -> wasRecentlyModified(p))
                    .collect(Collectors.toConcurrentMap(
                            p -> pathToClassName(p),
                            p -> CopyUtils.readFileContentNoIOExceptions( p))
                    );
        }
        if (changedClasses.isEmpty()) {
            return;
        }

        lastChange = System.currentTimeMillis();

        if (FAKEREPLACE_HANDLER == null) {
            RunMojoMain.restartApp(false);
        } else {
            FAKEREPLACE_HANDLER.handle(changedClasses);
            RunMojoMain.restartApp(true);
        }
        System.out.println("Hot replace total time: " + (System.currentTimeMillis() - start) + "ms");
    }

    private boolean wasRecentlyModified(final Path p) {
        try {
            return Files.getLastModifiedTime(p).toMillis() > lastChange;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String pathToClassName(final Path path) {
        String pathName = classesDir.relativize(path).toString();
        String className = pathName.substring(0, pathName.length() - 6).replace("/", ".");
        return className;
    }

    interface UpdateHandler {

        void handle(Map<String, byte[]> changed);

    }

    private static class FakereplaceHandler implements UpdateHandler {

        @Override
        public void handle(Map<String, byte[]> changed) {
            ClassDefinition[] classes = new ClassDefinition[changed.size()];
            int c = 0;
            for (Map.Entry<String, byte[]> e : changed.entrySet()) {
                ClassDefinition cd = null;
                try {
                    cd = new ClassDefinition(Class.forName(e.getKey(), false, RunMojoMain.getCurrentAppClassLoader()), e.getValue());
                } catch (ClassNotFoundException e1) {
                    //TODO: added classes
                    throw new RuntimeException(e1);
                }
                classes[c++] = cd;

            }
            Fakereplace.redefine(classes, new AddedClass[0], true);
        }
    }

}
