package io.quarkus.it.hibernate.search.orm.elasticsearch.analysis;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;

@Dependent
@Named("backend-analysis")
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
