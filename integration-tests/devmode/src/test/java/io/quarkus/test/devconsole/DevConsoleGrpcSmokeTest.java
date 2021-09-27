package io.quarkus.test.devconsole;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.grpc.examples.helloworld.Greeter;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.quarkus.grpc.GrpcService;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;
import io.smallrye.mutiny.Uni;

/**
 * Note that this test cannot be placed under the relevant {@code -deployment} module because then the DEV UI processor would
 * not be able to locate the template resources correctly.
 */
public class DevConsoleGrpcSmokeTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addPackage(Greeter.class.getPackage())
                    .addClass(TestGreeter.class))
            .setCodeGenSources("proto");

    @Test
    public void testServices() {
        RestAssured.get("q/dev/io.quarkus.quarkus-grpc/services")
                .then()
                .statusCode(200).body(Matchers.containsString("helloworld.Greeter"));
    }

    @GrpcService
    public static class TestGreeter implements Greeter {

        @Override
        public Uni<HelloReply> sayHello(HelloRequest request) {
            return Uni.createFrom().item(HelloReply.newBuilder().setMessage("Hola!").build());
        }

    }

}
