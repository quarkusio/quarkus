package io.quarkus.hibernate.orm.envers;

import org.hibernate.envers.enhanced.SequenceIdRevisionEntity;
import org.hibernate.envers.enhanced.SequenceIdRevisionMapping;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

class SequenceIdRevisionEntityTest extends AbstractSequenceIdRevisionEntityTest {

    @RegisterExtension
    static final QuarkusExtensionTest runner = createRunner(false);

    @Override
    Class<? extends SequenceIdRevisionMapping> revisionEntityClass() {
        return SequenceIdRevisionEntity.class;
    }
}
