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

        // Detecta si tiene @SpringBootTest (la de Spring original)
        boolean hasSpringBootTest = hasSpringBootTestAnnotation(testClass);

        if (hasSpringBootTest) {
            System.out.println("✅ Spring Boot Test Annotation detected");

            // Procesa la configuración de @SpringBootTest
            processSpringBootTestConfiguration(testClass);

            // CRÍTICO: Llama a super.beforeAll() para activar Quarkus
            System.out.println("🚀 Activando Quarkus...");
            super.beforeAll(context);
            System.out.println("✅ Quarkus activado!");

        } else {
            System.out.println("ℹ️ No @SpringBootTest detected, skipping");
            // Si no tiene @SpringBootTest, no hacemos nada
            // Esto permite que otros tests normales funcionen sin interferencia
        }
    }

    private boolean hasSpringBootTestAnnotation(Class<?> testClass) {
        try {
            Class<? extends Annotation> springBootTestClass = Class.forName(SPRING_BOOT_TEST_ANNOTATION)
                    .asSubclass(Annotation.class);

            return testClass.isAnnotationPresent(springBootTestClass);

        } catch (ClassNotFoundException e) {
            // La anotación de Spring no está en el classpath
            return false;
        }
    }

    private void processSpringBootTestConfiguration(Class<?> testClass) {
        try {
            Class<? extends Annotation> springBootTestClass = Class.forName(SPRING_BOOT_TEST_ANNOTATION)
                    .asSubclass(Annotation.class);

            Annotation annotation = testClass.getAnnotation(springBootTestClass);

            if (annotation != null) {
                // Usa reflection para leer el atributo 'properties'
                java.lang.reflect.Method propertiesMethod = springBootTestClass.getMethod("properties");
                String[] properties = (String[]) propertiesMethod.invoke(annotation);

                System.out.println("📝 Procesando properties de @SpringBootTest:");
                for (String property : properties) {
                    System.out.println("   - " + property);

                    // Aplica como system property
                    String[] parts = property.split("=", 2);
                    if (parts.length == 2) {
                        System.setProperty(parts[0].trim(), parts[1].trim());
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("⚠️ Error procesando @SpringBootTest: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
