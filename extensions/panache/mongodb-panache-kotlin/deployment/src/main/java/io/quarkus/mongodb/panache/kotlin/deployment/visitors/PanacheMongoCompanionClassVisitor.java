package io.quarkus.mongodb.panache.kotlin.deployment.visitors;

import static io.quarkus.gizmo.Gizmo.ASM_API_VERSION;
import static org.jboss.jandex.DotName.createSimple;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import io.quarkus.deployment.util.AsmUtil;
import io.quarkus.mongodb.panache.deployment.ByteCodeType;
import io.quarkus.mongodb.panache.deployment.TypeBundle;
import io.quarkus.mongodb.panache.kotlin.deployment.KotlinGenerator;
import io.quarkus.panache.common.deployment.PanacheEntityEnhancer;

public class PanacheMongoCompanionClassVisitor extends ClassVisitor {
    private final Map<String, MethodInfo> bridgeMethods = new TreeMap<>();
    private final IndexView indexView;
    private String entityBinaryType;
    private String entitySignature;
    private KotlinGenerator generator;
    private TypeBundle types;

    public PanacheMongoCompanionClassVisitor(ClassVisitor outputClassVisitor, ClassInfo entityInfo, IndexView indexView,
            TypeBundle types) {
        super(ASM_API_VERSION, outputClassVisitor);
        this.indexView = indexView;
        this.types = types;

        entityInfo
                .methods()
                .forEach(method -> {
                    if (method.hasAnnotation(PanacheEntityEnhancer.DOTNAME_GENERATE_BRIDGE)) {
                        bridgeMethods.put(method.name() + AsmUtil.getDescriptor(method, m -> null), method);
                    }
                });
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", PanacheMongoCompanionClassVisitor.class.getSimpleName() + "[", "]")
                .add(entityBinaryType)
                .toString();
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);

        String className = name.replace('.', '/');

        ByteCodeType[] foundTypeArguments = KotlinGenerator.recursivelyFindEntityTypeArguments(indexView,
                createSimple(name.replace('/', '.')), types.entityCompanionBase().dotName());

        entityBinaryType = className.replace("$Companion", "");
        entitySignature = "L" + entityBinaryType + ";";

        Map<String, String> typeArguments = new HashMap<>();
        typeArguments.put("Entity", entitySignature);
        typeArguments.put("Id", foundTypeArguments[1].descriptor());

        Map<String, ByteCodeType> typeParameters = new HashMap<>();
        typeParameters.put("Entity", foundTypeArguments[0]);
        typeParameters.put("Id", foundTypeArguments[1]);

        generator = new KotlinGenerator(this.cv, typeParameters, typeArguments, types);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodInfo methodInfo = bridgeMethods.get(name + descriptor);
        if (methodInfo == null) {
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        } else {
            generator.add(methodInfo);
        }
        return null;
    }

    @Override
    public void visitEnd() {
        generator.generate();
        if (cv != null) {
            cv.visitEnd();
        }
    }
}
