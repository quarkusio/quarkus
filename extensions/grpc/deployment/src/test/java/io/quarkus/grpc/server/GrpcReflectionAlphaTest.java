package io.quarkus.grpc.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Flow;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.google.protobuf.ByteString;

import grpc.health.v1.HealthGrpc;
import grpc.reflection.v1alpha.MutinyServerReflectionGrpc;
import grpc.reflection.v1alpha.Reflection;
import io.grpc.Status;
import io.grpc.reflection.testing.MutinyReflectableServiceGrpc;
import io.grpc.reflection.testing.ReflectionTestDepthThreeProto;
import io.grpc.reflection.testing.ReflectionTestDepthTwoProto;
import io.grpc.reflection.testing.ReflectionTestProto;
import io.grpc.reflection.testing.Reply;
import io.grpc.reflection.testing.Request;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.GrpcService;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.multi.processors.UnicastProcessor;

/**
 * Check the behavior of the reflection service (v1alpha)
 */
public class GrpcReflectionAlphaTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addPackage(HealthGrpc.class.getPackage())
                    .addPackage(MutinyReflectableServiceGrpc.class.getPackage()).addClass(MyReflectionService.class))
            .setFlatClassPath(true).withConfigurationResource("reflection-config.properties");

    @GrpcClient("reflection-service")
    MutinyServerReflectionGrpc.MutinyServerReflectionStub reflection;

    private UnicastProcessor<Reflection.ServerReflectionRequest> processor;
    private ResettableSubscriber<Reflection.ServerReflectionResponse> subscriber;

    @BeforeEach
    public void setUp() {
        processor = UnicastProcessor.create();
        subscriber = new ResettableSubscriber<>();
    }

    @AfterEach
    public void cleanUp() {
        processor.onComplete();
        subscriber.cancel();
    }

    @Test
    public void testRetrievingListOfServices() {
        Reflection.ServerReflectionRequest request = Reflection.ServerReflectionRequest.newBuilder()
                .setHost("localhost").setListServices("").build();

        Reflection.ServerReflectionResponse response = invoke(request);
        List<Reflection.ServiceResponse> list = response.getListServicesResponse().getServiceList();
        assertThat(list).hasSize(2)
                .anySatisfy(r -> assertThat(r.getName()).isEqualTo("grpc.reflection.testing.ReflectableService"))
                .anySatisfy(r -> assertThat(r.getName()).isEqualTo("grpc.health.v1.Health"));
    }

    @Test
    public void testRetrievingFilesByFileName() {
        Reflection.ServerReflectionRequest request = Reflection.ServerReflectionRequest.newBuilder()
                .setHost("localhost").setFileByFilename("reflection/reflection_test_depth_three.proto").build();

        Reflection.ServerReflectionResponse expected = Reflection.ServerReflectionResponse.newBuilder()
                .setValidHost("localhost").setOriginalRequest(request)
                .setFileDescriptorResponse(Reflection.FileDescriptorResponse.newBuilder()
                        .addFileDescriptorProto(ReflectionTestDepthThreeProto.getDescriptor().toProto().toByteString())
                        .build())
                .build();

        Reflection.ServerReflectionResponse response = invoke(request);
        assertThat(response).isEqualTo(expected);
    }

    @Test
    public void testRetrievingFilesByFileNameWithUnknownFileName() {
        Reflection.ServerReflectionRequest request = Reflection.ServerReflectionRequest.newBuilder()
                .setHost("localhost").setFileByFilename("reflection/unknown.proto").build();

        Reflection.ServerReflectionResponse response = invoke(request);
        assertThat(response.getErrorResponse().getErrorCode()).isEqualTo(Status.Code.NOT_FOUND.value());
    }

    private Reflection.ServerReflectionResponse invoke(Reflection.ServerReflectionRequest request) {
        subscriber.reset();
        Multi<Reflection.ServerReflectionResponse> multi = reflection.serverReflectionInfo(processor);
        multi.subscribe().withSubscriber(subscriber);
        subscriber.awaitForSubscription();
        processor.onNext(request);
        return subscriber.awaitAndGetLast();
    }

    @Test
    public void testRetrievingFilesContainingSymbol() {
        Reflection.ServerReflectionRequest request = Reflection.ServerReflectionRequest.newBuilder()
                .setHost("localhost").setFileContainingSymbol("grpc.reflection.testing.ReflectableService.Method")
                .build();

        List<ByteString> responses = Arrays.asList(ReflectionTestProto.getDescriptor().toProto().toByteString(),
                ReflectionTestDepthTwoProto.getDescriptor().toProto().toByteString(),
                ReflectionTestDepthThreeProto.getDescriptor().toProto().toByteString());

        Reflection.ServerReflectionResponse response = invoke(request);
        List<ByteString> list = response.getFileDescriptorResponse().getFileDescriptorProtoList();
        assertThat(list).containsExactlyInAnyOrderElementsOf(responses);
    }

    @Test
    public void testRetrievingFilesContainingUnknownSymbol() {
        Reflection.ServerReflectionRequest request = Reflection.ServerReflectionRequest.newBuilder()
                .setHost("localhost")
                .setFileContainingSymbol("grpc.reflection.testing.ReflectableService.UnknownMethod").build();

        Reflection.ServerReflectionResponse response = invoke(request);
        List<ByteString> list = response.getFileDescriptorResponse().getFileDescriptorProtoList();
        assertThat(list).isEmpty();
        assertThat(response.getErrorResponse().getErrorMessage()).contains("UnknownMethod");
        assertThat(response.getErrorResponse().getErrorCode()).isEqualTo(Status.Code.NOT_FOUND.value());
    }

    @Test
    public void testRetrievingFilesContainingNestedSymbol() {
        Reflection.ServerReflectionRequest request = Reflection.ServerReflectionRequest.newBuilder()
                .setHost("localhost").setFileContainingSymbol("grpc.reflection.testing.NestedTypeOuter.Middle.Inner")
                .build();
        Reflection.ServerReflectionResponse expected = Reflection.ServerReflectionResponse.newBuilder()
                .setValidHost("localhost").setOriginalRequest(request)
                .setFileDescriptorResponse(Reflection.FileDescriptorResponse.newBuilder()
                        .addFileDescriptorProto(ReflectionTestDepthThreeProto.getDescriptor().toProto().toByteString())
                        .build())
                .build();
        Reflection.ServerReflectionResponse resp = invoke(request);
        assertThat(resp).isEqualTo(expected);
    }

    @Test
    public void testRetrievingFilesContainingExtension() {
        Reflection.ServerReflectionRequest request = Reflection.ServerReflectionRequest.newBuilder()
                .setHost("localhost")
                .setFileContainingExtension(Reflection.ExtensionRequest.newBuilder()
                        .setContainingType("grpc.reflection.testing.ThirdLevelType").setExtensionNumber(100).build())
                .build();

        List<ByteString> expected = Arrays.asList(ReflectionTestProto.getDescriptor().toProto().toByteString(),
                ReflectionTestDepthTwoProto.getDescriptor().toProto().toByteString(),
                ReflectionTestDepthThreeProto.getDescriptor().toProto().toByteString());

        Reflection.ServerReflectionResponse response = invoke(request);
        assertThat(response.getFileDescriptorResponse().getFileDescriptorProtoList())
                .containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    public void testRetrievingFilesContainingNestedExtension() {
        Reflection.ServerReflectionRequest request = Reflection.ServerReflectionRequest.newBuilder()
                .setHost("localhost")
                .setFileContainingExtension(Reflection.ExtensionRequest.newBuilder()
                        .setContainingType("grpc.reflection.testing.ThirdLevelType").setExtensionNumber(101).build())
                .build();

        Reflection.ServerReflectionResponse expected = Reflection.ServerReflectionResponse.newBuilder()
                .setValidHost("localhost").setOriginalRequest(request)
                .setFileDescriptorResponse(Reflection.FileDescriptorResponse.newBuilder()
                        .addFileDescriptorProto(ReflectionTestDepthTwoProto.getDescriptor().toProto().toByteString())
                        .addFileDescriptorProto(ReflectionTestDepthThreeProto.getDescriptor().toProto().toByteString())
                        .build())
                .build();

        Reflection.ServerReflectionResponse response = invoke(request);
        assertThat(response).isEqualTo(expected);
    }

    @Test
    public void testRetrievingAllExtensionNumbersOfType() {
        Reflection.ServerReflectionRequest request = Reflection.ServerReflectionRequest.newBuilder()
                .setHost("localhost").setAllExtensionNumbersOfType("grpc.reflection.testing.ThirdLevelType").build();

        List<Integer> expected = Arrays.asList(100, 101);

        Reflection.ServerReflectionResponse response = invoke(request);
        List<Integer> list = response.getAllExtensionNumbersResponse().getExtensionNumberList();
        assertThat(list).containsExactlyInAnyOrderElementsOf(expected);
    }

    private static class ResettableSubscriber<T> implements Flow.Subscriber<T> {

        private Flow.Subscription subscription;
        private volatile T last;
        private boolean completed;
        private Throwable failure;

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
        }

        public void reset() {
            this.subscription = null;
        }

        public void awaitForSubscription() {
            await().until(() -> subscription != null);
        }

        public T awaitAndGetLast() {
            validate();
            last = null;
            subscription.request(1);
            await().until(() -> last != null);
            return last;
        }

        @Override
        public void onNext(T t) {
            last = t;
        }

        @Override
        public void onError(Throwable throwable) {
            this.failure = throwable;
        }

        @Override
        public void onComplete() {
            this.completed = true;
        }

        private void validate() {
            if (this.failure != null || this.completed) {
                throw new IllegalStateException("Subscriber already in a terminal state");
            }
        }

        public void cancel() {
            this.subscription.cancel();
        }
    }

    @GrpcService
    public static class MyReflectionService extends MutinyReflectableServiceGrpc.ReflectableServiceImplBase {
        @Override
        public Uni<Reply> method(Request request) {
            String message = request.getMessage();
            return Uni.createFrom().item(Reply.newBuilder().setMessage(message).build());
        }
    }

}
