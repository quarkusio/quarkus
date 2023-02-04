package io.quarkus.it.hibernate.search.orm.opensearch.analysis;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Named;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;

@Dependent
@Named("backend-analysis")
public class BackendAnalysisConfigurer implements ElasticsearchAnalysisConfigurer {
    @Override
    public void configure(ElasticsearchAnalysisConfigurationContext context) {
        context.analyzer("standard").type("standard");
        context.normalizer("lowercase").custom().tokenFilters("lowercase");
    }
}
