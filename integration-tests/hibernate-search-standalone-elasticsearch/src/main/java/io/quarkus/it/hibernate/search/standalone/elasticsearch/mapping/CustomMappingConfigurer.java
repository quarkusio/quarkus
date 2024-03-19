package io.quarkus.it.hibernate.search.standalone.elasticsearch.mapping;

import jakarta.inject.Inject;

import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.mapper.pojo.standalone.mapping.StandalonePojoMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.standalone.mapping.StandalonePojoMappingConfigurer;

import io.quarkus.hibernate.search.standalone.elasticsearch.SearchExtension;
import io.quarkus.it.hibernate.search.standalone.elasticsearch.analysis.MyCdiContext;

@SearchExtension
public class CustomMappingConfigurer implements StandalonePojoMappingConfigurer {
    @Inject
    MyCdiContext cdiContext;

    @Override
    public void configure(StandalonePojoMappingConfigurationContext context) {
        MyCdiContext.checkAvailable(cdiContext);
        TypeMappingStep type = context.programmaticMapping().type(EntityMappedProgrammatically.class);
        type.searchEntity();
        type.indexed().index(EntityMappedProgrammatically.INDEX);
        type.property("id").documentId();
        type.property("text").fullTextField();
    }
}
