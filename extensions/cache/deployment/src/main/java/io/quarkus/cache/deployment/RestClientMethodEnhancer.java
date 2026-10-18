package io.quarkus.cache.deployment;

import java.util.function.BiFunction;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import io.quarkus.cache.runtime.CacheKeyParameterPositions;
import io.quarkus.gizmo.Gizmo;

/*
 * When a MicroProfile REST Client method annotated with a caching annotation is executed, an interception is performed by the
 * MP REST Client interceptors implementation, which is not Arc. This implementation is unaware of the
 * @CacheKeyParameterPositions annotation that we add at build time in CacheAnnotationsTransformer to avoid relying on
 * reflection at run time to identify the cache key elements. Reflection is bad for performances because of underlying
 * synchronized calls. This class does a similar job as CacheAnnotationsTransformer and since it relies on bytecode
 * transformation, the @CacheKeyParameterPositions annotation is available during the MP REST Client cache interception.
 */
class RestClientMethodEnhancer implements BiFunction<String, ClassVisitor, ClassVisitor> {

    private static final String CACHE_KEY_PARAMETER_POSITIONS_DESCRIPTOR = "L"
            + CacheKeyParameterPositions.class.getName().replace('.', '/') + ";";

    private final String methodName;
    private final short[] cacheKeyParameterPositions;

    public RestClientMethodEnhancer(String methodName, short[] cacheKeyParameterPositions) {
        this.methodName = methodName;
        this.cacheKeyParameterPositions = cacheKeyParameterPositions;
    }

    @Override
    public ClassVisitor apply(String className, ClassVisitor classVisitor) {
        return new ClassVisitor(Gizmo.ASM_API_VERSION, classVisitor) {

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                    String[] exceptions) {

                MethodVisitor superVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!name.equals(methodName)) {
                    // This is not the method we want to enhance, let's skip the bytecode transformation.
                    return superVisitor;
                } else {
                    return new MethodVisitor(Gizmo.ASM_API_VERSION, superVisitor) {

                        @Override
                        public void visitEnd() {

                            /*
                             * If the method parameters at positions 0 and 2 are annotated with @CacheKey, the following code
                             * will add the `@CacheKeyParameterPositions(value={0, 2})` annotation to the method.
                             */
                            AnnotationVisitor annotation = super.visitAnnotation(CACHE_KEY_PARAMETER_POSITIONS_DESCRIPTOR,
                                    true);
                            annotation.visit("value", cacheKeyParameterPositions);
                            annotation.visitEnd();
                            super.visitEnd();
                        }
                    };
                }
            }
        };
    }
}
