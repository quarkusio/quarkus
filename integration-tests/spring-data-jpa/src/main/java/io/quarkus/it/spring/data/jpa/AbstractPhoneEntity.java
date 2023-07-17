package io.quarkus.it.spring.data.jpa;

import jakarta.persistence.MappedSuperclass;

// demonstrates that @MappedSuperclass detection works for more than one level and for parameterized types
@MappedSuperclass
public abstract class AbstractPhoneEntity<ID extends PhoneNumberId> extends AbstractTypedIdEntity<ID> {

    protected AbstractPhoneEntity(ID id) {
        super(id);
    }
}
