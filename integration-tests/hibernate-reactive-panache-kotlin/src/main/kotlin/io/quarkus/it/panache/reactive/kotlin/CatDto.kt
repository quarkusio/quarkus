package io.quarkus.it.panache.reactive.kotlin

import io.quarkus.hibernate.reactive.panache.common.ProjectedFieldName
import io.quarkus.runtime.annotations.RegisterForReflection

@RegisterForReflection
class CatDto(var name: String, @param:ProjectedFieldName("owner.name") var ownerName: String)
