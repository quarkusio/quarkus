package org.jboss.shamrock.weld.deployment;

import java.util.ArrayList;
import java.util.List;

public class WeldDeployment {

    private final List<Class<?>> additionalBeans = new ArrayList<>();

    public void addAdditionalBean(Class<?> beanClass) {
        additionalBeans.add(beanClass);
    }

    List<Class<?>> getAdditionalBeans() {
        return additionalBeans;
    }
}
