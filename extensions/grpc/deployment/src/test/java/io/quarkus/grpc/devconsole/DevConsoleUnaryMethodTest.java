package io.quarkus.grpc.devconsole;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.grpc.examples.helloworld.MutinyGreeterGrpc;
import io.quarkus.grpc.server.services.MutinyHelloService;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class DevConsoleUnaryMethodTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addPackage(MutinyGreeterGrpc.class.getPackage())
                    .addClass(MutinyHelloService.class));

    @Test
    public void testUnaryMethodCall() {
        RestAssured.with().body("{\n\"name\": \"Martin\"}")
                .post("q/dev/io.quarkus.quarkus-grpc/test?serviceName=helloworld.Greeter&methodName=SayHello")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("Hello Martin"));

    }

}
