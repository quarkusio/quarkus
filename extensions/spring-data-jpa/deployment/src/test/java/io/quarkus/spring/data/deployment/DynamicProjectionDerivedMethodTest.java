package io.quarkus.spring.data.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.transaction.Transactional;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;

import io.quarkus.hibernate.orm.panache.common.ProjectedFieldName;
import io.quarkus.test.QuarkusExtensionTest;

class DynamicProjectionDerivedMethodTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Parent.class, ParentNameOnly.class, ParentIdOnly.class, ParentRepository.class))
            .withConfigurationResource("application.properties");

    @Autowired
    ParentRepository repository;

    @Test
    @Transactional
    void dynamicProjectionToEntityReturnsEntity() {
        Parent p = new Parent();
        p.setName("parent-1");
        p = repository.save(p);

        Parent projected = repository.findById(p.getId(), Parent.class);
        assertThat(projected).isNotNull();
        assertThat(projected.getName()).isEqualTo("parent-1");
    }

    @Test
    @Transactional
    void dynamicProjectionToDtoReturnsProjectedType() {
        Parent p = new Parent();
        p.setName("parent-2");
        p = repository.save(p);

        ParentNameOnly projected = repository.findById(p.getId(), ParentNameOnly.class);
        assertThat(projected).isNotNull();
        assertThat(projected).isNotInstanceOf(Parent.class);
        assertThat(projected.getName()).isEqualTo("parent-2");
    }

    @Test
    @Transactional
    void dynamicProjectionToAnotherDtoReturnsProjectedType() {
        Parent p = new Parent();
        p.setName("parent-3");
        p = repository.save(p);

        ParentIdOnly projected = repository.findById(p.getId(), ParentIdOnly.class);
        assertThat(projected).isNotNull();
        assertThat(projected).isNotInstanceOf(Parent.class);
        assertThat(projected.getId()).isEqualTo(p.getId());
    }

    @Entity(name = "Parent")
    public static class Parent {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
        private String name;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    // DTO projection target — Panache constructor-based projection
    public static class ParentNameOnly {
        private final String name;

        public ParentNameOnly(@ProjectedFieldName("name") String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public static class ParentIdOnly {
        private final Long id;

        public ParentIdOnly(@ProjectedFieldName("id") Long id) {
            this.id = id;
        }

        public Long getId() {
            return id;
        }
    }

    public interface ParentRepository extends JpaRepository<Parent, Long> {
        <T> T findById(Long id, Class<T> type);
    }
}
