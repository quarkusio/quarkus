package io.quarkus.it.hibernate.search.elasticsearch.analysis;

import javax.inject.Inject;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;

public class IndexAnalysis1Configurer implements ElasticsearchAnalysisConfigurer {
    // This will always be null.
    // It's only here to check that this class is instantiated without relying on CDI
    // (because we want to test non-CDI configurers too).
    @Inject
    MyCdiContext cdiContext;

    @Override
    public void configure(ElasticsearchAnalysisConfigurationContext context) {
        MyCdiContext.checkNotAvailable(cdiContext);

        context.analyzer("index-level-analyzer-1").custom()
                .tokenizer("keyword")
                .tokenFilters("index-level-tokenfilter");

        context.tokenFilter("index-level-tokenfilter").type("pattern_replace")
                .param("pattern", ".+")
                .param("replacement", "token_inserted_by_index_analysis_1");
    }
}
