package io.quarkus.hibernate.search.orm.elasticsearch.test.offline;

import org.hibernate.search.mapper.orm.mapping.HibernateOrmMappingConfigurationContext;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingConfigurer;

public class ProgrammaticMappingConfigurer implements HibernateOrmSearchMappingConfigurer {
    @Override
    public void configure(HibernateOrmMappingConfigurationContext context) {
        context.programmaticMapping().type(ProgrammaticallyMappedEntity.class).indexed();
    }
}
