package io.quarkus.it.panache.defaultpu;

import io.quarkus.hibernate.orm.panache.common.ProjectedFieldName;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Projection DTO that references two fields of the same {@code owner} association, used to verify that a
 * {@code JoinType.LEFT} projection generates a single shared join for both.
 */
@RegisterForReflection
public class CatOwnerRefDto {

    public Long ownerId;

    public String ownerName;

    public CatOwnerRefDto(@ProjectedFieldName("owner.id") Long ownerId, @ProjectedFieldName("owner.name") String ownerName) {
        this.ownerId = ownerId;
        this.ownerName = ownerName;
    }

}
