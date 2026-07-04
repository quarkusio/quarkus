package io.quarkus.arc.runtime;

import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.enterprise.inject.spi.InjectionPoint;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.impl.InjectionPointProvider;
import io.smallrye.config.inject.ConfigProducerUtil;

public class ConfigPropertyCreator implements BeanCreator<Object> {
    @Override
    public Object create(SyntheticCreationalContext<Object> context) {
        String requiredType = context.getParams().get("requiredType").toString();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = ConfigPropertyCreator.class.getClassLoader();
        }

        try {
            Class.forName(requiredType, true, cl);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Cannot load required type: " + requiredType);
        }

        InjectionPoint injectionPoint = InjectionPointProvider.getCurrent(context);
        if (injectionPoint == null) {
            throw new IllegalStateException("No current injection point found");
        }

        ConfigStaticInitCheckInterceptor.recordConfigValue(injectionPoint, null);

        try {
            return ConfigProducerUtil.getValue(injectionPoint, ConfigProvider.getConfig());
        } catch (Exception e) {
            throw new DeploymentException(e);
        }
    }
}
