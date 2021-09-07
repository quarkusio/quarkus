package io.quarkus.jdbc.oracle.deployment;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import io.smallrye.common.constraint.Assert;

/**
 * The metadata override facility of GraalVM's native-image
 * works with regular expressions.
 * We're testing our expressions here to match against the
 * constants the compiler is expecting (inferred by debugging
 * the compiler) as it's otherwise a bit tricky to assert
 * if they have been applied.
 */
public class RegexMatchTest {

    @Test
    public void jarRegexIsMatching() {
        final String EXAMPLE_CLASSPATH = "/home/sanne/sources/quarkus/integration-tests/jpa-oracle/target/quarkus-integration-test-jpa-oracle-999-SNAPSHOT-native-image-source-jar/lib/com.oracle.database.jdbc.ojdbc11-21.3.0.0.jar";
        final Pattern pattern = Pattern.compile(OracleMetadataOverrides.DRIVER_JAR_MATCH_REGEX);
        final Matcher matcher = pattern.matcher(EXAMPLE_CLASSPATH);
        Assert.assertTrue(matcher.find());
    }

    @Test
    public void resourceRegexIsMatching() {
        //We need to exclude both of these:
        final String RES1 = "/META-INF/native-image/native-image.properties";
        final String RES2 = "/META-INF/native-image/reflect-config.json";
        final Pattern pattern = Pattern.compile(OracleMetadataOverrides.NATIVE_IMAGE_RESOURCE_MATCH_REGEX);

        Assert.assertTrue(pattern.matcher(RES1).find());
        Assert.assertTrue(pattern.matcher(RES2).find());

        //While this one should NOT be ignored:
        final String RES3 = "/META-INF/native-image/resource-config.json";
        final String RES4 = "/META-INF/native-image/jni-config.json";
        Assert.assertFalse(pattern.matcher(RES3).find());
        Assert.assertFalse(pattern.matcher(RES4).find());
    }

}
