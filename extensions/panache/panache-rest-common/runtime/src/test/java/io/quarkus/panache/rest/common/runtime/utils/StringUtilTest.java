package io.quarkus.panache.rest.common.runtime.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StringUtilTest {

    @Test
    void shouldIgnoreNull() {
        assertThat(StringUtil.camelToHyphenated(null)).isNull();
        assertThat(StringUtil.toPlural(null)).isNull();
    }

    @Test
    void shouldHandleEmptyString() {
        assertThat(StringUtil.camelToHyphenated("")).isEqualTo("");
    }

    @Test
    void shouldConvertPerson() {
        assertThat(StringUtil.camelToHyphenated("Person")).isEqualTo("person");
    }

    @Test
    void shouldConvertUserAccount() {
        assertThat(StringUtil.camelToHyphenated("UserAccount")).isEqualTo("user-account");
    }

    @Test
    void shouldPluralizePerson() {
        assertThat(StringUtil.toPlural("person")).isEqualTo("people");
    }

    @Test
    void shouldPluralizeCriterion() {
        assertThat(StringUtil.toPlural("criterion")).isEqualTo("criteria");
    }

    @Test
    void shouldPluralizeAnalysis() {
        assertThat(StringUtil.toPlural("analysis")).isEqualTo("analyses");
    }

    @Test
    void shouldPluralizeCity() {
        assertThat(StringUtil.toPlural("city")).isEqualTo("cities");
    }

    @Test
    void shouldPluralizeWife() {
        assertThat(StringUtil.toPlural("wife")).isEqualTo("wives");
    }

    @Test
    void shouldPluralizeWolf() {
        assertThat(StringUtil.toPlural("wolf")).isEqualTo("wolves");
    }

    @Test
    void shouldPluralizeBus() {
        assertThat(StringUtil.toPlural("bus")).isEqualTo("buses");
    }

    @Test
    void shouldPluralizeCat() {
        assertThat(StringUtil.toPlural("cat")).isEqualTo("cats");
    }
}
