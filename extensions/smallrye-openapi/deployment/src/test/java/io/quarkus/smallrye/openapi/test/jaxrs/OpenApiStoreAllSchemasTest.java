package io.quarkus.smallrye.openapi.test.jaxrs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

class OpenApiStoreAllSchemasTest {

    private static final String directory = "target/generated/all-schemas/";
    private static final String adminDirectory = "target/generated/all-schemas-admin/";

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(ProfilesResource.class))
            .overrideConfigKey("quarkus.smallrye-openapi.store-schemas-directory", directory)
            .overrideConfigKey("quarkus.smallrye-openapi.user.scan-profiles", "user")
            .overrideConfigKey("quarkus.smallrye-openapi.internal.scan-profiles", "internal")
            // the "admin" document configures its own directory, which takes precedence
            .overrideConfigKey("quarkus.smallrye-openapi.admin.scan-profiles", "admin")
            .overrideConfigKey("quarkus.smallrye-openapi.admin.store-schema-directory", adminDirectory);

    @Test
    void testStoresSchemaForAllConfiguredDocuments() throws IOException {
        Assertions.assertTrue(Files.exists(Paths.get(directory, "openapi.json")));
        Assertions.assertTrue(Files.exists(Paths.get(directory, "openapi.yaml")));

        Assertions.assertTrue(Files.exists(Paths.get(directory, "openapi-user.json")));
        Assertions.assertTrue(Files.exists(Paths.get(directory, "openapi-user.yaml")));
        Assertions.assertTrue(Files.exists(Paths.get(directory, "openapi-internal.json")));
        Assertions.assertTrue(Files.exists(Paths.get(directory, "openapi-internal.yaml")));

        String userDocument = Files.readString(Paths.get(directory, "openapi-user.json"));
        Assertions.assertTrue(userDocument.contains("/api/user"));
        Assertions.assertFalse(userDocument.contains("/api/internal"));
        Assertions.assertFalse(userDocument.contains("/api/admin"));
    }

    @Test
    void testDocumentSpecificDirectoryTakesPrecedence() {
        Assertions.assertTrue(Files.exists(Paths.get(adminDirectory, "openapi-admin.json")));
        Assertions.assertTrue(Files.exists(Paths.get(adminDirectory, "openapi-admin.yaml")));

        Assertions.assertFalse(Files.exists(Paths.get(directory, "openapi-admin.json")));
        Assertions.assertFalse(Files.exists(Paths.get(directory, "openapi-admin.yaml")));
    }

    @Path("/api")
    public static class ProfilesResource {

        @GET
        @Path("/user")
        @Extension(name = "x-smallrye-profile-user", value = "")
        public String user() {
            return "user";
        }

        @GET
        @Path("/internal")
        @Extension(name = "x-smallrye-profile-internal", value = "")
        public String internal() {
            return "internal";
        }

        @GET
        @Path("/admin")
        @Extension(name = "x-smallrye-profile-admin", value = "")
        public String admin() {
            return "admin";
        }

        @GET
        @Path("/no-profile")
        public String noProfile() {
            return "no-profile";
        }
    }
}
