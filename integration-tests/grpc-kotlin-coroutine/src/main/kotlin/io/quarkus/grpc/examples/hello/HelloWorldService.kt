package io.quarkus.grpc.examples.hello

import examples.HelloReply
import examples.HelloRequest
import io.quarkus.grpc.GrpcService
import io.smallrye.common.vertx.VertxContext
import io.vertx.core.Vertx

@GrpcService
class HelloWorldService : GreeterCoroutineImplBase() {

    override suspend fun sayHello(request: HelloRequest): HelloReply {
        val ctx = Vertx.currentContext()
        val isDuplicated = ctx != null && VertxContext.isDuplicatedContext(ctx)

        return HelloReply.newBuilder()
            .setMessage("Hello ${request.name}")
            .setHasVertxContext(ctx != null)
            .setIsDuplicatedContext(isDuplicated)
            .build()
    }
}
