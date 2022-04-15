package io.quarkus.hibernate.search.elasticsearch.test.configuration;

import static io.quarkus.hibernate.search.elasticsearch.test.util.TransactionUtils.inTransaction;
import static org.assertj.core.api.Assertions.assertThat;

import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.transaction.UserTransaction;

import org.hibernate.Session;
import org.hibernate.search.backend.elasticsearch.ElasticsearchExtension;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.search.elasticsearch.test.util.TransactionUtils;
import io.quarkus.test.QuarkusUnitTest;

public class CustomMappingAndSettingsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(TransactionUtils.class)
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
            .overrideConfigKey("quarkus.hibernate-search-orm.elasticsearch.schema-management.mapping-file",
                    "custom-mapping.json")
            .overrideConfigKey("quarkus.hibernate-search-orm.elasticsearch.schema-management.settings-file",
                    "custom-settings.json");

    @Inject
    Session session;

    @Inject
    SearchSession searchSession;

    @Inject
    UserTransaction transaction;

    @Test
    @ActivateRequestContext
    public void test() {
        inTransaction(transaction, () -> session.persist(new IndexedEntity("foo bar")));

        inTransaction(transaction, () -> {
            // This can only return a hit if:
            // - the custom mapping was actually configured and used during index creation
            //   (so that field name.ngram exists).
            // - the NGram analyzer was actually configured and used during index creation
            //   (so that field name.ngram uses a ngram analyzer).
            assertThat(searchSession.search(IndexedEntity.class)
                    .extension(ElasticsearchExtension.get())
                    .where(f -> f.fromJson("{'match': { 'name.ngram': { 'query': 'fubar' } } }"))
                    .fetchHits(20))
                            .hasSize(1);
        });
    }
}
