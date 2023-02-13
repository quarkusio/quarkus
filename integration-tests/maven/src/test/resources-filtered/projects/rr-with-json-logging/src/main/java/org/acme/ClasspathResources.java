package org.acme;

import org.apache.commons.io.IOUtils;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.function.Supplier;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Path("/classpathResources")
public class ClasspathResources {

    private static final String SUCCESS = "success";

    @GET
    public String readClassPathResources() {
        return runAssertions(
                () -> assertInvalidExactFileLocation(),
                () -> assertCorrectExactFileLocation(),
                () -> assertInvalidDirectory(),
                () -> assertCorrectDirectory(),
                () -> assertMultiRelease(),
                () -> assertUniqueDirectories()
        );
    }

    private String runAssertions(Supplier<String>... assertions) {
        String result;
        for (Supplier<String> assertion : assertions) {
            result = assertion.get();
            if (!SUCCESS.equals(result)) {
                return result;
            }
        }
        return SUCCESS;
    }

    private String assertInvalidExactFileLocation() {
        final String testType = "invalid-exact-location";
        try {
            Enumeration<URL> exactFileLocationEnumeration = this.getClass().getClassLoader().getResources("db/location/test2.sql");
            List<URL> exactFileLocationList = urlList(exactFileLocationEnumeration);
            if (exactFileLocationList.size() != 0) {
                return errorResult(testType, "wrong number of urls");
            }
            return SUCCESS;
        } catch (Exception e) {
            e.printStackTrace();
            return errorResult(testType, "exception during resolution of resource");
        }
    }

    private String assertMultiRelease() {
        final String testType = "assert-multi-release-jar";
        if (System.getProperty("java.version").startsWith("1.")) {
            return SUCCESS;
        }
        try {
            //this class is only present in multi release jars
            //for fast-jar we need to make sure it is loaded correctly
            Class<?> clazz = this.getClass().getClassLoader().loadClass("io.smallrye.context.Jdk9CompletableFutureWrapper");
            if (clazz.getClassLoader() == getClass().getClassLoader()) {
                return SUCCESS;
            }
            return errorResult(testType, "Incorrect ClassLoader for " + clazz);
        } catch (Exception e) {
            e.printStackTrace();
            return errorResult(testType, "exception during resolution of resource");
        }
    }
    private String assertCorrectExactFileLocation() {
        final String testType = "correct-exact-location";
        try {
            Enumeration<URL> exactFileLocationEnumeration = this.getClass().getClassLoader().getResources("db/location/test.sql");
            List<URL> exactFileLocationList = urlList(exactFileLocationEnumeration);
            if (exactFileLocationList.size() != 1) {
                return errorResult(testType, "wrong number of urls");
            }
            String fileContent = IOUtils.toString(exactFileLocationList.get(0).toURI(), StandardCharsets.UTF_8);
            if (!fileContent.contains("CREATE TABLE")) {
                return errorResult(testType, "wrong file content");
            }
            return SUCCESS;
        } catch (Exception e) {
            e.printStackTrace();
            return errorResult(testType, "exception during resolution of resource");
        }
    }

    private String assertInvalidDirectory() {
        final String testType = "invalid-directory";
        try {
            Enumeration<URL> exactFileLocationEnumeration = this.getClass().getClassLoader().getResources("db/location2");
            List<URL> exactFileLocationList = urlList(exactFileLocationEnumeration);
            if (exactFileLocationList.size() != 0) {
                return errorResult(testType, "wrong number of urls");
            }
            return SUCCESS;
        } catch (Exception e) {
            e.printStackTrace();
            return errorResult(testType, "exception during resolution of resource");
        }
    }

    private String assertCorrectDirectory() {
        final String testType = "correct-directory";
        try {
            Enumeration<URL> directoryEnumeration = this.getClass().getClassLoader().getResources("db/location");
            List<URL> directoryURLList = urlList(directoryEnumeration);
            if (directoryURLList.size() != 1) {
                return errorResult(testType, "wrong number of directory urls");
            }

            URL singleURL = directoryURLList.get(0);

            int separatorIndex = singleURL.getPath().lastIndexOf('!');
            String jarPath = singleURL.getPath().substring(0, separatorIndex);
            String directoryName = singleURL.getPath().substring(separatorIndex + 2) + "/";

            try (JarFile jarFile = new JarFile(Paths.get(new URI(jarPath)).toFile())) {
                Enumeration<JarEntry> entries = jarFile.entries();
                List<JarEntry> entriesInDirectory = new ArrayList<>();
                while (entries.hasMoreElements()) {
                    JarEntry currentEntry = entries.nextElement();
                    String entryName = currentEntry.getName();
                    if (entryName.startsWith(directoryName) && !entryName.equals(directoryName)) {
                        entriesInDirectory.add(currentEntry);
                    }
                }

                if (entriesInDirectory.size() != 1) {
                    return errorResult(testType, "wrong number of entries in jar directory");
                }

                try (InputStream is = jarFile.getInputStream(entriesInDirectory.get(0))) {
                    String fileContent = IOUtils.toString(is, StandardCharsets.UTF_8);
                    if (!fileContent.contains("CREATE TABLE")) {
                        return errorResult(testType, "wrong file content");
                    }
                    return SUCCESS;
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
            return errorResult(testType, "exception during resolution of resource");
        }
    }

    private String assertUniqueDirectories() {
        final String testType = "unique-directories";
        try {
            Enumeration<URL> resources = this.getClass().getClassLoader().getResources("META-INF/kie.conf");
            List<URL> resourcesList = Collections.list(resources);
            // 'META-INF/kie.conf' should be present in 'kie-internal', 'drools-core', 'drools-compiler' and 'drools-model-compiler'
            if (resourcesList.size() != 4) {
                return errorResult(testType, "wrong number of directory urls");
            }
            return SUCCESS;
        } catch (Exception e) {
            e.printStackTrace();
            return errorResult(testType, "exception during resolution of resource");
        }
    }

    private List<URL> urlList(Enumeration<URL> enumeration) {
        if (enumeration == null) {
            return Collections.emptyList();
        }
        List<URL> result = new ArrayList<>();
        while (enumeration.hasMoreElements()) {
            result.add(enumeration.nextElement());
        }
        return result;
    }

    private String errorResult(String testType, String message) {
        return testType + " / " + message;
    }
}
