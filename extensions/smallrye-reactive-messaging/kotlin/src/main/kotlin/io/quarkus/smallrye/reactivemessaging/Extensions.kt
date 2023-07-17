package io.quarkus.smallrye.reactivemessaging

import kotlinx.coroutines.future.await
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.eclipse.microprofile.reactive.messaging.Message

suspend fun <T> Message<T>.ackSuspending() = ack().await()

suspend fun <T> Message<T>.nackSuspending(t: Throwable) = nack(t).await()

suspend fun <T> Emitter<T>.sendSuspending(t: T) = send(t).await()
