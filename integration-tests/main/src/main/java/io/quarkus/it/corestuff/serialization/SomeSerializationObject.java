package io.quarkus.it.corestuff.serialization;

import java.io.Serializable;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection(serialization = true)
public class SomeSerializationObject implements Serializable {

    private Person person;
    private ExternalizablePerson externalizablePerson;

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public ExternalizablePerson getExternalizablePerson() {
        return externalizablePerson;
    }

    public void setExternalizablePerson(ExternalizablePerson externalizablePerson) {
        this.externalizablePerson = externalizablePerson;
    }
}
