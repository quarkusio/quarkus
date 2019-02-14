package org.jboss.shamrock.extest;

import org.jboss.logging.Logger;
import org.jboss.shamrock.arc.runtime.BeanContainer;
import org.jboss.shamrock.arc.runtime.BeanContainerListener;
import org.jboss.shamrock.runtime.annotations.Template;

@Template
public class TestTemplate {
    static final Logger log = Logger.getLogger(TestTemplate.class);

    BeanContainerListener configureBeans(Class<?> beanClass) {
        return beanContainer -> {
            log.infof("Begin BeanContainerListener callback\n");
            Object instance = beanContainer.instance(beanClass);
            log.infof("configureBeans, instance=%s\n", instance);
        };
    }
}
