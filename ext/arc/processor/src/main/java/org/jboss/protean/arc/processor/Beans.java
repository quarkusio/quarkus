package org.jboss.protean.arc.processor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

final class Beans {

    private Beans() {
    }

    /**
     *
     * @param beanClass
     * @param beanDeployment
     * @return a new bean info
     */
    static BeanInfo createClassBean(ClassInfo beanClass, BeanDeployment beanDeployment) {
        Set<AnnotationInstance> qualifiers = new HashSet<>();
        ScopeInfo scope = null;
        Set<Type> types = Types.getTypeClosure(beanClass, Collections.emptyMap(), beanDeployment);
        for (AnnotationInstance annotation : beanClass.classAnnotations()) {
            if (beanDeployment.getQualifier(annotation.name()) != null) {
                qualifiers.add(annotation);
            } else if (scope == null) {
                scope = ScopeInfo.from(annotation.name());
            }
        }
        return new BeanInfo(beanClass, beanDeployment, scope, types, qualifiers, Injection.init(beanClass, beanDeployment), null);
    }

    /**
     *
     * @param producerMethod
     * @param declaringBean
     * @param beanDeployment
     * @return a new bean info
     */
    static BeanInfo createProducerMethod(MethodInfo producerMethod, BeanInfo declaringBean, BeanDeployment beanDeployment) {
        Set<AnnotationInstance> qualifiers = new HashSet<>();
        ScopeInfo scope = null;
        Set<Type> types = Types.getTypeClosure(producerMethod, beanDeployment);
        for (AnnotationInstance annotation : producerMethod.annotations()) {
            if (beanDeployment.getQualifier(annotation.name()) != null) {
                qualifiers.add(annotation);
            } else if (scope == null) {
                scope = ScopeInfo.from(annotation.name());
            }
        }
        return new BeanInfo(producerMethod, beanDeployment, scope, types, qualifiers, Injection.init(producerMethod, beanDeployment), declaringBean);
    }

    /**
     *
     * @param producerField
     * @param declaringBean
     * @param beanDeployment
     * @return a new bean info
     */
    static BeanInfo createProducerField(FieldInfo producerField, BeanInfo declaringBean, BeanDeployment beanDeployment) {
        Set<AnnotationInstance> qualifiers = new HashSet<>();
        ScopeInfo scope = null;
        Set<Type> types = Types.getTypeClosure(producerField, beanDeployment);
        for (AnnotationInstance annotation : producerField.annotations()) {
            if (beanDeployment.getQualifier(annotation.name()) != null) {
                qualifiers.add(annotation);
            } else if (scope == null) {
                scope = ScopeInfo.from(annotation.name());
            }
        }
        return new BeanInfo(producerField, beanDeployment, scope, types, qualifiers, Collections.emptyList(), declaringBean);
    }

}
