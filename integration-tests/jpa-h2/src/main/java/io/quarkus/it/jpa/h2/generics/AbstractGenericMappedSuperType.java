package io.quarkus.it.jpa.h2.generics;

import jakarta.persistence.Basic;
import jakarta.persistence.Lob;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class AbstractGenericMappedSuperType<T> {

    @Basic
    @Lob
    private T whateverType;

}
