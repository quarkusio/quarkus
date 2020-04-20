package io.quarkus.panache.common.deployment;

import java.util.List;

import org.jboss.jandex.MethodInfo;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import io.quarkus.gizmo.Gizmo;

public class PanacheMethodCustomizerVisitor extends MethodVisitor {

    private List<PanacheMethodCustomizer> methodCustomizers;
    private MethodInfo method;
    private Type thisClass;

    public PanacheMethodCustomizerVisitor(MethodVisitor superVisitor, MethodInfo method, Type thisClass,
            List<PanacheMethodCustomizer> methodCustomizers) {
        super(Gizmo.ASM_API_VERSION, superVisitor);
        this.thisClass = thisClass;
        this.method = method;
        this.methodCustomizers = methodCustomizers;
    }

    @Override
    public void visitCode() {
        super.visitCode();
        for (PanacheMethodCustomizer customizer : methodCustomizers) {
            customizer.customize(thisClass, method, mv);
        }
    }
}
