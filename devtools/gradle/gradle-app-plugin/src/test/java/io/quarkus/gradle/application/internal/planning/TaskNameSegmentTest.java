package io.quarkus.gradle.application.internal.planning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TaskNameSegmentTest {

    @Test
    void normalizesBuildNamesToTaskSegments() {
        assertThat(TaskNameSegment.of("app").value()).isEqualTo("App");
        assertThat(TaskNameSegment.of("native1").value()).isEqualTo("Native1");
        assertThat(TaskNameSegment.of("native-main").value()).isEqualTo("NativeMain");
        assertThat(TaskNameSegment.of("native_main").value()).isEqualTo("NativeMain");
    }

    @Test
    void rejectsNamesThatCannotBeConvertedPredictably() {
        assertThatThrownBy(() -> TaskNameSegment.of(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TaskNameSegment.of("1native"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TaskNameSegment.of("native--main"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TaskNameSegment.of("native.main"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void collisionKeyIsCaseInsensitiveTaskSegment() {
        assertThat(TaskNameSegment.of("native-main").collisionKey()).isEqualTo("nativemain");
        assertThat(TaskNameSegment.of("nativeMain").collisionKey()).isEqualTo("nativemain");
    }
}
