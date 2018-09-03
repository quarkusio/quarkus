package org.jboss.protean.arc.processor;

abstract class AbstractGenerator {

    protected String providerName(String name) {
        // TODO we can do better
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    protected String getBaseName(BeanInfo bean, String beanClassName) {
        String name = Types.getSimpleName(beanClassName);
        return name.substring(0, name.indexOf(BeanGenerator.BEAN_SUFFIX));
    }
}
