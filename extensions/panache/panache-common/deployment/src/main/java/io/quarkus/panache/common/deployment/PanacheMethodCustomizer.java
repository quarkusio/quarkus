package io.quarkus.panache.common.deployment;

import org.jboss.jandex.MethodInfo;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public interface PanacheMethodCustomizer {

    public void customize(Type entityClassSignature, MethodInfo method, MethodVisitor mv);

}
