package io.quarkus.smallrye.reactivemessaging

import kotlinx.coroutines.future.await
import org.eclipse.microprofile.reactive.messaging.Emitter

suspend fun <T> Emitter<T>.sendSuspending(t: T) = send(t).await()