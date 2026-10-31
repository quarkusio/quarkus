package io.quarkus.hibernate.accessor.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hibernate.accessor.AccessorFactory;
import org.hibernate.accessor.ValueReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class HibernateAccessorRecordTest extends HibernateAccessorBaseTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(RecordEntity.class));

    @Test
    void readStringFieldViaFieldAccessor() throws Exception {
        ValueReader<?> reader = reader(RecordEntity.class, "name");

        RecordEntity entity = new RecordEntity("hello", 42);
        assertThat(reader.get(entity)).isEqualTo("hello");
    }

    @Test
    void readIntFieldViaFieldAccessor() throws Exception {
        ValueReader<?> reader = reader(RecordEntity.class, "age");

        RecordEntity entity = new RecordEntity("test", 25);
        assertThat(reader.get(entity)).isEqualTo(25);
    }

    @Test
    void readStringViaMethodAccessor() throws Exception {
        ValueReader<?> reader = readerMethod(RecordEntity.class, "name");

        RecordEntity entity = new RecordEntity("method-read", 0);
        assertThat(reader.get(entity)).isEqualTo("method-read");
    }

    @Test
    void readIntViaMethodAccessor() throws Exception {
        ValueReader<?> reader = readerMethod(RecordEntity.class, "age");

        RecordEntity entity = new RecordEntity("test", 99);
        assertThat(reader.get(entity)).isEqualTo(99);
    }

    @Test
    void fieldWriterThrowsForRecord() throws Exception {
        AccessorFactory factory = loadGeneratedFactory();

        assertThatThrownBy(() -> factory.valueWriter(RecordEntity.class.getDeclaredField("name")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

}
