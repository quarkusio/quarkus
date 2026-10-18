package io.quarkus.it.jpa.generics;

import jakarta.persistence.Basic;
import jakarta.persistence.Lob;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class AbstractGenericMappedSuperType<T> {

    @Basic
    @Lob
    private T whateverType;

}
