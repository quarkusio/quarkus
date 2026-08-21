package io.quarkus.arc.test.invoker

import jakarta.enterprise.invoke.AsyncHandler
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED

class KotlinCoroutineAsyncHandler<T> : AsyncHandler.ParameterType<Continuation<T>> {
    override fun transformArgument(original: Continuation<T>, completion: Runnable): Continuation<T> {
        return object : Continuation<T> {
            override fun resumeWith(result: Result<T>) {
                completion.run()
                original.resumeWith(result)
            }

            override val context: CoroutineContext
                get() = original.context
        }
    }

    override fun transformReturnValue(original: Any?, completion: Runnable): Any? {
        if (original !== COROUTINE_SUSPENDED) {
            completion.run()
        }
        return original
    }
}
