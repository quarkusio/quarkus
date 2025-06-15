package io.quarkus.grpc.server.devmode;

import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.example.test.MutinyStreamsGrpc;
import com.example.test.StreamsGrpc;
import com.example.test.StreamsOuterClass;

import devmodetest.v1.Devmodetest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.quarkus.test.QuarkusDevModeTest;
import io.smallrye.mutiny.Multi;

public class GrpcDevModeTest {
    @RegisterExtension
    public static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(DevModeTestService.class, DevModeTestStreamService.class, DevModeTestInterceptor.class,
                            DevModeTestRestResource.class, DevModeServiceCallingResource.class, DevModeService.class)
                    .addPackage(GreeterGrpc.class.getPackage()).addPackage(HelloReply.class.getPackage())
                    .addPackage(Devmodetest.class.getPackage()).addPackage(StreamsGrpc.class.getPackage())
                    .addPackage(StreamsOuterClass.Item.class.getPackage())
                    .addAsResource("dev-mode.properties", "application.properties"))
            .setCodeGenSources("proto");

    protected ManagedChannel channel;

    @BeforeEach
    public void init() {
        channel = ManagedChannelBuilder.forAddress("localhost", 9000).usePlaintext().build();
    }

    @AfterEach
    public void shutdown() throws InterruptedException {
        channel.shutdownNow().awaitTermination(2, TimeUnit.SECONDS);
    }

    @Test
    public void testInterceptorReload() {
        callHello("Winnie", ".*Winnie");

        assertThat(when().get("/test/interceptor-status").asString()).isEqualTo("status");

        test.modifySourceFile("DevModeTestInterceptor.java",
                text -> text.replace("return \"status\"", "return \"altered-status\""));

        callHello("Winnie", ".*Winnie");
        assertThat(when().get("/test/interceptor-status").asString()).isEqualTo("altered-status");
    }

    @Test
    public void testSingleReload() {
        callHello("Winnie", "Hello, Winnie");
        test.modifySourceFile("DevModeTestService.java",
                text -> text.replaceAll("String greeting = .*;", "String greeting = \"hello, \";"));
        callHello("Winnie", "hello, Winnie");
    }

    @Test
    public void testReloadAfterRest() {
        test.modifySourceFile("DevModeTestService.java",
                text -> text.replaceAll("String greeting = .*;", "String greeting = \"hell no, \";"));
        test.modifySourceFile("DevModeTestRestResource.java", text -> text.replace("testresponse", "testresponse2"));

        assertThat(when().get("/test").asString()).isEqualTo("testresponse2");
        callHello("Winnie", "hell no, Winnie");
    }

    @Test
    public void testReloadBeforeRest() {
        test.modifySourceFile("DevModeTestService.java",
                text -> text.replaceAll("String greeting = .*;", "String greeting = \"hell yes, \";"));
        test.modifySourceFile("DevModeTestRestResource.java", text -> text.replace("testresponse", "testresponse3"));

        callHello("Winnie", "hell yes, Winnie");
        assertThat(when().get("/test").asString()).isEqualTo("testresponse3");
    }

    @Test
    public void testEchoStreamReload() {
        final CopyOnWriteArrayList<String> results = new CopyOnWriteArrayList<>();
        CompletionStage<Boolean> firstStreamFinished = callEcho("foo", results);
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> results, Matchers.hasItem("echo::foo"));

        test.modifySourceFile("DevModeTestStreamService.java", text -> text.replace("echo::", "newecho::"));

        final CopyOnWriteArrayList<String> newResults = new CopyOnWriteArrayList<>();
        callEcho("foo", newResults);
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> newResults, Matchers.hasItem("newecho::foo"));
        assertThat(firstStreamFinished).isCompleted();
    }

    @Test
    public void testProtoFileChangeReload() throws InterruptedException {
        callHello("HACK_TO_GET_STATUS_NUMBER", "2");
        test.modifyFile("proto/devmodetest.proto", text -> text.replaceAll("TEST_ONE = .*;", "TEST_ONE = 15;"));
        Thread.sleep(5000); // to wait for eager reload for code gen sources to happen
        callHello("HACK_TO_GET_STATUS_NUMBER", "15");
    }

    @Test
    public void testProtoFileChangeAndImplementationReload() throws InterruptedException {
        when().get("/dev-mode-test").then().statusCode(200).body(equalTo("ORIGINAL_GET"));

        test.modifyFile("proto/devmodetest.proto", text -> text.replaceAll("// placeholder for a new method",
                "rpc StreamCheck(DevModeRequest) returns (stream DevModeResponse);"));

        test.modifySourceFile("DevModeService.java",
                text -> text.replace("// test will add override here", "@Override"));
        test.modifySourceFile("DevModeServiceCallingResource.java",
                text -> text.replace("\"ORIGINAL_GET\"", "responseFor(client.streamCheck(request))"));
        Thread.sleep(5000); // to wait for eager reload for code gen sources to happen
        when().get("/dev-mode-test").then().statusCode(200).body(equalTo("OKAY"));
    }

    private CompletionStage<Boolean> callEcho(String name, List<String> output) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        Multi<StreamsOuterClass.Item> request = Multi.createFrom().item(name)
                .map(StreamsOuterClass.Item.newBuilder()::setName).map(StreamsOuterClass.Item.Builder::build);
        Multi<StreamsOuterClass.Item> echo = MutinyStreamsGrpc.newMutinyStub(channel).echo(request);
        echo.subscribe().with(s -> s.request(Long.MAX_VALUE), item -> output.add(item.getName()), error -> {
            error.printStackTrace();
            result.completeExceptionally(error);
        }, () -> result.complete(true));
        return result;
    }

    private void callHello(String name, String responseMatcher) {
        HelloReply reply = GreeterGrpc.newBlockingStub(channel)
                .sayHello(HelloRequest.newBuilder().setName(name).build());
        assertThat(reply.getMessage()).matches(responseMatcher);
    }
}
