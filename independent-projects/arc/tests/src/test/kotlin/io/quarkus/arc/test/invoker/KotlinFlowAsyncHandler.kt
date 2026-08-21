package io.quarkus.arc.test.invoker

import jakarta.enterprise.invoke.AsyncHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion

class KotlinFlowAsyncHandler<T> : AsyncHandler.ReturnType<Flow<T>> {
    override fun transform(original: Flow<T>, completion: Runnable): Flow<T> {
        return original.onCompletion {
            completion.run()
        }
    }
}
