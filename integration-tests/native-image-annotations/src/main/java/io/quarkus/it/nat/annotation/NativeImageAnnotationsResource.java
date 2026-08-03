package io.quarkus.it.nat.annotation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
            // Lambda serialization is handled via reachability-metadata.json
            // The capturing class and lambda descriptor are configured in ProperLambdaHolder's
            // RegisterForReflection annotation.
            final Function<String, String> deserialized = roundTrip(ProperLambdaHolder.getLambda());
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
            // This fails because LegacyLambdaHolder doesn't have proper metadata.
            final Function<String, String> deserialized = roundTrip(LegacyLambdaHolder.getLambda());
            return deserialized.apply("SHOULD_NOT_REACH_HERE");
        } catch (Exception e) {
            // Expected e.g. MissingReflectionRegistrationError or ClassNotFoundException
            return "EXPECTED_FAILURE";
        }
    }

    @SuppressWarnings("unchecked")
    private Function<String, String> roundTrip(Function<String, String> func) throws Exception {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(func);
        }
        final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        try (ObjectInputStream ois = new ObjectInputStream(bais)) {
            return (Function<String, String>) ois.readObject();
        }
    }
}
