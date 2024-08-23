package io.quarkus.test.devui;

import java.util.Iterator;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.grpc.examples.helloworld.Greeter;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.grpc.GrpcService;
import io.quarkus.test.QuarkusDevModeTest;
import io.smallrye.mutiny.Uni;

public class DevUIGrpcSmokeTest extends DevUIJsonRPCTest {

    @RegisterExtension
    static final QuarkusDevModeTest config = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar.addPackage(Greeter.class.getPackage())
                    .addClass(TestGreeter.class))
            .setCodeGenSources("proto");

    public DevUIGrpcSmokeTest() {
        super("io.quarkus.quarkus-grpc");
    }

    @Test
    public void testServices() throws Exception {
        JsonNode services = super.executeJsonRPCMethod("getServices");
        Assertions.assertNotNull(services);
        Assertions.assertTrue(services.isArray());
        Iterator<JsonNode> en = services.elements();
        boolean serviceExists = false;
        boolean methodExists = false;
        while (en.hasNext()) {
            JsonNode service = en.next();
            String name = service.get("name").asText();
            if (name.equals("helloworld.Greeter")) {
                serviceExists = true;
                JsonNode methods = service.get("methods");
                Assertions.assertNotNull(methods);
                Assertions.assertTrue(methods.isArray());
                Iterator<JsonNode> mi = methods.elements();
                while (mi.hasNext()) {
                    JsonNode method = mi.next();
                    String bareMethodName = method.get("bareMethodName").asText();
                    if (bareMethodName.equals("SayHello")) {
                        methodExists = true;
                        break;
                    }
                }
            }
        }

        Assertions.assertTrue(serviceExists);
        Assertions.assertTrue(methodExists);
    }

    @Test
    public void testTestService() throws Exception {
        Map<String, Object> params = Map.of(
                "serviceName", "helloworld.Greeter",
                "methodName", "SayHello",
                "methodType", "UNARY",
                "content", "{'name': 'Phillip'}");
        JsonNode helloReply = super.executeJsonRPCMethod("testService", params);
        Assertions.assertNotNull(helloReply);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode grpcMessage = mapper.readTree(helloReply.asText());
        String message = grpcMessage.get("message").asText();
        Assertions.assertNotNull(message);
        Assertions.assertEquals("Hola Phillip!", message);
    }

    @GrpcService
    public static class TestGreeter implements Greeter {

        @Override
        public Uni<HelloReply> sayHello(HelloRequest request) {
            return Uni.createFrom().item(HelloReply.newBuilder().setMessage("Hola " + request.getName() + "!").build());
        }

    }

}
