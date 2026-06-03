package io.quarkus.grpc.examples.hello

import examples.GreeterGrpc
import examples.HelloReply
import examples.HelloRequest
import io.grpc.ServerServiceDefinition
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.kotlin.AbstractCoroutineServerImpl
import io.grpc.kotlin.ServerCalls.unaryServerMethodDefinition

abstract class GreeterCoroutineImplBase : AbstractCoroutineServerImpl() {

    open suspend fun sayHello(request: HelloRequest): HelloReply {
        throw StatusException(
            Status.UNIMPLEMENTED.withDescription(
                "Method helloworld.Greeter.SayHello is unimplemented"
            )
        )
    }

    override fun bindService(): ServerServiceDefinition =
        ServerServiceDefinition.builder(GreeterGrpc.getServiceDescriptor())
            .addMethod(
                unaryServerMethodDefinition(
                    context = this.context,
                    descriptor = GreeterGrpc.getSayHelloMethod(),
                    implementation = ::sayHello,
                )
            )
            .build()
}
