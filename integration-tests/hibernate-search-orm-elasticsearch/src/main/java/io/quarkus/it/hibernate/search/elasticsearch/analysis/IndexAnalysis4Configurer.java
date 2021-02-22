package io.quarkus.it.hibernate.search.elasticsearch.analysis;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;

@ApplicationScoped
@Named("index-analysis-4")
public class IndexAnalysis4Configurer implements ElasticsearchAnalysisConfigurer {
    @Inject
    MyCdiContext cdiContext;

    @Override
    public void configure(ElasticsearchAnalysisConfigurationContext context) {
        MyCdiContext.checkAvailable(cdiContext);

        context.analyzer("index-level-analyzer-4").custom()
                .tokenizer("keyword")
                .tokenFilters("index-level-tokenfilter");

        context.tokenFilter("index-level-tokenfilter").type("pattern_replace")
                .param("pattern", ".+")
                .param("replacement", "token_inserted_by_index_analysis_4");
    }
}
