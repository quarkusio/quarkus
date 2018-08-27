package org.jboss.protean.gizmo;

import org.objectweb.asm.ClassWriter;

public interface MemberCreator<T extends MemberCreator<T>> {

    int getModifiers();

    T setModifiers(int mods);

    void write(ClassWriter file);

}
