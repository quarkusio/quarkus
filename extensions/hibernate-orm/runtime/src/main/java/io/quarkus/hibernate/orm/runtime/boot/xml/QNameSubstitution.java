package io.quarkus.hibernate.orm.runtime.boot.xml;

import javax.xml.namespace.QName;

import io.quarkus.runtime.ObjectSubstitution;
import io.quarkus.runtime.annotations.RecordableConstructor;

public class QNameSubstitution implements ObjectSubstitution<QName, QNameSubstitution.Serialized> {

    @Override
    public Serialized serialize(QName obj) {
        return new Serialized(obj.getNamespaceURI(), obj.getLocalPart(), obj.getPrefix());
    }

    @Override
    public QName deserialize(Serialized obj) {
        return new QName(obj.namespaceURI, obj.localPart, obj.prefix);
    }

    public static class Serialized {
        public final String namespaceURI;
        public final String localPart;
        public final String prefix;

        @RecordableConstructor
        public Serialized(String namespaceURI, String localPart, String prefix) {
            this.namespaceURI = namespaceURI;
            this.localPart = localPart;
            this.prefix = prefix;
        }
    }

}
