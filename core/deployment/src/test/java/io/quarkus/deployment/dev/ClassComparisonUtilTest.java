package io.quarkus.deployment.dev;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
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

    @Nested
    class CompareAnnotationsWithTypeUse {

        /**
         * Test for https://github.com/quarkusio/quarkus/issues/53971
         * When an annotation has both FIELD and TYPE_USE targets, javac generates both
         * a field annotation and a type annotation in the bytecode. The old implementation
         * used Collectors.toMap() which threw IllegalStateException on duplicate names.
         * The new implementation uses AnnotationInstanceEquivalenceProxy which compares by
         * type and values only, ignoring targets, so duplicates are properly handled.
         */
        @Test
        public void fieldAndTypeUseAnnotationsFromRealBytecode() throws IOException {
            // Index a class that has a field with @NotNullAnnotation (which has both FIELD and TYPE_USE targets)
            Indexer indexer = new Indexer();
            indexer.indexClass(EntityWithTypeUseAnnotation.class);
            Index index = indexer.complete();

            ClassInfo classInfo = index.getClassByName(EntityWithTypeUseAnnotation.class);
            FieldInfo field = classInfo.field("name");

            // The field has both a FIELD annotation and a TYPE annotation for @NotNullAnnotation
            List<AnnotationInstance> annotations = field.annotations();

            // With the old map-based approach using toMap(AnnotationInstance::name, Function.identity()),
            // this would throw IllegalStateException: Duplicate key
            // The new approach uses equivalence proxy which deduplicates by type+values
            Assertions.assertDoesNotThrow(() -> ClassComparisonUtil.compareAnnotations(annotations, annotations));
        }

        @Test
        public void repeatableAnnotationsWithContainer() {
            // When using @Repeatable, e.g., @NotNull @NotNull, javac generates a container annotation
            // @NotNull.List containing the repeated annotations
            AnnotationInstance container = annotation(NotNullAnnotation.List.class);

            // Comparing container vs no container should be different
            List<AnnotationInstance> instances1 = List.of(container);
            List<AnnotationInstance> instances2 = List.of(annotation(NotNullAnnotation.class));

            Assertions.assertFalse(ClassComparisonUtil.compareAnnotations(instances1, instances2));
        }

        @Test
        public void differentAnnotationTypesAreNotEqual() {
            AnnotationInstance ann1 = annotation(NotNullAnnotation.class);
            AnnotationInstance ann2 = annotation(NotBlankAnnotation.class);

            List<AnnotationInstance> instances1 = List.of(ann1);
            List<AnnotationInstance> instances2 = List.of(ann2);

            Assertions.assertFalse(ClassComparisonUtil.compareAnnotations(instances1, instances2));
        }

        @Test
        public void multipleAnnotationTypes() {
            // Multiple different annotation types
            AnnotationInstance ann1 = annotation(NotNullAnnotation.class);
            AnnotationInstance ann2 = annotation(NotBlankAnnotation.class);

            List<AnnotationInstance> instances1 = List.of(ann1, ann2);
            List<AnnotationInstance> instances2 = List.of(ann2, ann1); // Different order

            Assertions.assertTrue(ClassComparisonUtil.compareAnnotations(instances1, instances2));
        }

        private static AnnotationInstance annotation(Class<? extends Annotation> annotationClass) {
            return AnnotationInstance.builder(annotationClass).build();
        }

        @java.lang.annotation.Repeatable(NotNullAnnotation.List.class)
        @Target({ ElementType.FIELD, ElementType.TYPE_USE })
        @Retention(RetentionPolicy.RUNTIME)
        @Documented
        private @interface NotNullAnnotation {
            @Target({ ElementType.FIELD, ElementType.TYPE_USE })
            @Retention(RetentionPolicy.RUNTIME)
            @Documented
            @interface List {
                NotNullAnnotation[] value();
            }
        }

        @Target({ ElementType.FIELD, ElementType.TYPE_USE })
        @Retention(RetentionPolicy.RUNTIME)
        @Documented
        private @interface NotBlankAnnotation {
        }

        static class EntityWithTypeUseAnnotation {
            @NotNullAnnotation
            public String name;
        }
    }

}
