package io.quarkus.it.classtransformer;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.gizmo.Gizmo;

/**
 * class that adds an additional @GET @Path("/transformed") method to every JAX-RS endpoint.
 * <p>
 * This is intended as a test of the class transformation functionality, it should probably be removed
 * when we have better test frameworks
 */
public class ClassTransformerProcessor {

    private static final DotName PATH = DotName.createSimple("javax.ws.rs.Path");

    @BuildStep
    public void build(CombinedIndexBuildItem combinedIndex,
            BuildProducer<BytecodeTransformerBuildItem> transformers) throws Exception {
        final Set<String> pathAnnotatedClasses = new HashSet<>();

        Collection<AnnotationInstance> annotations = combinedIndex.getIndex().getAnnotations(PATH);
        for (AnnotationInstance a : annotations) {
            if (a.target().kind() == AnnotationTarget.Kind.CLASS) {
                pathAnnotatedClasses.add(a.target().asClass().toString());
            }
        }
        if (!pathAnnotatedClasses.isEmpty()) {
            for (String i : pathAnnotatedClasses) {
                transformers.produce(new BytecodeTransformerBuildItem(i, new BiFunction<String, ClassVisitor, ClassVisitor>() {
                    @Override
                    public ClassVisitor apply(String className, ClassVisitor classVisitor) {
                        ClassVisitor cv = new ClassVisitor(Gizmo.ASM_API_VERSION, classVisitor) {

                            @Override
                            public void visit(int version, int access, String name, String signature, String superName,
                                    String[] interfaces) {
                                super.visit(version, access, name, signature, superName, interfaces);
                                MethodVisitor mv = visitMethod(Modifier.PUBLIC, "transformed", "()Ljava/lang/String;", null,
                                        null);

                                AnnotationVisitor annotation = mv.visitAnnotation("Ljavax/ws/rs/Path;", true);
                                annotation.visit("value", "/transformed");
                                annotation.visitEnd();
                                annotation = mv.visitAnnotation("Ljavax/ws/rs/GET;", true);
                                annotation.visitEnd();

                                mv.visitLdcInsn("Transformed Endpoint");
                                mv.visitInsn(Opcodes.ARETURN);
                                mv.visitMaxs(1, 1);
                                mv.visitEnd();
                            }
                        };
                        return cv;
                    }
                }));
            }
        }
    }
}
