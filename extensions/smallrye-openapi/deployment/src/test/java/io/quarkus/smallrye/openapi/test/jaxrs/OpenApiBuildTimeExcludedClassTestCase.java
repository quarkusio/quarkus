package io.quarkus.smallrye.openapi.test.jaxrs;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.arc.profile.UnlessBuildProfile;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.arc.properties.UnlessBuildProperty;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

class OpenApiBuildTimeExcludedClassTestCase {

    static String quarkusProfile;

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest().withApplicationRoot((jar) -> jar
            .addClasses(IfBuildProfileTest.class, IfBuildProfileBar.class, IfBuildPropertyBarBazIsTrue.class,
                    IfBuildProperyFooBarIsTrue.class, UnlessBuildProfileBar.class, UnlessBuildProfileTest.class,
                    UnlessBuildPropertyBarBazIsFalse.class, UnlessBuildProperyFooBarIsFalse.class)
            .addAsResource(
                    new StringAsset(
                            "%test.foobar=true\n" + "%test.barbaz=false\n" + "foobar=false\n" + "barbaz=true\n"),
                    "application.properties"));

    @Test
    void testAutoSecurityRequirement() {
        RestAssured.given().header("Accept", "application/json").when().get("/q/openapi").then().log().body()
                .body("paths", aMapWithSize(4))

                .body("paths", hasKey("/test-profile-enabled")).body("paths", not(hasKey("/test-profile-not-enabled")))
                .body("paths", hasKey("/bar-profile-not-enabled")).body("paths", not(hasKey("/bar-profile-enabled")))

                .body("paths", hasKey("/foobar-property-true")).body("paths", hasKey("/foobar-property-not-false"))
                .body("paths", not(hasKey("/barbaz-property-true")))
                .body("paths", not(hasKey("/barbaz-property-not-false")));
    }

    @Path("/test-profile-enabled")
    @IfBuildProfile("test")
    public static class IfBuildProfileTest {
        @GET
        public String endpoint() {
            return "";
        }
    }

    @Path("/bar-profile-enabled")
    @IfBuildProfile("bar")
    public static class IfBuildProfileBar {
        @GET
        public String endpoint() {
            return "";
        }
    }

    @Path("/test-profile-not-enabled")
    @UnlessBuildProfile("test")
    public static class UnlessBuildProfileTest {
        @GET
        public String endpoint() {
            return "";
        }
    }

    @Path("/bar-profile-not-enabled")
    @UnlessBuildProfile("bar")
    public static class UnlessBuildProfileBar {
        @GET
        public String endpoint() {
            return "";
        }
    }

    @Path("/foobar-property-true")
    @IfBuildProperty(name = "foobar", stringValue = "true", enableIfMissing = false)
    public static class IfBuildProperyFooBarIsTrue {
        @GET
        public String endpoint() {
            return "";
        }
    }

    @Path("/barbaz-property-true")
    @IfBuildProperty(name = "barbaz", stringValue = "true", enableIfMissing = false)
    public static class IfBuildPropertyBarBazIsTrue {
        @GET
        public String endpoint() {
            return "";
        }
    }

    @Path("/foobar-property-not-false")
    @UnlessBuildProperty(name = "foobar", stringValue = "false", enableIfMissing = false)
    public static class UnlessBuildProperyFooBarIsFalse {
        @GET
        public String endpoint() {
            return "";
        }
    }

    @Path("/barbaz-property-not-false")
    @UnlessBuildProperty(name = "barbaz", stringValue = "false", enableIfMissing = false)
    public static class UnlessBuildPropertyBarBazIsFalse {
        @GET
        public String endpoint() {
            return "";
        }
    }

}
