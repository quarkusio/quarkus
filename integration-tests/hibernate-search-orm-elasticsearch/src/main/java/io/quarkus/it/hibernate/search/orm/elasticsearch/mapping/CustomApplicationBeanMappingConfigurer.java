package io.quarkus.it.hibernate.search.orm.elasticsearch.mapping;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.hibernate.search.mapper.orm.mapping.HibernateOrmMappingConfigurationContext;

import io.quarkus.it.hibernate.search.orm.elasticsearch.analysis.MyCdiContext;

@ApplicationScoped
@Named("custom-application-bean-mapping-configurer")
public class CustomApplicationBeanMappingConfigurer extends AbstractCustomMappingConfigurer {
    @Inject
    MyCdiContext cdiContext;

    public CustomApplicationBeanMappingConfigurer() {
        super(MappingTestingApplicationBeanEntity.class, MappingTestingApplicationBeanEntity.INDEX);
    }

    @Override
    public void configure(HibernateOrmMappingConfigurationContext context) {
        MyCdiContext.checkAvailable(cdiContext);
        super.configure(context);
    }
}
