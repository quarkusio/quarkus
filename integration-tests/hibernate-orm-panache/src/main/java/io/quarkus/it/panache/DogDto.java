package io.quarkus.it.panache;

import io.quarkus.hibernate.orm.panache.common.ProjectedFieldName;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class DogDto {

    public String name;

    public String ownerName;

    public DogDto(String name, @ProjectedFieldName("owner.name") String ownerName) {
        this.name = name;
        this.ownerName = ownerName;
    }

}
