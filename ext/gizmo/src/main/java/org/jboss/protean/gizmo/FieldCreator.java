package org.jboss.protean.gizmo;


public interface FieldCreator extends MemberCreator<FieldCreator>,AnnotatedElement {

    FieldDescriptor getFieldDescriptor();

}
