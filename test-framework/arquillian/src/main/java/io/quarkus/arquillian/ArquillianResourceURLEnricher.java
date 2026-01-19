package io.quarkus.arquillian;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.jboss.arquillian.container.spi.context.annotation.DeploymentScoped;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.TestEnricher;

import io.quarkus.registry.ValueRegistry;
import io.quarkus.registry.ValueRegistry.RuntimeKey;

/**
 * Performs injection of @ArquillianResource fields of type java.net.URL
 */
public class ArquillianResourceURLEnricher implements TestEnricher {

    @Inject
    @DeploymentScoped
    private Instance<QuarkusDeployment> deployment;

    @Override
    public void enrich(Object testCase) {
        if (QuarkusDeployableContainer.testInstance != null) {
            Class<?> clazz = QuarkusDeployableContainer.testInstance.getClass();
            while (clazz != Object.class) {
                Field[] fields = clazz.getDeclaredFields();
                for (Field field : fields) {
                    for (Annotation annotation : field.getAnnotations()) {
                        if (annotation.annotationType().getName().equals(ArquillianResource.class.getName())) {
                            if (field.getType().equals(URL.class)) {
                                try {
                                    field.setAccessible(true);
                                    field.set(QuarkusDeployableContainer.testInstance, new URL(testUrl(deployment)));
                                    break;
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            } else if (field.getType().equals(URI.class)) {
                                try {
                                    field.setAccessible(true);
                                    field.set(QuarkusDeployableContainer.testInstance, URI.create(testUrl(deployment)));
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
        Object[] values = new Object[method.getParameterTypes().length];
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            ArquillianResource resource = getResourceAnnotation(method.getParameterAnnotations()[i]);
            if (resource != null) {
                if (parameterTypes[i].equals(URL.class)) {
                    try {
                        values[i] = new URL(testUrl(deployment));
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                }
                if (parameterTypes[i].equals(URI.class)) {
                    values[i] = URI.create(testUrl(deployment));
                }
            }
        }
        return values;
    }

    private String testUrl(Instance<QuarkusDeployment> deployment) {
        ValueRegistry valueRegistry = deployment.get().getRunningApp().valueRegistry();
        String url = valueRegistry.get(RuntimeKey.key("test.url"));
        // This is to work around https://github.com/arquillian/arquillian-core/issues/216
        if (!url.endsWith("/")) {
            url = url + "/";
        }
        return url;
    }

    private ArquillianResource getResourceAnnotation(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation.annotationType() == ArquillianResource.class) {
                return (ArquillianResource) annotation;
            }
        }
        return null;
    }
}
