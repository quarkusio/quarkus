package io.quarkus.grpc.examples.hello;

import examples.GreeterGrpc;
import examples.MutinyGreeterGrpc;
import io.quarkus.grpc.GrpcClient;

//@Path("/hello")
public class HelloWorldNewEndpoint {

    @GrpcClient("hello")
    GreeterGrpc.GreeterBlockingStub blockingHelloService;

    @GrpcClient("hello")
    MutinyGreeterGrpc.MutinyGreeterStub mutinyHelloService;

    /*
     * @GET
     *
     * @Path("/blocking/{name}")
     * public String helloBlocking(@PathParam("name") String name) {
     * return blockingHelloService.sayHello(HelloRequest.newBuilder().setName(name).build()).getMessage();
     * }
     *
     * @GET
     *
     * @Path("/mutiny/{name}")
     * public Uni<String> helloMutiny(@PathParam("name") String name) {
     * return mutinyHelloService.sayHello(HelloRequest.newBuilder().setName(name).build())
     * .onItem().transform(HelloReply::getMessage);
     * }
     */
}
