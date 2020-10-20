package io.quarkus.it.smallrye.config;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.arc.profile.UnlessBuildProfile;

@Path("/profiles")
@Consumes(MediaType.TEXT_PLAIN)
@Produces(MediaType.TEXT_PLAIN)
public class MultipleProfilesResource {
    @Inject
    Instance<ActiveProfile> activeProfiles;

    @GET
    public Response getActiveProfiles() {
        return Response.ok(activeProfiles.stream()
                .map(ActiveProfile::value)
                .reduce((integer, integer2) -> integer | integer2)
                .orElse(0)).build();
    }

    public interface ActiveProfile {
        int value();
    }

    @ApplicationScoped
    @IfBuildProfile("common")
    public static class CommonProfile implements ActiveProfile {
        @Override
        public int value() {
            return 1;
        }
    }

    @ApplicationScoped
    @IfBuildProfile("test")
    public static class TestProfile implements ActiveProfile {
        @Override
        public int value() {
            return 2;
        }
    }

    @ApplicationScoped
    @IfBuildProfile("prod")
    public static class ProdProfile implements ActiveProfile {
        @Override
        public int value() {
            return 4;
        }
    }

    @ApplicationScoped
    @UnlessBuildProfile("main")
    public static class MainProfile implements ActiveProfile {
        @Override
        public int value() {
            return 8;
        }
    }
}
