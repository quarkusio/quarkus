package io.quarkus.runtime.graal;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.graalvm.nativeimage.hosted.Feature;

public class WeakReflectionFeature implements Feature {

    public static final String META_INF_QUARKUS_NATIVE_WEAK_REFLECTION_JSON = "META-INF/quarkus-native-weak-reflection.json";

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        InputStream resourceAsStream = getClass().getClassLoader()
                .getResourceAsStream(META_INF_QUARKUS_NATIVE_WEAK_REFLECTION_JSON);
        if (resourceAsStream != null) {
            try (WeakReflectionConfigParser parser = new WeakReflectionConfigParser(resourceAsStream)) {
                while (parser.hasNext()) {
                    Map<String, Object> objectProperties = parser.next();
                    registerForWeakReflection((String) objectProperties.get("name"),
                            (Boolean) objectProperties.get("constructors"), (Boolean) objectProperties.get("methods"),
                            (Boolean) objectProperties.get("fields"), access);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public String getDescription() {
        return "Register each class in " + META_INF_QUARKUS_NATIVE_WEAK_REFLECTION_JSON
                + " for weak reflection on Substrate VM";
    }

    private static void registerForWeakReflection(String className, boolean constructors, boolean methods,
            boolean fields, BeforeAnalysisAccess beforeAnalysisAccess) {
        try {
            Class<?> aClass = Class.forName(className, false, Thread.currentThread().getContextClassLoader());
            WeakReflection.register(beforeAnalysisAccess, aClass, constructors, methods, fields);
        } catch (Throwable e) {
            // e.printStackTrace();
        }
    }

    /**
     * A specific Json parser based on regular expressions allowing to parse a weak reflection configuration file.
     */
    private static class WeakReflectionConfigParser implements Closeable {

        /**
         * The regular expression defining the delimiter of Json objects knowing that they are stored into a Json array.
         */
        private static final Pattern PATTERN_DELIMITER = Pattern.compile("\\[\\{|},\\{|}]");
        /**
         * The regular expression defining the format of a Json property.
         */
        private static final Pattern PATTERN_PROPERTY = Pattern.compile("\"([^\"]*)\":(true|false|\"[^\"]*\")");

        private final Scanner scanner;

        WeakReflectionConfigParser(InputStream input) {
            this.scanner = new Scanner(input, StandardCharsets.UTF_8).useDelimiter(PATTERN_DELIMITER);
        }

        /**
         * @return {@code true} if there is at least one more Json Object to parse, {@code false} otherwise.
         */
        boolean hasNext() {
            return scanner.hasNext();
        }

        /**
         * @return the next Json Object that could be found in a {@code Map} format.
         */
        Map<String, Object> next() {
            Matcher matcherProperty = PATTERN_PROPERTY.matcher(scanner.next());
            Map<String, Object> objectProperties = new HashMap<>();
            while (matcherProperty.find()) {
                String propertyName = matcherProperty.group(1);
                String propertyValue = matcherProperty.group(2);
                if ("name".equals(propertyName)) {
                    objectProperties.put(propertyName, propertyValue.substring(1, propertyValue.length() - 1));
                } else {
                    objectProperties.put(propertyName, Boolean.valueOf(propertyValue));
                }
            }
            return objectProperties;
        }

        @Override
        public void close() throws IOException {
            scanner.close();
        }
    }
}
