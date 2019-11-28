package io.quarkus.arquillian;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.TestEnricher;

/**
 * Performs injection of @ArquillianResource fields of type java.net.URL
 */
public class ArquillianResourceURLEnricher implements TestEnricher {

    @Override
    public void enrich(Object testCase) {
        if (QuarkusDeployableContainer.testInstance != null) {
            Class clazz = QuarkusDeployableContainer.testInstance.getClass();
            while (clazz != Object.class) {
                Field[] fields = clazz.getDeclaredFields();
                for (Field field : fields) {
                    for (Annotation annotation : field.getAnnotations()) {
                        if (annotation.annotationType().getName().equals(ArquillianResource.class.getName())) {
                            if (field.getType().equals(URL.class)) {
                                try {
                                    field.setAccessible(true);
                                    URL url = new URL(System.getProperty("test.url"));
                                    field.set(QuarkusDeployableContainer.testInstance, url);
                                    break;
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            } else if (field.getType().equals(URI.class)) {
                                try {
                                    field.setAccessible(true);
                                    URI url = new URI(System.getProperty("test.url"));
                                    field.set(QuarkusDeployableContainer.testInstance, url);
                                    break;
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    }
                }
                clazz = clazz.getSuperclass();
            }
        }
    }

    @Override
    public Object[] resolve(Method method) {
        return null;
    }
}
