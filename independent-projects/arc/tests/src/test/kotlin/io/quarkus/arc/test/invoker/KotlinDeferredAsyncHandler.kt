package io.quarkus.arc.test.invoker

import jakarta.enterprise.invoke.AsyncHandler
import kotlinx.coroutines.Deferred

class KotlinDeferredAsyncHandler<T> : AsyncHandler.ReturnType<Deferred<T>> {
    override fun transform(original: Deferred<T>, completion: Runnable): Deferred<T> {
        original.invokeOnCompletion {
            completion.run()
        }
        return original
    }
}
