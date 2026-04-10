package io.quarkus.it.nat.annotation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.Function;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

@Path("/native-image-annotations")
public class NativeImageAnnotationsResource {

    @Path("/access-classpath-resource")
    @Produces("text/plain")
    @GET
    public String accessClasspathResource() {
        try (InputStream resourceInputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("file.txt")) {
            if (resourceInputStream == null) {
                return "Resource not found";
            }
            return new String(resourceInputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Path("/access-resource-bundle/en")
    @Produces("text/plain")
    @GET
    public String accessResourceBundleEn() {
        final ResourceBundle resourceBundle = ResourceBundle.getBundle("messages", Locale.ENGLISH);
        if (resourceBundle == null) {
            return "Resource bundle not found";
        }
        return resourceBundle.getString("message.sayHello");
    }

    @Path("/access-resource-bundle/ar")
    @Produces("text/plain")
    @GET
    public String accessResourceBundleAr() {
        final ResourceBundle resourceBundle = ResourceBundle.getBundle("messages", Locale.forLanguageTag("ar"));
        if (resourceBundle == null) {
            return "Resource bundle not found";
        }
        return resourceBundle.getString("message.sayHello");
    }

    @Path("/serialize-proper")
    @Produces("text/plain")
    @GET
    public String serializeProper() {
        try {
            /* @formatter:off
             * GraalVM/Mandrel Static Analysis intercepts this exact string literal at build time
             * and registers the lambda for serialization.
             * See https://www.graalvm.org/latest/reference-manual/native-image/metadata/#serialization-metadata-registration-in-code
             * @formatter:on
             */
            final ObjectInputFilter filter = ObjectInputFilter.Config
                    .createFilter("io.quarkus.it.nat.annotation.ProperLambdaHolder$$Lambda*;");
            final Function<String, String> deserialized = roundTrip(ProperLambdaHolder.getLambda(), filter);
            return deserialized.apply("SUCCESS");
        } catch (Exception e) {
            return "FAIL: " + e.getMessage();
        }
    }

    @Path("/serialize-legacy")
    @Produces("text/plain")
    @GET
    public String serializeLegacy() {
        try {
            // ObjectInputFilter not created, relies on the legacy @RegisterForReflection/
            // That JSON array is ignored as of issue-41016, so this fails at runtime.
            final Function<String, String> deserialized = roundTrip(LegacyLambdaHolder.getLambda(), null);
            return deserialized.apply("SHOULD_NOT_REACH_HERE");
        } catch (Exception e) {
            // MissingReflectionRegistrationError or similar serialization failure
            return "EXPECTED_FAILURE";
        }
    }

    @SuppressWarnings("unchecked")
    private Function<String, String> roundTrip(Function<String, String> func, ObjectInputFilter filter) throws Exception {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(func);
        }
        final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        try (ObjectInputStream ois = new ObjectInputStream(bais)) {
            if (filter != null) {
                ois.setObjectInputFilter(filter);
            }
            return (Function<String, String>) ois.readObject();
        }
    }
}
