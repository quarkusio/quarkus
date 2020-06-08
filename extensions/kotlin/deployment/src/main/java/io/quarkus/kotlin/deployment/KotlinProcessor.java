package io.quarkus.kotlin.deployment;

import io.quarkus.deployment.Feature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassFinalFieldsWritablePredicateBuildItem;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.jackson.spi.ClassPathJacksonModuleBuildItem;

public class KotlinProcessor {

    private static final String KOTLIN_JACKSON_MODULE = "com.fasterxml.jackson.module.kotlin.KotlinModule";
    public static final Logger logger = Logger.getLogger(KotlinProcessor.class.getName());

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.KOTLIN);
    }

    /*
     * Register the Kotlin Jackson module if that has been added to the classpath
     * Producing the BuildItem is entirely safe since if quarkus-jackson is not on the classpath
     * the BuildItem will just be ignored
     */
    @BuildStep
    void registerKotlinJacksonModule(BuildProducer<ClassPathJacksonModuleBuildItem> classPathJacksonModules) {
        try {
            Class.forName(KOTLIN_JACKSON_MODULE, false, Thread.currentThread().getContextClassLoader());
            classPathJacksonModules.produce(new ClassPathJacksonModuleBuildItem(KOTLIN_JACKSON_MODULE));
        } catch (Exception ignored) {
        }
    }

    /**
     * Kotlin data classes that have multiple constructors need to have their final fields writable,
     * otherwise creating a instance of them with default values, fails in native mode
     */
    @BuildStep
    ReflectiveClassFinalFieldsWritablePredicateBuildItem dataClassPredicate() {
        return new ReflectiveClassFinalFieldsWritablePredicateBuildItem(new IsDataClassWithDefaultValuesPredicate());
    }

    @BuildStep
    void transformToOpenClasses(
            ApplicationIndexBuildItem applicationIndexBuildItem,
            BuildProducer<BytecodeTransformerBuildItem> transformer,
            AllOpenConfig allOpenConfig) {

        Collection<ClassInfo> classes = applicationIndexBuildItem.getIndex().getKnownClasses();

        ArrayList<DotName> classesToTransform = new ArrayList<>();
        for (ClassInfo classInfo : classes) {
            for (List<AnnotationInstance> annotationInstances : classInfo.annotations().values()) {
                for (AnnotationInstance annotationInstance : annotationInstances) {
                    for (DotName defaultAnnotation : allOpenConfig.defaultAnnotations) {
                        if (annotationInstance.name().equals(defaultAnnotation)) {
                            classesToTransform.add(classInfo.name());
                        }
                    }
                }
            }
        }

        for (DotName classToTransform : classesToTransform) {
            logger.info("ClassToTransform : " + classToTransform);
            transformer.produce(new BytecodeTransformerBuildItem(classToTransform.toString(),
                    new BiFunction<String, ClassVisitor, ClassVisitor>() {
                        @Override
                        public ClassVisitor apply(String s, ClassVisitor classVisitor) {
                            ClassVisitor cv = new ClassVisitor(Gizmo.ASM_API_VERSION, classVisitor) {
                                @Override
                                public void visit(int version, int access, String name, String signature, String superName,
                                        String[] interfaces) {
                                    super.visit(version, access, name, signature, superName, interfaces);
                                    access = access ^ Opcodes.ACC_FINAL;
                                    classVisitor.visit(version, access, name, signature, superName, interfaces);
                                }
                            };
                            return cv;
                        }
                    }));
        }

    }
}
