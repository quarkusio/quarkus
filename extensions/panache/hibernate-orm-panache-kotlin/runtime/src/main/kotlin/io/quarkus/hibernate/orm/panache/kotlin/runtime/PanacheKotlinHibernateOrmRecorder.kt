package io.quarkus.hibernate.orm.panache.kotlin.runtime

import io.quarkus.hibernate.orm.panache.common.runtime.AbstractJpaOperations
import io.quarkus.runtime.annotations.Recorder

@Recorder
open class PanacheKotlinHibernateOrmRecorder {
    open fun setEntityToPersistenceUnit(entityToPersistenceUnit: Map<String?, String?>?) {
        AbstractJpaOperations.setEntityToPersistenceUnit(entityToPersistenceUnit)
    }
}
