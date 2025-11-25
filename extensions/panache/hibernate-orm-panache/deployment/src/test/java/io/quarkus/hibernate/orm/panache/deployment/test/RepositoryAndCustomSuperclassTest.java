package io.quarkus.hibernate.orm.panache.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.TestTransaction;

public class RepositoryAndCustomSuperclassTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyCustomEntitySuperclass.class, MyConcreteEntity.class, MyRepository.class));

    @Inject
    MyRepository repository;

    @Test
    @TestTransaction
    public void smokeTest() {
        assertThat(repository.count()).isEqualTo(0);
        repository.persist(new MyConcreteEntity());
        assertThat(repository.count()).isEqualTo(1);
    }

    @Entity(name = "Concrete")
    public static class MyConcreteEntity extends MyCustomEntitySuperclass {

        @Column
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

    }

    @Entity(name = "Super")
    public abstract static class MyCustomEntitySuperclass {

        @Id
        @GeneratedValue
        private Integer id;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

    }

    @ApplicationScoped
    public static class MyRepository implements PanacheRepositoryBase<MyCustomEntitySuperclass, Integer> {
    }
}
