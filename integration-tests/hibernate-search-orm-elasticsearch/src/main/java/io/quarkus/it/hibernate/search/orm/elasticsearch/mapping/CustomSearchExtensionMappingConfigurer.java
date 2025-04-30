package io.quarkus.it.hibernate.search.orm.elasticsearch.mapping;

import jakarta.inject.Inject;

import org.hibernate.search.mapper.orm.mapping.HibernateOrmMappingConfigurationContext;

import io.quarkus.hibernate.search.orm.elasticsearch.SearchExtension;
import io.quarkus.it.hibernate.search.orm.elasticsearch.analysis.MyCdiContext;

@SearchExtension
public class CustomSearchExtensionMappingConfigurer extends AbstractCustomMappingConfigurer {
    @Inject
    MyCdiContext cdiContext;

    public CustomSearchExtensionMappingConfigurer() {
        super(MappingTestingSearchExtensionEntity.class, MappingTestingSearchExtensionEntity.INDEX);
    }

    @Override
    public void configure(HibernateOrmMappingConfigurationContext context) {
        MyCdiContext.checkAvailable(cdiContext);
        super.configure(context);
    }
}
