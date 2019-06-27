package io.quarkus.it.hibernate.search.elasticsearch.search;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.analysis.model.dsl.ElasticsearchAnalysisDefinitionContainerContext;

public class DefaultITAnalysisConfigurer implements ElasticsearchAnalysisConfigurer {

    @Override
    public void configure(ElasticsearchAnalysisDefinitionContainerContext context) {
        context.analyzer("standard").type("standard");
        context.normalizer("lowercase").custom().withTokenFilters("lowercase");
    }
}
