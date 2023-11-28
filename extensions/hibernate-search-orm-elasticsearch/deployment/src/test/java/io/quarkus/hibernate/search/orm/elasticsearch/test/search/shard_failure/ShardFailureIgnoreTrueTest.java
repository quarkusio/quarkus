package io.quarkus.hibernate.search.orm.elasticsearch.test.search.shard_failure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.inject.Inject;

import org.hibernate.search.mapper.orm.session.SearchSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.search.orm.elasticsearch.test.util.TransactionUtils;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.QuarkusUnitTest;

public class ShardFailureIgnoreTrueTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(TransactionUtils.class)
                    .addClass(MyEntity1.class)
                    .addClass(MyEntity2.class)
                    .addAsResource("hsearch-4915/index2.json"))
            .withConfigurationResource("application.properties")
            // Request that shard failures be ignored
            .overrideConfigKey("quarkus.hibernate-search-orm.elasticsearch.query.shard-failure.ignore", "true")
            // Override the type of the keyword field to integer, to create an error in one shard only.
            .overrideConfigKey(
                    "quarkus.hibernate-search-orm.elasticsearch.indexes.\"MyEntity2\".schema-management.mapping-file",
                    "hsearch-4915/index2.json");

    @Inject
    SearchSession session;

    @Test
    public void testShardFailureIgnored() {
        QuarkusTransaction.joiningExisting().run(() -> {
            session.toEntityManager().persist(new MyEntity1("42"));
            session.toEntityManager().persist(new MyEntity2("42"));
        });
        QuarkusTransaction.joiningExisting().run(() -> {
            assertThat(session.search(List.of(MyEntity1.class, MyEntity2.class))
                    .where(f -> f.wildcard().field("text").matching("4*"))
                    .fetchHits(20))
                    // MyEntity2 fails because "text" is an integer field there
                    // We expect that index (shard) to be ignored
                    .hasSize(1);
        });
    }
}
