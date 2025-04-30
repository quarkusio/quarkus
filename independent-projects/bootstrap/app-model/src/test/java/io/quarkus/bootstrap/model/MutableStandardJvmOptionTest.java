package io.quarkus.bootstrap.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class MutableStandardJvmOptionTest {

    @Test
    public void testHasNoValue() {
        assertThat(MutableStandardJvmOption.newInstance("enable-preview").hasValue()).isFalse();
    }

    @Test
    public void testHasValue() {
        assertThat(MutableStandardJvmOption.newInstance("module-path", "value").hasValue()).isTrue();
    }

    @Test
    public void testCliArgumentEnablePreview() {
        assertThat(MutableStandardJvmOption.newInstance("enable-preview").toCliOptions()).containsExactly("--enable-preview");
    }

    @Test
    public void testCliArgumentIllegalAccess() {
        assertThat(MutableStandardJvmOption.newInstance("illegal-access", "warn").toCliOptions())
                .containsExactly("--illegal-access=warn");
    }

    @Test
    public void testCliArgumentAddModules() {
        assertThat(MutableStandardJvmOption.newInstance("add-modules")
                .addValue("java.compiler")
                .addValue("java.net")
                .toCliOptions())
                .containsExactly("--add-modules=java.compiler,java.net");
    }

    @Test
    public void testCliArgumentAddOpens() {
        assertThat(MutableStandardJvmOption.newInstance("add-opens")
                .addValue("java.base/java.util=ALL-UNNAMED")
                .addValue("java.base/java.io=org.acme.one,org.acme.two")
                .addValue("java.base/java.io=org.acme.three")
                .toCliOptions())
                .containsExactly("--add-opens", "java.base/java.io=org.acme.one,org.acme.three,org.acme.two",
                        "--add-opens", "java.base/java.util=ALL-UNNAMED");
    }

}
