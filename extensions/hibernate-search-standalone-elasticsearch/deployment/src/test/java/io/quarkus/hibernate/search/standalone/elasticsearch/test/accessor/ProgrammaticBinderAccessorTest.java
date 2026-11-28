package io.quarkus.hibernate.search.standalone.elasticsearch.test.accessor;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;

import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;
import org.hibernate.search.mapper.pojo.model.PojoElementAccessor;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.mapping.StandalonePojoMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.standalone.mapping.StandalonePojoMappingConfigurer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.accessor.GeneratedFieldAccessor;
import io.quarkus.hibernate.accessor.GeneratedMethodAccessor;
import io.quarkus.hibernate.search.standalone.elasticsearch.SearchExtension;
import io.quarkus.test.QuarkusExtensionTest;

public class ProgrammaticBinderAccessorTest {
    @RegisterExtension
    static final QuarkusExtensionTest app = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(Author.class, AuthorMapping.class, NameBinder.class, Book.class)
                    .addAsResource("application.properties"))
            .overrideConfigKey("quarkus.hibernate-accessor.strategy", "generated");

    @Inject
    SearchMapping mapping;

    @Test
    @ActivateRequestContext
    void indexesNameThroughBinder() {
        try (var session = mapping.createSession()) {
            session.indexingPlan().add(new Author(1, "Ursula"));
        }
        try (var session = mapping.createSession()) {
            assertThat(session.search(Author.class).select(f -> f.id(Integer.class))
                    .where(f -> f.match().field("name").matching("Ursula")).fetchHits(10))
                    .containsExactly(1);
        }
    }

    @SearchExtension
    public static class AuthorMapping implements StandalonePojoMappingConfigurer {
        @Override
        public void configure(StandalonePojoMappingConfigurationContext context) {
            var author = context.programmaticMapping().type(Author.class);
            author.searchEntity();
            author.indexed();
            author.property("id").documentId();
            author.binder(new NameBinder());
        }
    }

    public static class NameBinder implements TypeBinder {
        @Override
        public void bind(TypeBindingContext context) {
            PojoElementAccessor<String> name = context.bridgedElement().property("name").createAccessor(String.class);
            context.dependencies().use("name");
            var field = context.indexSchemaElement().field("name", f -> f.asString()).toReference();
            context.bridge(Author.class, (target, author, bridgeContext) -> target.addValue(field, name.read(author)));
        }
    }

    @SearchEntity
    @Indexed
    public static class Book {
        @DocumentId
        private Integer id;
    }

    public static class Author {
        @GeneratedFieldAccessor
        private final int id;
        private final String name;

        public Author(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @GeneratedMethodAccessor
        public String getName() {
            return name;
        }
    }
}
