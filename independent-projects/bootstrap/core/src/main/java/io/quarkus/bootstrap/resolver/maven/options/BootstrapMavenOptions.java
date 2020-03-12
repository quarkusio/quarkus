package io.quarkus.bootstrap.resolver.maven.options;

import io.quarkus.bootstrap.util.PropertyUtils;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class resolves relevant Maven command line options in case it's called
 * from a Maven build process. Maven internally uses org.apache.commons.cli.* API
 * besides Maven-specific API. This class locates the Maven's lib directory that
 * was used to launch the build process and loads the necessary classes from that
 * lib directory.
 */
public class BootstrapMavenOptions {

    private static final String ACTIVATE_PROFILES = "P";

    public static Map<String, Object> parse(String cmdLine) {
        if (cmdLine == null) {
            return Collections.emptyMap();
        }
        final String[] args = cmdLine.split("\\s+");
        if (args.length == 0) {
            return Collections.emptyMap();
        }

        final String mavenHome = PropertyUtils.getProperty("maven.home");
        if (mavenHome == null) {
            return invokeParser(Thread.currentThread().getContextClassLoader(), args);
        }

        final Path mvnLib = Paths.get(mavenHome).resolve("lib");
        if (!Files.exists(mvnLib)) {
            throw new IllegalStateException("Maven lib dir does not exist: " + mvnLib);
        }
        final URL[] urls;
        try (Stream<Path> files = Files.list(mvnLib)) {
            final List<URL> list = files.map(p -> {
                try {
                    return p.toUri().toURL();
                } catch (MalformedURLException e) {
                    throw new IllegalStateException("Failed to translate " + p + " to URL", e);
                }
            }).collect(Collectors.toCollection(ArrayList::new));

            list.add(getClassOrigin(BootstrapMavenOptions.class).toUri().toURL());
            urls = list.toArray(new URL[list.size()]);

        } catch (Exception e) {
            throw new IllegalStateException("Failed to create a URL list out of " + mvnLib + " content", e);
        }
        final ClassLoader originalCl = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader ucl = new URLClassLoader(urls, null)) {
            Thread.currentThread().setContextClassLoader(ucl);
            return invokeParser(ucl, args);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to close URL classloader", e);
        } finally {
            Thread.currentThread().setContextClassLoader(originalCl);
        }
    }

    public static BootstrapMavenOptions newInstance(String cmdLine) {
        return new BootstrapMavenOptions(parse(cmdLine));
    }

    private final Map<String, Object> options;
    private List<String> activeProfileIds;
    private List<String> inactiveProfileIds;

    private BootstrapMavenOptions(Map<String, Object> options) {
        this.options = options;
    }

    public boolean hasOption(String name) {
        return options.containsKey(name);
    }

    public String getOptionValue(String name) {
        final Object o = options.get(name);
        return o == null ? null : o.toString();
    }

    public String[] getOptionValues(String name) {
        final Object o = options.get(name);
        if (o == null) {
            return null;
        }
        if (o instanceof String[]) {
            return (String[]) o;
        }
        return new String[] { o.toString() };
    }

    public boolean isEmpty() {
        return options.isEmpty();
    }

    public List<String> getActiveProfileIds() {
        if (activeProfileIds == null) {
            parseProfileArgs();
        }
        return activeProfileIds;
    }

    public List<String> getInactiveProfileIds() {
        if (inactiveProfileIds == null) {
            parseProfileArgs();
        }
        return inactiveProfileIds;
    }

    private void parseProfileArgs() {
        final String[] profileOptionValues = getOptionValues(ACTIVATE_PROFILES);
        if (profileOptionValues != null && profileOptionValues.length > 0) {
            final List<String> activeProfiles = new ArrayList<>(0);
            final List<String> inactiveProfiles = new ArrayList<>(0);
            for (String profileOptionValue : profileOptionValues) {
                final StringTokenizer profileTokens = new StringTokenizer(profileOptionValue, ",");
                while (profileTokens.hasMoreTokens()) {
                    final String profileAction = profileTokens.nextToken().trim();
                    if (profileAction.isEmpty()) {
                        continue;
                    }
                    final char c = profileAction.charAt(0);
                    if (c == '-' || c == '!') {
                        inactiveProfiles.add(profileAction.substring(1));
                    } else if (c == '+') {
                        activeProfiles.add(profileAction.substring(1));
                    } else {
                        activeProfiles.add(profileAction);
                    }
                }
            }
            this.activeProfileIds = activeProfiles;
            this.inactiveProfileIds = inactiveProfiles;
        } else {
            activeProfileIds = Collections.emptyList();
            inactiveProfileIds = Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> invokeParser(ClassLoader cl, String[] args) {
        try {
            final Class<?> parserCls = cl.loadClass("io.quarkus.bootstrap.resolver.maven.options.BootstrapMavenOptionsParser");
            final Method parseMethod = parserCls.getMethod("parse", String[].class);
            return (Map<String, Object>) parseMethod.invoke(null, (Object) args);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse command line arguments " + Arrays.asList(args), e);
        }
    }

    /**
     * Returns the JAR or the root directory that contains the class file that is on the
     * classpath of the context classloader
     */
    public static Path getClassOrigin(Class<?> cls) throws IOException {
        return getResourceOrigin(cls.getClassLoader(), cls.getName().replace('.', '/') + ".class");
    }

    public static Path getResourceOrigin(ClassLoader cl, final String name) throws IOException {
        URL url = cl.getResource(name);
        if (url == null) {
            throw new IOException("Failed to locate the origin of " + name);
        }
        String classLocation = url.toExternalForm();
        if (url.getProtocol().equals("jar")) {
            classLocation = classLocation.substring(4, classLocation.length() - name.length() - 2);
        } else {
            classLocation = classLocation.substring(0, classLocation.length() - name.length());
        }
        return urlSpecToPath(classLocation);
    }

    private static Path urlSpecToPath(String urlSpec) throws IOException {
        try {
            return Paths.get(new URL(urlSpec).toURI());
        } catch (Throwable e) {
            throw new IOException(
                    "Failed to create an instance of " + Path.class.getName() + " from " + urlSpec, e);
        }
    }
}
