package org.jboss.shamrock.example.classtransformer;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.shamrock.deployment.ArchiveContext;
import org.jboss.shamrock.deployment.ProcessorContext;
import org.jboss.shamrock.deployment.ResourceProcessor;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;


/**
 * class that adds an additional @GET @Path("/transformed") method to every JAX-RS endpoint.
 * <p>
 * This is intended as a test of the class transformation functionality, it should probably be removed
 * when we have better test frameworks
 */
public class ClassTransformerProcessor implements ResourceProcessor {

    private static final DotName PATH = DotName.createSimple("javax.ws.rs.Path");

    @Override
    public void process(ArchiveContext archiveContext, ProcessorContext processorContext) throws Exception {
        final Set<String> pathAnnotatedClasses = new HashSet<>();

        Collection<AnnotationInstance> annotations = archiveContext.getCombinedIndex().getAnnotations(PATH);
        for (AnnotationInstance a : annotations) {
            if (a.target().kind() == AnnotationTarget.Kind.CLASS) {
                pathAnnotatedClasses.add(a.target().asClass().toString());
            }
        }
        if (!pathAnnotatedClasses.isEmpty()) {
            for (String i : pathAnnotatedClasses) {
                processorContext.addByteCodeTransformer(i, new BiFunction<String, ClassVisitor, ClassVisitor>() {
                    @Override
                    public ClassVisitor apply(String className, ClassVisitor classVisitor) {
                        ClassVisitor cv = new ClassVisitor(Opcodes.ASM6, classVisitor) {

                            @Override
                            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                                super.visit(version, access, name, signature, superName, interfaces);
                                MethodVisitor mv = visitMethod(Modifier.PUBLIC, "transformed", "()Ljava/lang/String;", null, null);

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
                });
            }
        }
    }

    @Override
    public int getPriority() {
        return 0;
    }
}
