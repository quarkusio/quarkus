package org.jboss.shamrock.deployment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BeanDeployment {

    private final List<String> additionalBeans = new ArrayList<>();
    private final Map<String, byte[]> generatedBeans = new HashMap<>();

    public void addAdditionalBean(Class<?>... beanClass) {
        additionalBeans.addAll(Arrays.stream(beanClass).map(Class::getName).collect(Collectors.toList()));
    }

    public void addAdditionalBean(String... beanClass) {
        additionalBeans.addAll(Arrays.stream(beanClass).collect(Collectors.toList()));
    }
    public void addGeneratedBean(String name, byte[] bean) {
        generatedBeans.put(name, bean);
    }

    public List<String> getAdditionalBeans() {
        return additionalBeans;
    }

    public Map<String, byte[]> getGeneratedBeans() {
        return generatedBeans;
    }
}
