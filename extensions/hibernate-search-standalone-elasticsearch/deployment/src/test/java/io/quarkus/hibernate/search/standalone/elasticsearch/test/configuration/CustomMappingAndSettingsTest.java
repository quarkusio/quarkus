package io.quarkus.hibernate.search.standalone.elasticsearch.test.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;

import org.hibernate.search.backend.elasticsearch.ElasticsearchExtension;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class CustomMappingAndSettingsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(IndexedEntity.class)
                    .addAsResource(new StringAsset("{\n" +
                            "  \"properties\": {\n" +
                            "    \"name\": {\n" +
                            "      \"type\": \"keyword\",\n" +
                            "      \"fields\": {\n" +
                            "        \"ngram\": {\n" +
                            "          \"type\": \"text\",\n" +
                            "          \"analyzer\": \"my_analyzer_ngram\"\n" +
                            "        }\n" +
                            "      }\n" +
                            "    }\n" +
                            "  }\n" +
                            "}"), "custom-mapping.json")
                    .addAsResource(new StringAsset("{\n" +
                            "  \"analysis\": {\n" +
                            "    \"analyzer\": {\n" +
                            "      \"my_analyzer_ngram\": {\n" +
                            "        \"type\": \"custom\",\n" +
                            "        \"tokenizer\": \"my_analyzer_ngram_tokenizer\"\n" +
                            "      }\n" +
                            "    },\n" +
                            "    \"tokenizer\": {\n" +
                            "      \"my_analyzer_ngram_tokenizer\": {\n" +
                            "        \"type\": \"ngram\",\n" +
                            "        \"min_gram\": \"2\",\n" +
                            "        \"max_gram\": \"3\"\n" +
                            "      }\n" +
                            "    }\n" +
                            "  }\n" +
                            "}"), "custom-settings.json"))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-search-standalone.elasticsearch.schema-management.mapping-file",
                    "custom-mapping.json")
            .overrideConfigKey("quarkus.hibernate-search-standalone.elasticsearch.schema-management.settings-file",
                    "custom-settings.json");

    @Inject
    SearchMapping searchMapping;

    @Test
    @ActivateRequestContext
    public void test() {
        try (var searchSession = searchMapping.createSession()) {
            searchSession.indexingPlan().add(new IndexedEntity(3L, "foo bar"));
        }

        try (var searchSession = searchMapping.createSession()) {
            // This can only return a hit if:
            // - the custom mapping was actually configured and used during index creation
            //   (so that field name.ngram exists).
            // - the NGram analyzer was actually configured and used during index creation
            //   (so that field name.ngram uses a ngram analyzer).
            assertThat(searchSession.search(IndexedEntity.class)
                    .extension(ElasticsearchExtension.get())
                    .selectEntityReference()
                    .where(f -> f.fromJson("{'match': { 'name.ngram': { 'query': 'fubar' } } }"))
                    .fetchHits(20))
                    .hasSize(1);
        }
    }
}
