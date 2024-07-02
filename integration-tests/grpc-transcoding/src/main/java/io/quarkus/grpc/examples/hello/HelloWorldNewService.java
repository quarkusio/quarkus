package io.quarkus.grpc.examples.hello;

import examples.*;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;

@GrpcService
public class HelloWorldNewService implements Greeter {

    @Override
    public Uni<HelloReply> simplePath(HelloRequest request) {
        String greeting = "Hello from Simple Path, " + request.getName() + "!";
        return Uni.createFrom().item(HelloReply.newBuilder().setMessage(greeting).build());
    }

    @Override
    public Uni<HelloReply> complexPath(HelloRequest request) {
        String greeting = "Hello from Complex Path, " + request.getName() + "!";
        return Uni.createFrom().item(HelloReply.newBuilder().setMessage(greeting).build());
    }

    @Override
    public Uni<HelloReply> resourceLookup(ResourceRequest request) {
        String greeting = "Resource details: type='" + request.getResourceType() +
                "', id='" + request.getResourceId() + "'";
        return Uni.createFrom().item(HelloReply.newBuilder().setMessage(greeting).build());
    }

    @Override
    public Uni<HelloReply> nestedResourceLookup(UpdateRequest request) {
        String greeting = "Greeting with id '" + request.getGreetingId() + "' " +
                "updated with nested resource details: name='" + request.getUpdatedContent().getName() + "'";

        return Uni.createFrom().item(HelloReply.newBuilder().setMessage(greeting).build());
    }

    @Override
    public Uni<HelloReply> searchGreetings(SearchRequest request) {
        String greeting = "Matching greetings for your query: '" + request.getQuery() + "'";
        return Uni.createFrom().item(HelloReply.newBuilder().setMessage(greeting).build());
    }

    @Override
    public Uni<HelloReply> updateGreeting(UpdateRequest request) {
        String greeting = "Greeting with id '" + request.getGreetingId() + "' updated!";
        return Uni.createFrom().item(HelloReply.newBuilder().setMessage(greeting).build());
    }
}
