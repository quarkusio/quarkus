package io.quarkus.it.hibernate.search.elasticsearch.analysis;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;

@Dependent
@Named("index-analysis-2")
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
