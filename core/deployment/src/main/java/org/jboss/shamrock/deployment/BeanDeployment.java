package org.jboss.shamrock.deployment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BeanDeployment {

    private final List<Class<?>> additionalBeans = new ArrayList<>();

    public void addAdditionalBean(Class<?> ... beanClass) {
        additionalBeans.addAll(Arrays.asList(beanClass));
    }

    public List<Class<?>> getAdditionalBeans() {
        return additionalBeans;
    }

}
