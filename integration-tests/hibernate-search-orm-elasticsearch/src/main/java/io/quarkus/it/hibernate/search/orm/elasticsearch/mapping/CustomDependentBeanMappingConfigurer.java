package io.quarkus.it.hibernate.search.orm.elasticsearch.mapping;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.hibernate.search.mapper.orm.mapping.HibernateOrmMappingConfigurationContext;

import io.quarkus.it.hibernate.search.orm.elasticsearch.analysis.MyCdiContext;

@Dependent
@Named("custom-dependent-bean-mapping-configurer")
public class CustomDependentBeanMappingConfigurer extends AbstractCustomMappingConfigurer {
    @Inject
    MyCdiContext cdiContext;

    public CustomDependentBeanMappingConfigurer() {
        super(MappingTestingDependentBeanEntity.class, MappingTestingDependentBeanEntity.INDEX);
    }

    @Override
    public void configure(HibernateOrmMappingConfigurationContext context) {
        MyCdiContext.checkAvailable(cdiContext);
        super.configure(context);
    }
}
