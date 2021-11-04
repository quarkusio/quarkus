package io.quarkus.hibernate.orm.metadatabuildercontributor;

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class MetadataBuilderContributorTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class)
                    .addClass(CustomMetadataBuilderContributor.class)
                    .addAsResource("application-metadata-builder-contributor.properties", "application.properties"));

    @Inject
    EntityManager entityManager;

    @Test
    @Transactional
    public void test() {
        MyEntity entity = new MyEntity();
        entity.setName("some_name");
        entityManager.persist(entity);

        assertThat(entityManager.createQuery("select addHardcodedSuffix(e.name) from MyEntity e", String.class)
                .getSingleResult())
                        .isEqualTo("some_name_some_suffix");
    }

}
