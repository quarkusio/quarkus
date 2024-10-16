package io.quarkus.it.hibernate.search.standalone.elasticsearch.analysis;

import jakarta.inject.Inject;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;

import io.quarkus.hibernate.search.standalone.elasticsearch.SearchExtension;

@SearchExtension
public class BackendAnalysisConfigurer implements ElasticsearchAnalysisConfigurer {
    @Inject
    MyCdiContext cdiContext;

    @Override
    public void configure(ElasticsearchAnalysisConfigurationContext context) {
        MyCdiContext.checkAvailable(cdiContext);

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
