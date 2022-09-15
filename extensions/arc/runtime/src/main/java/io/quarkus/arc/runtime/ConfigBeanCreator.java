package io.quarkus.arc.runtime;

import java.util.Map;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.enterprise.inject.spi.InjectionPoint;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.impl.InjectionPointProvider;
import io.smallrye.config.inject.ConfigProducerUtil;

public class ConfigBeanCreator implements BeanCreator<Object> {
    @Override
    public Object create(CreationalContext<Object> creationalContext, Map<String, Object> params) {
        String requiredType = params.get("requiredType").toString();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = ConfigBeanCreator.class.getClassLoader();
        }

        try {
            Class.forName(requiredType, true, cl);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Cannot load required type: " + requiredType);
        }

        InjectionPoint injectionPoint = InjectionPointProvider.get();
        if (injectionPoint == null) {
            throw new IllegalStateException("No current injection point found");
        }

        try {
            return ConfigProducerUtil.getValue(injectionPoint, ConfigProvider.getConfig());
        } catch (Exception e) {
            throw new DeploymentException(e);
        }
    }
}
