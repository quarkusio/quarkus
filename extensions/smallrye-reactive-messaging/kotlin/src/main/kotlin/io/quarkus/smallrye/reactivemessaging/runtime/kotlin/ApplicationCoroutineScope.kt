package io.quarkus.smallrye.reactivemessaging.runtime.kotlin

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import javax.annotation.PreDestroy
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
class ApplicationCoroutineScope : CoroutineScope, AutoCloseable {
    override val coroutineContext: CoroutineContext = SupervisorJob()

    @PreDestroy
    override fun close() {
        coroutineContext.cancel()
    }
}
