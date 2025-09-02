package io.quarkus.deployment.dev;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.MethodParameterInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ClassComparisonUtilTest {

    @Nested
    class CompareMethodAnnotations {

        @Test
        public void annotationsEqual() {
            AnnotationInstance instance1 = methodParameterAnnotation(AnnotationForTest1.class);
            AnnotationInstance instance2 = methodParameterAnnotation(AnnotationForTest1.class);

            List<AnnotationInstance> instances1 = List.of(instance1);
            List<AnnotationInstance> instances2 = List.of(instance2);

            Assertions.assertTrue(ClassComparisonUtil.compareMethodAnnotations(instances1, instances2));
        }

        @Test
        public void annotationsNotEqual() {
            AnnotationInstance instance1 = methodParameterAnnotation(AnnotationForTest1.class);
            AnnotationInstance instance2 = methodParameterAnnotation(AnnotationForTest2.class);

            List<AnnotationInstance> instances1 = List.of(instance1);
            List<AnnotationInstance> instances2 = List.of(instance2);

            Assertions.assertFalse(ClassComparisonUtil.compareMethodAnnotations(instances1, instances2));
        }

        @Test
        public void compareMethodAnnotationsSizeDiffer() {
            AnnotationInstance instance = methodParameterAnnotation(AnnotationForTest1.class);

            List<AnnotationInstance> instances = List.of(instance);

            Assertions.assertFalse(ClassComparisonUtil.compareMethodAnnotations(instances, List.of()));
            Assertions.assertFalse(ClassComparisonUtil.compareMethodAnnotations(List.of(), instances));
        }

        @Test
        public void multipleAnnotationsAtSamePosition() {
            List<AnnotationInstance> instances1 = List.of(
                    methodParameterAnnotation(AnnotationForTest1.class),
                    methodParameterAnnotation(AnnotationForTest2.class));
            List<AnnotationInstance> instances2 = List.of(
                    methodParameterAnnotation(AnnotationForTest2.class),
                    methodParameterAnnotation(AnnotationForTest1.class));

            Assertions.assertTrue(ClassComparisonUtil.compareMethodAnnotations(instances1, instances2));
        }

        @Test
        public void multipleAnnotations() {
            List<AnnotationInstance> instances1 = List.of(
                    methodParameterAnnotation(AnnotationForTest1.class, 1),
                    methodParameterAnnotation(AnnotationForTest2.class, 2));

            List<AnnotationInstance> instances2 = List.of(
                    methodParameterAnnotation(AnnotationForTest1.class, 2),
                    methodParameterAnnotation(AnnotationForTest2.class, 1));

            Assertions.assertFalse(ClassComparisonUtil.compareMethodAnnotations(instances1, instances2));
        }

        private static AnnotationInstance methodParameterAnnotation(
                Class<? extends Annotation> annotation) {
            return methodParameterAnnotation(annotation, 1);
        }

        private static AnnotationInstance methodParameterAnnotation(
                Class<? extends Annotation> annotation, int position) {
            MethodParameterInfo target = MethodParameterInfo.create(null, (short) position);
            return AnnotationInstance.builder(annotation).buildWithTarget(target);
        }

        @Target({ ElementType.PARAMETER, ElementType.TYPE_USE })
        @Retention(RetentionPolicy.RUNTIME)
        @Documented
        private @interface AnnotationForTest1 {
        }

        @Target({ ElementType.PARAMETER, ElementType.TYPE_USE })
        @Retention(RetentionPolicy.RUNTIME)
        @Documented
        private @interface AnnotationForTest2 {
        }
    }

}
