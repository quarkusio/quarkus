package io.quarkus.hibernate.orm.runtime.boot.xml;

import javax.xml.namespace.QName;

import jakarta.xml.bind.JAXBElement;

import io.quarkus.runtime.ObjectSubstitution;
import io.quarkus.runtime.annotations.RecordableConstructor;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class JAXBElementSubstitution implements ObjectSubstitution<JAXBElement, JAXBElementSubstitution.Serialized> {

    @Override
    public Serialized serialize(JAXBElement obj) {
        return new Serialized(obj.getName(), obj.getDeclaredType(), obj.getScope(), obj.getValue());
    }

    @Override
    public JAXBElement deserialize(Serialized obj) {
        return new JAXBElement(obj.name, obj.declaredType, obj.scope, obj.value);
    }

    public static class Serialized {
        public final QName name;
        public final Class<?> declaredType;
        public final Class<?> scope;
        public final Object value;

        @RecordableConstructor
        public Serialized(QName name, Class<?> declaredType, Class<?> scope, Object value) {
            this.name = name;
            this.declaredType = declaredType;
            this.scope = scope;
            this.value = value;
        }
    }

}
