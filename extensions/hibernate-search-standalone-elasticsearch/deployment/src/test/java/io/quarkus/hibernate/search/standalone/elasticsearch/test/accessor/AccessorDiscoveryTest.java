package io.quarkus.hibernate.search.standalone.elasticsearch.test.accessor;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.TypeBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.TypeBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.TypeBridgeWriteContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.TypeBinding;
import org.hibernate.search.mapper.pojo.model.PojoElementAccessor;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class AccessorDiscoveryTest {
    @RegisterExtension
    static final QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot(
                    jar -> jar.addClasses(Person.class, Address.class, PersonProjection.class, AddressProjection.class,
                            Details.class, DetailsBase.class, DetailsBinder.class, DetailsBridge.class)
                            .addAsResource("application.properties"))
            .overrideConfigKey("quarkus.hibernate-accessor.strategy", "generated");

    @Inject
    SearchMapping mapping;

    @Test
    @ActivateRequestContext
    void embeddedTypesAndProjectionConstructors() {
        try (var session = mapping.createSession()) {
            session.indexingPlan().add(new Person(1L, new Address("Paris")));
        }
        try (var session = mapping.createSession()) {
            assertThat(session.search(Person.class).select(PersonProjection.class)
                    .where(f -> f.bool()
                            .must(f.match().field("address.city").matching("Paris"))
                            .must(f.match().field("address.city_copy").matching("Paris"))
                            .must(f.match().field("details.code").matching("FR")))
                    .fetchHits(10))
                    .containsExactly(new PersonProjection(new AddressProjection("Paris")));
        }
    }

    @SearchEntity
    @Indexed
    public static class Person {
        @DocumentId
        private final long id;
        @IndexedEmbedded
        private final Address address;
        @IndexedEmbedded
        private final Details details = new Details();

        public Person(long id, Address address) {
            this.id = id;
            this.address = address;
        }
    }

    public static class Address {
        @KeywordField(projectable = Projectable.YES)
        @KeywordField(name = "city_copy")
        private final String city;

        public Address(String city) {
            this.city = city;
        }
    }

    public static class DetailsBase {
        public String getCode() {
            return "FR";
        }
    }

    @TypeBinding(binder = @TypeBinderRef(type = DetailsBinder.class))
    public static class Details extends DetailsBase {
    }

    public static class DetailsBinder implements TypeBinder {
        @Override
        public void bind(TypeBindingContext context) {
            PojoElementAccessor<String> code = context.bridgedElement().property("code").createAccessor(String.class);
            context.dependencies().use("code");
            IndexFieldReference<String> field = context.indexSchemaElement().field("code", f -> f.asString()).toReference();
            context.bridge(Object.class, new DetailsBridge(code, field));
        }
    }

    public record DetailsBridge(PojoElementAccessor<String> code,
            IndexFieldReference<String> field) implements TypeBridge<Object> {
        @Override
        public void write(DocumentElement target, Object bridgedElement, TypeBridgeWriteContext context) {
            target.addValue(field, code.read(bridgedElement));
        }
    }

    @ProjectionConstructor
    public record PersonProjection(AddressProjection address) {
    }

    public record AddressProjection(String city) {
        @ProjectionConstructor
        public AddressProjection {
        }
    }
}
