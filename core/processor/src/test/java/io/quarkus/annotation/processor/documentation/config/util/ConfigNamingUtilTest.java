package io.quarkus.annotation.processor.documentation.config.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.quarkus.annotation.processor.documentation.config.model.ConfigPhase;

public class ConfigNamingUtilTest {

    @Test
    public void replaceNonAlphanumericByUnderscoresThenConvertToUpperCase() {
        assertEquals("QUARKUS_DATASOURCE__DATASOURCE_NAME__JDBC_BACKGROUND_VALIDATION_INTERVAL",
                ConfigNamingUtil.toEnvVarName("quarkus.datasource.\"datasource-name\".jdbc.background-validation-interval"));
        assertEquals(
                "QUARKUS_SECURITY_JDBC_PRINCIPAL_QUERY__NAMED_PRINCIPAL_QUERIES__BCRYPT_PASSWORD_MAPPER_ITERATION_COUNT_INDEX",
                ConfigNamingUtil.toEnvVarName(
                        "quarkus.security.jdbc.principal-query.\"named-principal-queries\".bcrypt-password-mapper.iteration-count-index"));
    }

    @Test
    void getRootPrefixTest() {
        String prefix = "quarkus";
        String name = Markers.HYPHENATED_ELEMENT_NAME;
        String simpleClassName = "MyConfig";
        String actual = ConfigNamingUtil.getRootPrefix(prefix, name, simpleClassName, ConfigPhase.RUN_TIME);
        assertEquals("quarkus.my", actual);

        prefix = "my.prefix";
        name = "";
        simpleClassName = "MyPrefix";
        actual = ConfigNamingUtil.getRootPrefix(prefix, name, simpleClassName, ConfigPhase.RUN_TIME);
        assertEquals("my.prefix", actual);

        prefix = "";
        name = "my.prefix";
        simpleClassName = "MyPrefix";
        actual = ConfigNamingUtil.getRootPrefix(prefix, name, simpleClassName, ConfigPhase.RUN_TIME);
        assertEquals("my.prefix", actual);

        prefix = "my";
        name = "prefix";
        simpleClassName = "MyPrefix";
        actual = ConfigNamingUtil.getRootPrefix(prefix, name, simpleClassName, ConfigPhase.RUN_TIME);
        assertEquals("my.prefix", actual);

        prefix = "quarkus";
        name = "prefix";
        simpleClassName = "SomethingElse";
        actual = ConfigNamingUtil.getRootPrefix(prefix, name, simpleClassName, ConfigPhase.RUN_TIME);
        assertEquals("quarkus.prefix", actual);

        prefix = "";
        name = Markers.HYPHENATED_ELEMENT_NAME;
        simpleClassName = "SomethingElse";
        actual = ConfigNamingUtil.getRootPrefix(prefix, name, simpleClassName, ConfigPhase.RUN_TIME);
        assertEquals("quarkus.something-else", actual);
    }

    @Test
    public void derivingConfigRootNameTestCase() {
        // should hyphenate class name
        String simpleClassName = "RootName";
        String actual = ConfigNamingUtil.deriveConfigRootName(simpleClassName, "", ConfigPhase.RUN_TIME);
        assertEquals("quarkus.root-name", actual);

        // should hyphenate class name after removing Config(uration) suffix
        simpleClassName = "RootNameConfig";
        actual = ConfigNamingUtil.deriveConfigRootName(simpleClassName, "", ConfigPhase.BUILD_TIME);
        assertEquals("quarkus.root-name", actual);

        simpleClassName = "RootNameConfiguration";
        actual = ConfigNamingUtil.deriveConfigRootName(simpleClassName, "", ConfigPhase.BUILD_AND_RUN_TIME_FIXED);
        assertEquals("quarkus.root-name", actual);

        // should hyphenate class name after removing RunTimeConfig(uration) suffix
        simpleClassName = "RootNameRunTimeConfig";
        actual = ConfigNamingUtil.deriveConfigRootName(simpleClassName, "", ConfigPhase.RUN_TIME);
        assertEquals("quarkus.root-name", actual);

        simpleClassName = "RootNameRuntimeConfig";
        actual = ConfigNamingUtil.deriveConfigRootName(simpleClassName, "", ConfigPhase.RUN_TIME);
        assertEquals("quarkus.root-name", actual);

        simpleClassName = "RootNameRunTimeConfiguration";
        actual = ConfigNamingUtil.deriveConfigRootName(simpleClassName, "", ConfigPhase.RUN_TIME);
        assertEquals("quarkus.root-name", actual);

        // should hyphenate class name after removing BuildTimeConfig(uration) suffix
        simpleClassName = "RootNameBuildTimeConfig";
        actual = ConfigNamingUtil.deriveConfigRootName(simpleClassName, "", ConfigPhase.BUILD_AND_RUN_TIME_FIXED);
        assertEquals("quarkus.root-name", actual);

        simpleClassName = "RootNameBuildTimeConfiguration";
        actual = ConfigNamingUtil.deriveConfigRootName(simpleClassName, "", ConfigPhase.BUILD_TIME);
        assertEquals("quarkus.root-name", actual);

        simpleClassName = "RootName";
        actual = ConfigNamingUtil.deriveConfigRootName(simpleClassName, "prefix", ConfigPhase.RUN_TIME);
        assertEquals("prefix.root-name", actual);

        simpleClassName = "RootName";
        actual = ConfigNamingUtil.deriveConfigRootName(simpleClassName, "my.prefix", ConfigPhase.RUN_TIME);
        assertEquals("my.prefix.root-name", actual);
    }
}
