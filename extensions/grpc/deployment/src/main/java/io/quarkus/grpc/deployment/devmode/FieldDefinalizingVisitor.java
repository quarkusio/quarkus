package io.quarkus.grpc.deployment.devmode;

import static java.util.Arrays.asList;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.gizmo.Gizmo;

public class FieldDefinalizingVisitor implements BiFunction<String, ClassVisitor, ClassVisitor> {

    private final Set<String> fields = new HashSet<>();

    public FieldDefinalizingVisitor(String... fields) {
        this.fields.addAll(asList(fields));
    }

    @Override
    public ClassVisitor apply(String s, ClassVisitor classVisitor) {
        return new ClassVisitor(Gizmo.ASM_API_VERSION, classVisitor) {
            @Override
            public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                if (fields.contains(name)) {
                    access = access & (~Opcodes.ACC_FINAL);
                    access = access | Opcodes.ACC_VOLATILE;
                }
                return super.visitField(access, name, descriptor, signature, value);
            }
        };
    }
}
