package io.quarkus.it.hibernate.search.elasticsearch.aws.analysis;

import javax.enterprise.context.Dependent;
import javax.inject.Named;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;

@Dependent
@Named("my-analysis-configurer")
public class MyAnalysisConfigurer implements ElasticsearchAnalysisConfigurer {
    @Override
    public void configure(ElasticsearchAnalysisConfigurationContext context) {
        context.analyzer("standard").type("standard");
        context.normalizer("lowercase").custom().tokenFilters("lowercase");

        context.analyzer("backend-level-analyzer").custom()
                .tokenizer("keyword")
                .tokenFilters("backend-level-tokenfilter");

        context.tokenFilter("backend-level-tokenfilter").type("pattern_replace")
                .param("pattern", ".+")
                .param("replacement", "token_inserted_by_backend_analysis");
    }
}
