package io.quarkus.hibernate.orm.envers;

import org.hibernate.envers.enhanced.SequenceIdRevisionMapping;
import org.hibernate.envers.enhanced.SequenceIdTrackingModifiedEntitiesRevisionEntity;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

class SequenceIdTrackingModifiedEntitiesRevisionEntityTest extends AbstractSequenceIdRevisionEntityTest {

    @RegisterExtension
    static final QuarkusExtensionTest runner = createRunner(true);

    @Override
    Class<? extends SequenceIdRevisionMapping> revisionEntityClass() {
        return SequenceIdTrackingModifiedEntitiesRevisionEntity.class;
    }
}
