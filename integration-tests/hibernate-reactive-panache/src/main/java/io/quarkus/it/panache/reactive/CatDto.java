package io.quarkus.it.panache.reactive;

import io.quarkus.hibernate.reactive.panache.common.ProjectedFieldName;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class CatDto {

    public String name;

    public String ownerName;

    public CatDto(String name, @ProjectedFieldName("owner.name") String ownerName) {
        this.name = name;
        this.ownerName = ownerName;
    }

}
