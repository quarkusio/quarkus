/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package io.quarkus.data.processor;

import org.hibernate.processor.model.MetaAttribute;
import org.hibernate.processor.model.Metamodel;

class CDIAccessorMetaAttribute implements MetaAttribute {

    private Metamodel metamodel;
    private String propertyName;
    private String typeName;

    public CDIAccessorMetaAttribute(Metamodel metamodel, String propertyName, String className) {
        this.metamodel = metamodel;
        this.propertyName = propertyName;
        this.typeName = className;
    }

    @Override
    public boolean hasTypedAttribute() {
        return true;
    }

    @Override
    public boolean hasStringAttribute() {
        return false;
    }

    @Override
    public String getAttributeDeclarationString() {
        final var declaration = new StringBuilder();
        modifiers(declaration);
        preamble(declaration);
        returnCDI(declaration);
        closingBrace(declaration);
        return declaration.toString();
    }

    private void returnCDI(StringBuilder declaration) {
        metamodel.importType("jakarta.enterprise.inject.spi.CDI");
        declaration
                .append("\treturn CDI.current().select(")
                .append(metamodel.importType(typeName))
                .append(".class).get();\n");
    }

    void closingBrace(StringBuilder declaration) {
        declaration.append("}");
    }

    void preamble(StringBuilder declaration) {
        declaration
                .append(metamodel.importType(typeName))
                .append(" ")
                .append(getPropertyName());
        declaration
                .append("() {\n");
    }

    @Override
    public String getAttributeNameDeclarationString() {
        return "";
    }

    @Override
    public String getMetaType() {
        throw new UnsupportedOperationException("operation not supported");
    }

    @Override
    public String getPropertyName() {
        return propertyName;
    }

    @Override
    public String getTypeDeclaration() {
        return "";
    }

    void modifiers(StringBuilder declaration) {
        declaration
                .append("\npublic static ");
    }

    @Override
    public Metamodel getHostingEntity() {
        return metamodel;
    }

}
