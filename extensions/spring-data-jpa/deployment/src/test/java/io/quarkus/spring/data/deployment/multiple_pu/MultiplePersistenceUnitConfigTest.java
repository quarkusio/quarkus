package io.quarkus.spring.data.deployment.multiple_pu;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Supplier;

import jakarta.inject.Inject;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.spring.data.deployment.multiple_pu.first.FirstEntity;
import io.quarkus.spring.data.deployment.multiple_pu.first.FirstEntityRepository;
import io.quarkus.spring.data.deployment.multiple_pu.second.SecondEntity;
import io.quarkus.spring.data.deployment.multiple_pu.second.SecondEntityRepository;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class MultiplePersistenceUnitConfigTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(FirstEntity.class, SecondEntity.class,
                            FirstEntityRepository.class, SecondEntityRepository.class,
                            PanacheTestResource.class)
                    .addAsResource("application-multiple-persistence-units.properties", "application.properties"));

    @Inject
    private FirstEntityRepository repository1;
    @Inject
    private SecondEntityRepository repository2;

    @BeforeEach
    void beforeEach() {
        repository1.deleteAll();
        repository2.deleteAll();
    }

    @Test
    public void panacheOperations() {
        /**
         * First entity operations
         */
        RestAssured.when().get("/persistence-unit/first/name-1").then().body(Matchers.is("1"));
        RestAssured.when().get("/persistence-unit/first/name-2").then().body(Matchers.is("2"));

        /**
         * second entity operations
         */
        RestAssured.when().get("/persistence-unit/second/name-1").then().body(Matchers.is("1"));
        RestAssured.when().get("/persistence-unit/second/name-2").then().body(Matchers.is("2"));
    }

    @Test
    public void entityLifecycle() {
        var detached = repository2.save(new SecondEntity());
        assertThat(detached.id).isNotNull();
        assertThat(inTx(repository2::count)).isEqualTo(1);

        detached.name = "name";
        repository2.save(detached);
        assertThat(inTx(repository2::count)).isEqualTo(1);

        inTx(() -> {
            var lazyRef = repository2.getOne(detached.id);
            assertThat(lazyRef.name).isEqualTo(detached.name);
            return null;
        });

        repository2.deleteByName("otherThan" + detached.name);
        assertThat(inTx(() -> repository2.findById(detached.id))).isPresent();

        repository2.deleteByName(detached.name);
        assertThat(inTx(() -> repository2.findById(detached.id))).isEmpty();
    }

    @Test
    void pagedQueries() {
        var newEntity = new SecondEntity();
        newEntity.name = "name";
        var detached = repository2.save(newEntity);

        Pageable pageable = PageRequest.of(0, 10, Sort.Direction.DESC, "id");

        var page = inTx(() -> repository2.findByName(detached.name, pageable));
        assertThat(page.getContent()).extracting(e -> e.id).containsExactly(detached.id);

        var pageIndexParam = inTx(() -> repository2.findByNameQueryIndexed(detached.name, pageable));
        assertThat(pageIndexParam.getContent()).extracting(e -> e.id).containsExactly(detached.id);

        var pageNamedParam = inTx(() -> repository2.findByNameQueryNamed(detached.name, pageable));
        assertThat(pageNamedParam.getContent()).extracting(e -> e.id).containsExactly(detached.id);
    }

    @Test
    void cascading() {
        var newParent = new SecondEntity();
        newParent.name = "parent";
        var newChild = new SecondEntity();
        newChild.name = "child";
        newParent.child = newChild;
        var detachedParent = repository2.save(newParent);

        assertThat(inTx(repository2::count)).isEqualTo(2);

        repository2.deleteByName(detachedParent.name);
        assertThat(inTx(repository2::count)).isZero();
    }

    private <T> T inTx(Supplier<T> action) {
        return QuarkusTransaction.requiringNew().call(action::get);
    }
}
