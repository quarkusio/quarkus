package io.quarkus.it.hibernate.search.standalone.elasticsearch.analysis;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;

import io.quarkus.hibernate.search.standalone.elasticsearch.SearchExtension;

@ApplicationScoped
@SearchExtension(index = "Analysis2TestingEntity")
public class IndexAnalysis2Configurer implements ElasticsearchAnalysisConfigurer {
    @Inject
    MyCdiContext cdiContext;

    @Override
    public void configure(ElasticsearchAnalysisConfigurationContext context) {
        MyCdiContext.checkAvailable(cdiContext);

        context.analyzer("index-level-analyzer-2").custom()
                .tokenizer("keyword")
                .tokenFilters("index-level-tokenfilter");

        context.tokenFilter("index-level-tokenfilter").type("pattern_replace")
                .param("pattern", ".+")
                .param("replacement", "token_inserted_by_index_analysis_2");
    }
}
