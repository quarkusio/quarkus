package io.quarkus.it.jpa.h2.generics;

import javax.persistence.Basic;
import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class AbstractGenericMappedSuperType<T> {

    @Basic
    @Lob
    private T whateverType;

}
