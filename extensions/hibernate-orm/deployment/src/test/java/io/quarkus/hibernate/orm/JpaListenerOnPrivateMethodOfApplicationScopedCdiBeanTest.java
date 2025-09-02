package io.quarkus.hibernate.orm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PostPersist;
import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class JpaListenerOnPrivateMethodOfApplicationScopedCdiBeanTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application.properties"))
            .assertException(e -> {
                assertThat(e).isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("SomeEntityListener#postPersist");
            });

    @Test
    @Transactional
    public void test() {
        fail("should never be called");
    }

    @Entity
    @EntityListeners(SomeEntityListener.class)
    public static class SomeEntity {
        private long id;
        private String name;

        public SomeEntity() {
        }

        public SomeEntity(String name) {
            this.name = name;
        }

        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "myEntitySeq")
        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "SomeEntity:" + name;
        }
    }

    @ApplicationScoped
    public static class SomeEntityListener {

        @PostPersist
        private void postPersist(SomeEntity someEntity) {
            fail("should not reach here");
        }
    }
}
