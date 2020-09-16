package io.quarkus.mongodb.panache.kotlin.deployment.visitors;

import static io.quarkus.deployment.util.AsmUtil.getDescriptor;
import static io.quarkus.mongodb.panache.kotlin.deployment.KotlinGenerator.findEntityTypeArguments;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import io.quarkus.mongodb.panache.deployment.ByteCodeType;
import io.quarkus.mongodb.panache.deployment.TypeBundle;
import io.quarkus.mongodb.panache.kotlin.deployment.KotlinGenerator;
import io.quarkus.panache.common.deployment.PanacheEntityEnhancer;
import io.quarkus.panache.common.deployment.visitors.PanacheRepositoryClassVisitor;

public class KotlinPanacheMongoRepositoryClassVisitor extends PanacheRepositoryClassVisitor {
    private final TypeBundle types;
    private KotlinGenerator generator;
    final Map<String, MethodInfo> toGenerate = new TreeMap<>();
    final Map<String, MethodInfo> toElide = new TreeMap<>();

    public KotlinPanacheMongoRepositoryClassVisitor(IndexView indexView, ClassVisitor outputClassVisitor, String className,
            TypeBundle types) {
        super(className, outputClassVisitor, indexView);
        this.types = types;
    }

    @Override
    protected final DotName getPanacheRepositoryDotName() {
        return types.repository().dotName();
    }

    @Override
    protected final DotName getPanacheRepositoryBaseDotName() {
        return types.repositoryBase().dotName();
    }

    @Override
    protected final String getPanacheOperationsInternalName() {
        return types.operations().internalName();
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        final String repositoryClassName = name.replace('/', '.');

        ByteCodeType[] foundTypeArguments = findEntityTypeArguments(indexView, repositoryClassName,
                getPanacheRepositoryBaseDotName());

        ByteCodeType idType = foundTypeArguments[1].unbox();
        typeArguments.put("Id", idType.descriptor());

        Map<String, ByteCodeType> typeParameters = new HashMap<>();
        typeParameters.put("Entity", foundTypeArguments[0]);
        typeParameters.put("Id", idType);

        daoClassInfo
                .methods()
                .forEach(method -> {
                    String descriptor = getDescriptor(method, m -> null);
                    if (method.hasAnnotation(PanacheEntityEnhancer.DOTNAME_GENERATE_BRIDGE)) {
                        toGenerate.put(method.name() + descriptor, method);
                        toElide.put(method.name() + descriptor, method);
                    }
                });
        generator = new KotlinGenerator(this.cv, typeParameters, typeArguments, types);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
            String[] exceptions) {
        MethodInfo methodInfo = toGenerate.get(name + descriptor);
        if (methodInfo == null) {
            if (toElide.get(name + descriptor) == null) {
                return super.visitMethod(access, name, descriptor, signature, exceptions);
            }
        } else {
            if (Modifier.isAbstract(daoClassInfo.flags())
                    && methodInfo.hasAnnotation(PanacheEntityEnhancer.DOTNAME_GENERATE_BRIDGE)) {
                userMethods.add(name + "/" + descriptor);
            }
        }

        return null;
    }

    @Override
    protected void generateModelBridge(MethodInfo method, AnnotationValue targetReturnTypeErased) {
        generator.generate(method);
    }

    @Override
    protected void generateJvmBridge(MethodInfo method) {
        if (!Modifier.isAbstract(daoClassInfo.flags())) {
            super.generateJvmBridge(method);
        }
    }
}
