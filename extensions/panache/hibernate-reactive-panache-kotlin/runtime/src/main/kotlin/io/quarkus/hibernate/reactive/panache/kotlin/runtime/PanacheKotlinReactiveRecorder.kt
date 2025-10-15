package io.quarkus.hibernate.reactive.panache.kotlin.runtime

import io.quarkus.hibernate.reactive.panache.common.runtime.AbstractJpaOperations
import io.quarkus.runtime.annotations.Recorder

@Recorder
open class PanacheKotlinReactiveRecorder {
    open fun setEntityToPersistenceUnit(
        entityToPersistenceUnit: Map<String?, String?>?,
        incomplete: Boolean,
    ) {
        AbstractJpaOperations.setEntityToPersistenceUnit(entityToPersistenceUnit)
    }
}
