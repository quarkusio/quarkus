package io.quarkus.it.panache.defaultpu;

import io.quarkus.hibernate.orm.panache.common.ProjectedFieldName;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record CatOwnerWeightDto(
        @ProjectedFieldName("owner.name") String ownerName,
        @ProjectedFieldName("SUM(weight)") Double totalWeight) {
}
