package org.jboss.shamrock.agroal.runtime;

import org.jboss.shamrock.runtime.Template;
import org.jboss.shamrock.runtime.cdi.BeanContainer;
import org.jboss.shamrock.runtime.cdi.BeanContainerListener;

@Template
public class DataSourceTemplate {

    public BeanContainerListener addDatasource(
                                  String url, Class driver,
                                  String userName, String password, Integer minSize, Integer maxSize) {
        return new BeanContainerListener() {
            @Override
            public void created(BeanContainer beanContainer) {
                DataSourceProducer producer = beanContainer.instance(DataSourceProducer.class);
                producer.setDriver(driver);
                producer.setUrl(url);
                producer.setUserName(userName);
                producer.setPassword(password);
                producer.setMinSize(minSize);
                producer.setMaxSize(maxSize);
            }
        };
    }

}
