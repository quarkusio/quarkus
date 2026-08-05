package io.quarkus.it.panache.reactive;

import io.quarkus.hibernate.reactive.panache.common.ProjectedFieldName;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record CatOwnerWeightDto(
        @ProjectedFieldName("owner.name") String ownerName,
        @ProjectedFieldName("SUM(weight)") Double totalWeight) {
}
