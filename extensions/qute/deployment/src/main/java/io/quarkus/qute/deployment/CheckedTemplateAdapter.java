package io.quarkus.qute.deployment;

import org.objectweb.asm.MethodVisitor;

public interface CheckedTemplateAdapter {

    String templateInstanceBinaryName();

    void convertTemplateInstance(MethodVisitor nativeMethodVisitor);

}
