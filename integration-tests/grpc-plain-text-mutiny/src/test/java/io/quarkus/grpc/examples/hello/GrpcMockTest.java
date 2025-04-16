package io.quarkus.grpc.examples.hello;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import examples.Greeter;
import examples.HelloReply;
import examples.HelloRequest;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;

@QuarkusTest
public class GrpcMockTest {

    @InjectMock
    @GrpcClient("hello")
    Greeter greeter;

    @Inject
    BeanCallingservice beanCallingService;

    @Test
    void test1() {
        HelloRequest request = HelloRequest.newBuilder().setName("clement").build();
        Mockito.when(greeter.sayHello(Mockito.any(HelloRequest.class)))
                .thenReturn(Uni.createFrom().item(HelloReply.newBuilder().setMessage("hello clement").build()));
        Assertions.assertEquals(greeter.sayHello(request).await().indefinitely().getMessage(), "hello clement");
    }

    @Test
    void test2() {
        HelloRequest request = HelloRequest.newBuilder().setName("roxanne").build();
        Mockito.when(greeter.sayHello(request))
                .thenReturn(Uni.createFrom().item(HelloReply.newBuilder().setMessage("hello roxanne").build()));
        Assertions.assertEquals(beanCallingService.call(), "hello roxanne");
    }

    @ApplicationScoped
    public static class BeanCallingservice {
        @InjectMock
        @GrpcClient("hello")
        Greeter greeter;

        public String call() {
            return greeter.sayHello(HelloRequest.newBuilder().setName("roxanne").build())
                    .map(HelloReply::getMessage)
                    .await().indefinitely();
        }
    }
}
