package io.quarkus.test.spring;

import java.lang.annotation.Annotation;

import org.junit.jupiter.api.extension.ExtensionContext;

import io.quarkus.test.junit.QuarkusTestExtension;

public class SpringBootTestAutoExtension extends QuarkusTestExtension {

    private static final String SPRING_BOOT_TEST_ANNOTATION = "org.springframework.boot.test.context.SpringBootTest";

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        System.out.println("========================================");
        System.out.println("SpringBootTestAutoExtension LOADED!");
        System.out.println("========================================");

        Class<?> testClass = context.getRequiredTestClass();

        boolean hasSpringBootTest = hasSpringBootTestAnnotation(testClass);

//        if (hasSpringBootTest) {
//            System.out.println("Spring Boot Test Annotation detected");
//
//            processSpringBootTestConfiguration(testClass);
//
//            System.out.println("🚀 Triggering Quarkus...");
//            super.beforeAll(context);
//            System.out.println("Quarkus triggered!");
//
//        } else {
//            System.out.println("No @SpringBootTest detected, skipping");
//        }
    }

    private boolean hasSpringBootTestAnnotation(Class<?> testClass) {
        try {
            Class<? extends Annotation> springBootTestClass = Class.forName(SPRING_BOOT_TEST_ANNOTATION)
                    .asSubclass(Annotation.class);

            return testClass.isAnnotationPresent(springBootTestClass);

        } catch (ClassNotFoundException e) {
            // @SpringBootTest is not in the classpath
            return false;
        }
    }

    private void processSpringBootTestConfiguration(Class<?> testClass) {
        try {
            Class<? extends Annotation> springBootTestClass = Class.forName(SPRING_BOOT_TEST_ANNOTATION)
                    .asSubclass(Annotation.class);

            Annotation annotation = testClass.getAnnotation(springBootTestClass);

            if (annotation != null) {
                System.out.println("@SpringBootTest found ");
            }

        } catch (Exception e) {
            System.err.println("⚠️ Error processing @SpringBootTest: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
