package io.quarkus.test.component;

import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;
import io.smallrye.config.SmallRyeConfig;

public class ConfigMappingBeanCreator implements BeanCreator<Object> {

    @Override
    public Object create(SyntheticCreationalContext<Object> context) {
        String prefix = context.getParams().get("prefix").toString();
        Class<?> mappingClass = tryLoad(context.getParams().get("mappingClass").toString());
        SmallRyeConfig config = ConfigBeanCreator.getConfig().unwrap(SmallRyeConfig.class);
        return config.getConfigMapping(mappingClass, prefix);
    }

    static Class<?> tryLoad(String name) {
        try {
            return Thread.currentThread().getContextClassLoader().loadClass(name);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Unable to load type: " + name, e);
        }
    }

}
