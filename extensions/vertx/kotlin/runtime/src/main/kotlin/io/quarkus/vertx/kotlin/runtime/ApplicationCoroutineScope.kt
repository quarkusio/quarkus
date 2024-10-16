package io.quarkus.vertx.kotlin.runtime

import jakarta.annotation.PreDestroy
import jakarta.inject.Singleton
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

@Singleton
class ApplicationCoroutineScope : CoroutineScope, AutoCloseable {
    override val coroutineContext: CoroutineContext = SupervisorJob()

    @PreDestroy
    override fun close() {
        coroutineContext.cancel()
    }
}
