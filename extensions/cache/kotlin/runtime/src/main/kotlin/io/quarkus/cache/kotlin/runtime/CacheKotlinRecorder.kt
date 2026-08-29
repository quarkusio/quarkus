package io.quarkus.cache.kotlin.runtime

import io.quarkus.cache.runtime.CacheSpecialMethodHandler
import io.quarkus.runtime.annotations.Recorder

@Recorder
open class CacheKotlinRecorder {

    open fun createSpecialMethodHandler(): CacheSpecialMethodHandler {
        return CacheKotlinSuspendMethodHandler()
    }
}
