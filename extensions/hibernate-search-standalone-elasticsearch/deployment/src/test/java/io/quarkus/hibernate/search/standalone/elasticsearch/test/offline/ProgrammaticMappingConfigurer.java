package io.quarkus.hibernate.search.standalone.elasticsearch.test.offline;

import org.hibernate.search.mapper.pojo.standalone.mapping.StandalonePojoMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.standalone.mapping.StandalonePojoMappingConfigurer;

public class ProgrammaticMappingConfigurer implements StandalonePojoMappingConfigurer {
    @Override
    public void configure(StandalonePojoMappingConfigurationContext context) {
        var type = context.programmaticMapping().type(ProgrammaticallyMappedEntity.class);
        type.searchEntity();
        type.indexed();
        type.property("id").documentId();
    }
}
