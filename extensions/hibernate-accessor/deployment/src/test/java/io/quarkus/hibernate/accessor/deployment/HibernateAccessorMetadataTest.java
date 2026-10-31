package io.quarkus.hibernate.accessor.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.List;

import org.hibernate.accessor.asm.aot.AotFieldMetadata;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.Index;
import org.junit.jupiter.api.Test;

import io.quarkus.hibernate.accessor.deployment.HibernateAccessorRegistrationBuildItem.Builder;
import io.quarkus.hibernate.accessor.runtime.FieldAccess;
import io.quarkus.hibernate.accessor.runtime.ReflectionFreeAccessor;
import io.quarkus.hibernate.accessor.runtime.ReflectionFreeConstructorAccessor;
import io.quarkus.hibernate.accessor.runtime.ReflectionFreeFieldAccessor;
import io.quarkus.hibernate.accessor.runtime.ReflectionFreeMethodAccessor;

class HibernateAccessorMetadataTest {
    @Test
    void defaultsSelectFieldReadsMethodsAndConstructors() throws IOException {
        var item = collect(Defaults.class);
        assertThat(item.getFieldReaders()).extracting(AotFieldMetadata::name).containsExactly("value");
        assertThat(item.getFieldWriters()).isEmpty();
        assertThat(item.getGetters()).extracting(m -> m.name()).containsExactly("getValue");
        assertThat(item.getSetters()).extracting(m -> m.name()).containsExactly("setValue");
        assertThat(item.getConstructors()).hasSize(1);
    }

    @Test
    void memberAnnotationsOverrideCategorySettings() throws IOException {
        var item = collect(Overrides.class);
        assertThat(item.getFieldReaders()).extracting(AotFieldMetadata::name).containsExactly("read");
        assertThat(item.getFieldWriters()).extracting(AotFieldMetadata::name).containsExactly("write");
        assertThat(item.getGetters()).extracting(m -> m.name()).containsExactly("getValue");
        assertThat(item.getSetters()).isEmpty();
        assertThat(item.getConstructors()).hasSize(1);
    }

    @Test
    void memberCanDisableTypeRegistration() throws IOException {
        var item = collect(DisabledMembers.class);
        assertThat(item.getFieldReaders()).isEmpty();
        assertThat(item.getFieldWriters()).isEmpty();
        assertThat(item.getGetters()).isEmpty();
        assertThat(item.getConstructors()).isEmpty();
    }

    @Test
    void explicitRecordWriteFailsWithProvenance() throws IOException {
        ClassInfo record = Index.of(RecordModel.class).getClassByName(RecordModel.class);
        assertThatThrownBy(() -> new Builder(record).source("ORM test")
                .addFieldWriter(record.field("value")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(RecordModel.class.getName() + "#value")
                .hasMessageContaining("ORM test");
    }

    @Test
    void annotatedRecordWriteFails() {
        assertThatThrownBy(() -> collect(WritableRecord.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("record field")
                .hasMessageContaining(ReflectionFreeAccessor.class.getName());
    }

    @Test
    void recordSupportsReadersAndConstructors() throws IOException {
        var item = collect(RecordModel.class);
        assertThat(item.getFieldReaders()).hasSize(1);
        assertThat(item.getFieldWriters()).isEmpty();
        assertThat(item.getConstructors()).hasSize(1);
    }

    @Test
    void staticFieldRequestFails() throws IOException {
        ClassInfo type = Index.of(Defaults.class).getClassByName(Defaults.class);
        assertThatThrownBy(() -> new Builder(type).source("test").addFieldReader(type.field("staticValue")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("static field");
    }

    @Test
    void registrationsMergeIndependentlyOfOrderAndDuplicates() throws IOException {
        ClassInfo type = Index.of(Defaults.class).getClassByName(Defaults.class);
        ClassInfo other = Index.of(RecordModel.class).getClassByName(RecordModel.class);
        var read = new Builder(type).source("Validator").addFieldReader(type.field("value")).build();
        var write = new Builder(type).source("ORM").addFieldWriter(type.field("value")).build();
        var second = new Builder(other).addFieldReader(other.field("value")).build();
        var forward = HibernateAccessorProcessor.mergeRegistrations(List.of(read, second, write, read));
        var reverse = HibernateAccessorProcessor.mergeRegistrations(List.of(write, read, second));
        assertThat(forward).isEqualTo(reverse);
        assertThat(forward).hasSize(2);
        var merged = forward.stream().filter(h -> h.type().name().equals(type.name().toString())).findFirst().orElseThrow();
        assertThat(merged.readers()).hasSize(1);
        assertThat(merged.writers()).containsExactlyElementsOf(merged.readers());
        assertThat(merged.readers().iterator().next()).isSameAs(read.getFieldReaders().iterator().next());
    }

    @Test
    void disabledAnnotationDoesNotCancelExtensionRequest() throws IOException {
        ClassInfo type = Index.of(DisabledMembers.class).getClassByName(DisabledMembers.class);
        var extension = new Builder(type).source("ORM").addFieldWriter(type.field("value")).build();
        var merged = HibernateAccessorProcessor.mergeRegistrations(List.of(collect(DisabledMembers.class), extension));
        assertThat(merged.getFirst().writers()).hasSize(1);
    }

    private static HibernateAccessorRegistrationBuildItem collect(Class<?> type) throws IOException {
        return HibernateAccessorAnnotationCollector.collect(Index.of(type)).iterator().next();
    }

    @ReflectionFreeAccessor
    static class Defaults {
        static String staticValue;
        final String value = new String("original");

        String getValue() {
            return value;
        }

        void setValue(String value) {
        }
    }

    @ReflectionFreeAccessor(fields = @ReflectionFreeFieldAccessor(enabled = false), methods = @ReflectionFreeMethodAccessor(enabled = false), constructors = @ReflectionFreeConstructorAccessor(enabled = false))
    static class Overrides {
        @ReflectionFreeFieldAccessor
        String read;
        @ReflectionFreeFieldAccessor(access = FieldAccess.WRITE)
        final String write;
        String ignored;

        @ReflectionFreeConstructorAccessor
        Overrides() {
            write = "original";
        }

        @ReflectionFreeMethodAccessor
        String getValue() {
            return read;
        }

        void setValue(String value) {
        }
    }

    @ReflectionFreeAccessor(fields = @ReflectionFreeFieldAccessor(access = FieldAccess.READ_WRITE))
    static class DisabledMembers {
        @ReflectionFreeFieldAccessor(enabled = false)
        String value;

        @ReflectionFreeConstructorAccessor(enabled = false)
        DisabledMembers() {
        }

        @ReflectionFreeMethodAccessor(enabled = false)
        String getValue() {
            return value;
        }
    }

    @ReflectionFreeAccessor
    record RecordModel(String value) {
    }

    @ReflectionFreeAccessor(fields = @ReflectionFreeFieldAccessor(access = FieldAccess.WRITE))
    record WritableRecord(String value) {
    }
}
