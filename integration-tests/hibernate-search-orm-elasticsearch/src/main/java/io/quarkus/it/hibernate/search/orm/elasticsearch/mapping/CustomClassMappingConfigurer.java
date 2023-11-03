package io.quarkus.it.hibernate.search.orm.elasticsearch.mapping;

import jakarta.inject.Inject;

import org.hibernate.search.mapper.orm.mapping.HibernateOrmMappingConfigurationContext;

import io.quarkus.it.hibernate.search.orm.elasticsearch.analysis.MyCdiContext;

public class CustomClassMappingConfigurer extends AbstractCustomMappingConfigurer {

    @Inject
    MyCdiContext cdiContext;

    public CustomClassMappingConfigurer() {
        super(MappingTestingClassEntity.class, MappingTestingClassEntity.INDEX);
    }

    @Override
    public void configure(HibernateOrmMappingConfigurationContext context) {
        MyCdiContext.checkNotAvailable(cdiContext);

        super.configure(context);
    }
}
