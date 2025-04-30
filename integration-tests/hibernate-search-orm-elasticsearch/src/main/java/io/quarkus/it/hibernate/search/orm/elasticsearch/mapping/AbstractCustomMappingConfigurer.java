package io.quarkus.it.hibernate.search.orm.elasticsearch.mapping;

import org.hibernate.search.mapper.orm.mapping.HibernateOrmMappingConfigurationContext;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingConfigurer;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;

public abstract class AbstractCustomMappingConfigurer implements HibernateOrmSearchMappingConfigurer {

    private final Class<?> type;
    private final String indexName;

    protected AbstractCustomMappingConfigurer(Class<?> type, String indexName) {
        this.type = type;
        this.indexName = indexName;
    }

    @Override
    public void configure(HibernateOrmMappingConfigurationContext context) {
        TypeMappingStep type = context.programmaticMapping().type(this.type);
        type.indexed().index(this.indexName);
        type.property("id").documentId();
        type.property("text").fullTextField();
    }
}
