/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package io.quarkus.data.processor;

import org.hibernate.processor.HibernateProcessor;
import org.hibernate.processor.model.MetaAttribute;
import org.hibernate.processor.model.Metamodel;

class CDITypeMetaAttribute implements MetaAttribute {

    private final Metamodel metamodel;
    private final String typeName;
    private final Object superTypeName;

    public CDITypeMetaAttribute(Metamodel metamodel, String className, String superTypeName) {
        this.metamodel = metamodel;
        this.superTypeName = superTypeName;
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
        closingBrace(declaration);
        return declaration.toString();
    }

    void closingBrace(StringBuilder declaration) {
        declaration.append("}");
    }

    void preamble(StringBuilder declaration) {
        declaration
                .append("class ")
                .append(typeName)
                .append(" implements ")
                .append(superTypeName)
                .append(" {\n");
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
        return "";
    }

    @Override
    public String getTypeDeclaration() {
        return "";
    }

    void modifiers(StringBuilder declaration) {
        metamodel.importType("jakarta.annotation.Generated");
        metamodel.importType("jakarta.enterprise.context.Dependent");
        declaration
                .append("\n@Dependent\n")
                .append("@Generated(\"")
                .append(HibernateProcessor.class.getName())
                .append("\")\n")
                .append("public static ");
    }

    @Override
    public Metamodel getHostingEntity() {
        return metamodel;
    }

}
