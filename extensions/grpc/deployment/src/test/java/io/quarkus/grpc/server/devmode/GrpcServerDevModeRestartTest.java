package io.quarkus.grpc.server.devmode;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.grpc.runtime.GrpcServer;
import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.value.registry.ValueRegistry;

public class GrpcServerDevModeRestartTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(GrpcServerEndpoint.class));

    @Test
    void grpcServerPortAfterRestart() {
        when().get("/grpc-server").then().statusCode(200).body(equalTo("before restart 8080"));

        config.modifySourceFile(GrpcServerDevModeRestartTest.class, s -> s.replace("before restart", "after restart"));

        when().get("/grpc-server").then().statusCode(200).body(equalTo("after restart 8080"));
    }

    @Path("/grpc-server")
    @ApplicationScoped
    public static class GrpcServerEndpoint {
        @Inject
        ValueRegistry valueRegistry;

        @GET
        public String port() {
            return "before restart " + valueRegistry.get(GrpcServer.GRPC_SERVER).getPort();
        }
    }
}
