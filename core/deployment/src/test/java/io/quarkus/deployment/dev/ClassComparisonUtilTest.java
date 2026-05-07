package io.quarkus.deployment.dev;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.jboss.jandex.Index;
import org.jboss.jandex.MethodParameterInfo;
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

            assertTrue(ClassComparisonUtil.compareMethodAnnotations(instances1, instances2));
        }

        @Test
        public void annotationsNotEqual() {
            AnnotationInstance instance1 = methodParameterAnnotation(AnnotationForTest1.class);
            AnnotationInstance instance2 = methodParameterAnnotation(AnnotationForTest2.class);

            List<AnnotationInstance> instances1 = List.of(instance1);
            List<AnnotationInstance> instances2 = List.of(instance2);

            assertFalse(ClassComparisonUtil.compareMethodAnnotations(instances1, instances2));
        }

        @Test
        public void compareMethodAnnotationsSizeDiffer() {
            AnnotationInstance instance = methodParameterAnnotation(AnnotationForTest1.class);

            List<AnnotationInstance> instances = List.of(instance);

            assertFalse(ClassComparisonUtil.compareMethodAnnotations(instances, List.of()));
            assertFalse(ClassComparisonUtil.compareMethodAnnotations(List.of(), instances));
        }

        @Test
        public void multipleAnnotationsAtSamePosition() {
            List<AnnotationInstance> instances1 = List.of(
                    methodParameterAnnotation(AnnotationForTest1.class),
                    methodParameterAnnotation(AnnotationForTest2.class));
            List<AnnotationInstance> instances2 = List.of(
                    methodParameterAnnotation(AnnotationForTest2.class),
                    methodParameterAnnotation(AnnotationForTest1.class));

            assertTrue(ClassComparisonUtil.compareMethodAnnotations(instances1, instances2));
        }

        @Test
        public void multipleAnnotations() {
            List<AnnotationInstance> instances1 = List.of(
                    methodParameterAnnotation(AnnotationForTest1.class, 1),
                    methodParameterAnnotation(AnnotationForTest2.class, 2));

            List<AnnotationInstance> instances2 = List.of(
                    methodParameterAnnotation(AnnotationForTest1.class, 2),
                    methodParameterAnnotation(AnnotationForTest2.class, 1));

            assertFalse(ClassComparisonUtil.compareMethodAnnotations(instances1, instances2));
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
         * Check that our approach to calling isSameStructure() on two different classes does make sense
         * -- i.e. that the method doesn't care about the class name, which would make all other tests meaningless.
         */
        @Test
        public void sanity() throws IOException {
            assertTrue(isSameStructure(EntityWithTypeUseAnnotation.class, EntityWithTypeUseAnnotation2.class));
        }

        @Test
        public void repeatableAnnotationsWithContainer() throws IOException {
            // When using @Repeatable, e.g., @NotNull @NotNull, javac generates a container annotation
            // @NotNull.List containing the repeated annotations
            assertFalse(isSameStructure(EntityWithTypeUseAnnotation.class, EntityWithTypeUseAnnotationRepeated.class));
        }

        /**
         * Test for https://github.com/quarkusio/quarkus/issues/53971
         * When an annotation has both FIELD and TYPE_USE targets, javac generates both
         * a field annotation and a type annotation in the bytecode. Using field.annotations()
         * would return both and cause duplicate keys when building a map by annotation name.
         * The fix uses field.declaredAnnotations() which only returns FIELD annotations.
         */
        @Test
        public void fieldAndTypeUseAnnotationsFromRealBytecode() throws IOException {
            // Compare the class to itself - this should not throw any exceptions
            assertTrue(isSameStructure(EntityWithTypeUseAnnotation.class, EntityWithTypeUseAnnotation.class));
        }

        @Test
        public void typeUseAnnotationPositionChangeFieldList() throws IOException {
            // Test that we detect when a TYPE_USE annotation moves position on a field
            // e.g., from @Ann List<String> to List<@Ann String>
            //
            // This works because the field type comparison (type().equals()) includes
            // annotation positioning information. The Type objects are:
            // - @Ann List<String> vs List<@Ann String>
            // These are structurally different types, so isSameStructure() correctly
            // detects the change via the type comparison before even checking annotations.
            assertFalse(isSameStructure(EntityWithFieldTypeAnnotationOnType.class,
                    EntityWithFieldTypeAnnotationOnTypeArgument.class));
        }

        @Test
        public void typeUseAnnotationPositionChangeFieldMap() throws IOException {
            // Test Map<@Ann String, String> vs Map<String, @Ann String> on a field
            // The annotation moves from key to value type - should be detected as different.
            //
            // Again, the type comparison handles this because the Type objects include
            // annotation positioning: Map<@Ann String, String> vs Map<String, @Ann String>
            // are structurally different types.
            assertFalse(isSameStructure(EntityWithMapKeyAnnotation.class, EntityWithMapValueAnnotation.class));
        }

        @Test
        public void typeUseAnnotationPositionChangeMethodReturnType() throws IOException {
            // Test that we detect when a TYPE_USE annotation moves position on a method return type
            // e.g., from @Ann List<String> to List<@Ann String>
            assertFalse(isSameStructure(EntityWithMethodReturnTypeAnnotationOnType.class,
                    EntityWithMethodReturnTypeAnnotationOnTypeArgument.class));
        }

        @Test
        public void typeUseAnnotationPositionChangeMethodParameter() throws IOException {
            // Test that we detect when a TYPE_USE annotation moves position on a method parameter
            // e.g., from @Ann List<String> to List<@Ann String>
            assertFalse(isSameStructure(EntityWithMethodParameterTypeAnnotationOnType.class,
                    EntityWithMethodParameterTypeAnnotationOnTypeArgument.class));
        }

        @Test
        public void typeUseAnnotationRemoved() throws IOException {
            // Test that we detect when a TYPE_USE annotation is removed from a field type
            // e.g., from @Ann List<String> to List<String>
            assertFalse(isSameStructure(EntityWithFieldTypeAnnotationOnType.class, EntityWithoutTypeAnnotation.class));
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

        @Target({ ElementType.TYPE_USE })
        @Retention(RetentionPolicy.RUNTIME)
        @Documented
        private @interface TypeOnlyAnnotation {
        }

        static class EntityWithTypeUseAnnotation {
            @NotNullAnnotation
            public String name;
        }

        static class EntityWithTypeUseAnnotation2 {
            @NotNullAnnotation
            public String name;
        }

        static class EntityWithTypeUseAnnotationRepeated {
            @NotNullAnnotation
            @NotNullAnnotation
            public String name;
        }

        static class EntityWithFieldTypeAnnotationOnType {
            @TypeOnlyAnnotation
            public List<String> items;
        }

        static class EntityWithFieldTypeAnnotationOnTypeArgument {
            public List<@TypeOnlyAnnotation String> items;
        }

        static class EntityWithMapKeyAnnotation {
            public java.util.Map<@TypeOnlyAnnotation String, String> data;
        }

        static class EntityWithMapValueAnnotation {
            public java.util.Map<String, @TypeOnlyAnnotation String> data;
        }

        static class EntityWithMethodReturnTypeAnnotationOnType {
            public @TypeOnlyAnnotation List<String> getItems() {
                return null;
            }
        }

        static class EntityWithMethodReturnTypeAnnotationOnTypeArgument {
            public List<@TypeOnlyAnnotation String> getItems() {
                return null;
            }
        }

        static class EntityWithMethodParameterTypeAnnotationOnType {
            public void process(@TypeOnlyAnnotation List<String> items) {
            }
        }

        static class EntityWithMethodParameterTypeAnnotationOnTypeArgument {
            public void process(List<@TypeOnlyAnnotation String> items) {
            }
        }

        static class EntityWithoutTypeAnnotation {
            public List<String> items;
        }
    }

    private static boolean isSameStructure(Class<?> clazz1, Class<?> clazz2) throws IOException {
        ClassInfo classInfo1 = Index.singleClass(clazz1);
        ClassInfo classInfo2 = Index.singleClass(clazz2);
        return ClassComparisonUtil.isSameStructure(classInfo1, classInfo2);
    }

}
