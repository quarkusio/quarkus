package io.quarkus.it.nat.annotation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.ResourceBundle;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

@Path("/native-image-annotations")
public class NativeImageAnnotationsResource {

    @Path("/access-classpath-resource")
    @Produces("text/plain")
    @GET
    public String accessClasspathResource() {
        try (var resourceInputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("file.txt")) {
            if (resourceInputStream == null)
                return "Resource not found";
            return new String(resourceInputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Path("/access-resource-bundle/en")
    @Produces("text/plain")
    @GET
    public String accessResourceBundleEn() {
        var resourceBundle = ResourceBundle.getBundle("messages", Locale.ENGLISH);
        if (resourceBundle == null)
            return "Resource bundle not found";
        return resourceBundle.getString("message.sayHello");
    }

    @Path("/access-resource-bundle/ar")
    @Produces("text/plain")
    @GET
    public String accessResourceBundleAr() {
        var resourceBundle = ResourceBundle.getBundle("messages", Locale.forLanguageTag("ar"));
        if (resourceBundle == null)
            return "Resource bundle not found";
        return resourceBundle.getString("message.sayHello");
    }

}
