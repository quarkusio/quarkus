package org.jboss.shamrock.agroal.runtime;

import org.jboss.shamrock.runtime.Template;
import org.jboss.shamrock.runtime.cdi.BeanContainer;
import org.jboss.shamrock.runtime.cdi.BeanContainerListener;

@Template
public class DataSourceTemplate {

    public BeanContainerListener addDatasource(DataSourceConfig config) {
        return new BeanContainerListener() {
            @Override
            public void created(BeanContainer beanContainer) {
                DataSourceProducer producer = beanContainer.instance(DataSourceProducer.class);
                try {
                    producer.setDriver(Class.forName(config.driver, true, Thread.currentThread().getContextClassLoader()));
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
                producer.setUrl(config.url);
                if (config.user.isPresent()) {
                    producer.setUserName(config.user.get());
                }
                if (config.password.isPresent()) {
                    producer.setPassword(config.password.get());
                }
                producer.setMinSize(config.minSize);
                producer.setMaxSize(config.maxSize);
            }
        };
    }

}
