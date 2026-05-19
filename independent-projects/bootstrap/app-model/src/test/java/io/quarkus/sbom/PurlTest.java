package io.quarkus.sbom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.entry;

import org.junit.jupiter.api.Test;

class PurlTest {

    @Test
    void mavenPurlDefaultType() {
        Purl purl = Purl.maven("io.quarkus", "quarkus-rest", "3.0.0");
        assertThat(purl.toString()).isEqualTo("pkg:maven/io.quarkus/quarkus-rest@3.0.0?type=jar");
        assertThat(purl.getType()).isEqualTo("maven");
        assertThat(purl.getNamespace()).isEqualTo("io.quarkus");
        assertThat(purl.getName()).isEqualTo("quarkus-rest");
        assertThat(purl.getVersion()).isEqualTo("3.0.0");
        assertThat(purl.getQualifiers()).containsOnly(entry("type", "jar"));
        assertThat(purl.getSubpath()).isNull();
    }

    @Test
    void mavenPurlWithClassifier() {
        Purl purl = Purl.maven("org.acme", "acme-app", "1.0-SNAPSHOT", "jar", "runner");
        assertThat(purl.toString()).isEqualTo("pkg:maven/org.acme/acme-app@1.0-SNAPSHOT?classifier=runner&type=jar");
        assertThat(purl.getType()).isEqualTo("maven");
        assertThat(purl.getNamespace()).isEqualTo("org.acme");
        assertThat(purl.getName()).isEqualTo("acme-app");
        assertThat(purl.getVersion()).isEqualTo("1.0-SNAPSHOT");
        assertThat(purl.getQualifiers()).containsOnly(entry("classifier", "runner"), entry("type", "jar"));
        assertThat(purl.getSubpath()).isNull();
    }

    @Test
    void mavenPurlWithoutClassifier() {
        Purl purl = Purl.maven("org.acme", "acme-app", "1.0-SNAPSHOT", "jar", null);
        assertThat(purl.toString()).isEqualTo("pkg:maven/org.acme/acme-app@1.0-SNAPSHOT?type=jar");
        assertThat(purl.getType()).isEqualTo("maven");
        assertThat(purl.getNamespace()).isEqualTo("org.acme");
        assertThat(purl.getName()).isEqualTo("acme-app");
        assertThat(purl.getVersion()).isEqualTo("1.0-SNAPSHOT");
        assertThat(purl.getQualifiers()).containsOnly(entry("type", "jar"));
        assertThat(purl.getSubpath()).isNull();
    }

    @Test
    void npmPurlWithScope() {
        Purl purl = Purl.npm("@babel", "core", "7.20.0");
        assertThat(purl.toString()).isEqualTo("pkg:npm/%40babel/core@7.20.0");
        assertThat(purl.getType()).isEqualTo("npm");
        assertThat(purl.getNamespace()).isEqualTo("@babel");
        assertThat(purl.getName()).isEqualTo("core");
        assertThat(purl.getVersion()).isEqualTo("7.20.0");
        assertThat(purl.getQualifiers()).isEmpty();
        assertThat(purl.getSubpath()).isNull();
    }

    @Test
    void npmPurlWithoutScope() {
        Purl purl = Purl.npm(null, "lodash", "4.17.21");
        assertThat(purl.toString()).isEqualTo("pkg:npm/lodash@4.17.21");
        assertThat(purl.getType()).isEqualTo("npm");
        assertThat(purl.getNamespace()).isNull();
        assertThat(purl.getName()).isEqualTo("lodash");
        assertThat(purl.getVersion()).isEqualTo("4.17.21");
        assertThat(purl.getQualifiers()).isEmpty();
        assertThat(purl.getSubpath()).isNull();
    }

    @Test
    void genericPurl() {
        Purl purl = Purl.generic("quarkus-run.jar", "1.0-SNAPSHOT");
        assertThat(purl.toString()).isEqualTo("pkg:generic/quarkus-run.jar@1.0-SNAPSHOT");
        assertThat(purl.getType()).isEqualTo("generic");
        assertThat(purl.getNamespace()).isNull();
        assertThat(purl.getName()).isEqualTo("quarkus-run.jar");
        assertThat(purl.getVersion()).isEqualTo("1.0-SNAPSHOT");
        assertThat(purl.getQualifiers()).isEmpty();
        assertThat(purl.getSubpath()).isNull();
    }

    @Test
    void genericPurlNullVersion() {
        Purl purl = Purl.generic("some-file.txt", null);
        assertThat(purl.toString()).isEqualTo("pkg:generic/some-file.txt");
        assertThat(purl.getType()).isEqualTo("generic");
        assertThat(purl.getNamespace()).isNull();
        assertThat(purl.getName()).isEqualTo("some-file.txt");
        assertThat(purl.getVersion()).isNull();
        assertThat(purl.getQualifiers()).isEmpty();
        assertThat(purl.getSubpath()).isNull();
    }

    @Test
    void builderMinimal() {
        Purl purl = Purl.builder().setType("pypi").setName("requests").build();
        assertThat(purl.toString()).isEqualTo("pkg:pypi/requests");
        assertThat(purl.getType()).isEqualTo("pypi");
        assertThat(purl.getNamespace()).isNull();
        assertThat(purl.getName()).isEqualTo("requests");
        assertThat(purl.getVersion()).isNull();
        assertThat(purl.getQualifiers()).isEmpty();
        assertThat(purl.getSubpath()).isNull();
    }

    @Test
    void builderWithAllFields() {
        Purl purl = Purl.builder()
                .setType("maven")
                .setNamespace("io.quarkus")
                .setName("quarkus-core")
                .setVersion("3.0.0")
                .addQualifier("type", "jar")
                .setSubpath("src/main")
                .build();
        assertThat(purl.toString()).isEqualTo("pkg:maven/io.quarkus/quarkus-core@3.0.0?type=jar#src/main");
        assertThat(purl.getType()).isEqualTo("maven");
        assertThat(purl.getNamespace()).isEqualTo("io.quarkus");
        assertThat(purl.getName()).isEqualTo("quarkus-core");
        assertThat(purl.getVersion()).isEqualTo("3.0.0");
        assertThat(purl.getQualifiers()).containsOnly(entry("type", "jar"));
        assertThat(purl.getSubpath()).isEqualTo("src/main");
    }

    @Test
    void nullTypeThrows() {
        assertThatNullPointerException()
                .isThrownBy(() -> Purl.builder().setName("foo").build());
    }

    @Test
    void nullNameThrows() {
        assertThatNullPointerException()
                .isThrownBy(() -> Purl.builder().setType("npm").build());
    }

    @Test
    void qualifiersSortedAlphabetically() {
        Purl purl = Purl.maven("org.acme", "acme", "1.0", "jar", "sources");
        String s = purl.toString();
        assertThat(s.indexOf("classifier=")).isLessThan(s.indexOf("type="));
    }

    @Test
    void parseRoundTripMaven() {
        Purl original = Purl.maven("org.acme", "acme-app", "1.0-SNAPSHOT", "jar", "runner");
        Purl parsed = Purl.parse(original.toString());
        assertThat(parsed).isEqualTo(original);
        assertThat(parsed.toString()).isEqualTo(original.toString());
        assertThat(parsed.getType()).isEqualTo("maven");
        assertThat(parsed.getNamespace()).isEqualTo("org.acme");
        assertThat(parsed.getName()).isEqualTo("acme-app");
        assertThat(parsed.getVersion()).isEqualTo("1.0-SNAPSHOT");
        assertThat(parsed.getQualifiers()).containsOnly(entry("classifier", "runner"), entry("type", "jar"));
        assertThat(parsed.getSubpath()).isNull();
    }

    @Test
    void parseRoundTripNpmScoped() {
        Purl original = Purl.npm("@babel", "core", "7.20.0");
        Purl parsed = Purl.parse(original.toString());
        assertThat(parsed).isEqualTo(original);
        assertThat(parsed.getType()).isEqualTo("npm");
        assertThat(parsed.getNamespace()).isEqualTo("@babel");
        assertThat(parsed.getName()).isEqualTo("core");
        assertThat(parsed.getVersion()).isEqualTo("7.20.0");
        assertThat(parsed.getQualifiers()).isEmpty();
        assertThat(parsed.getSubpath()).isNull();
    }

    @Test
    void parseRoundTripNpmUnscoped() {
        Purl original = Purl.npm(null, "lodash", "4.17.21");
        Purl parsed = Purl.parse(original.toString());
        assertThat(parsed).isEqualTo(original);
        assertThat(parsed.getType()).isEqualTo("npm");
        assertThat(parsed.getNamespace()).isNull();
        assertThat(parsed.getName()).isEqualTo("lodash");
        assertThat(parsed.getVersion()).isEqualTo("4.17.21");
        assertThat(parsed.getQualifiers()).isEmpty();
        assertThat(parsed.getSubpath()).isNull();
    }

    @Test
    void parseRoundTripGeneric() {
        Purl original = Purl.generic("quarkus-run.jar", "1.0-SNAPSHOT");
        Purl parsed = Purl.parse(original.toString());
        assertThat(parsed).isEqualTo(original);
        assertThat(parsed.getType()).isEqualTo("generic");
        assertThat(parsed.getNamespace()).isNull();
        assertThat(parsed.getName()).isEqualTo("quarkus-run.jar");
        assertThat(parsed.getVersion()).isEqualTo("1.0-SNAPSHOT");
        assertThat(parsed.getQualifiers()).isEmpty();
        assertThat(parsed.getSubpath()).isNull();
    }

    @Test
    void parseInvalidNoPkgPrefix() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Purl.parse("maven/io.quarkus/foo@1.0"));
    }

    @Test
    void parseNull() {
        assertThatNullPointerException()
                .isThrownBy(() -> Purl.parse(null));
    }

    @Test
    void equalsAndHashCode() {
        Purl a = Purl.maven("io.quarkus", "quarkus-rest", "3.0.0");
        Purl b = Purl.maven("io.quarkus", "quarkus-rest", "3.0.0");
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void notEqual() {
        Purl a = Purl.maven("io.quarkus", "quarkus-rest", "3.0.0");
        Purl b = Purl.maven("io.quarkus", "quarkus-rest", "3.1.0");
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void ofFactory() {
        Purl purl = Purl.of("cargo", null, "serde", "1.0.0");
        assertThat(purl.toString()).isEqualTo("pkg:cargo/serde@1.0.0");
        assertThat(purl.getType()).isEqualTo("cargo");
        assertThat(purl.getNamespace()).isNull();
        assertThat(purl.getName()).isEqualTo("serde");
        assertThat(purl.getVersion()).isEqualTo("1.0.0");
        assertThat(purl.getQualifiers()).isEmpty();
        assertThat(purl.getSubpath()).isNull();
    }

    @Test
    void emptyNamespaceIsNull() {
        Purl purl = Purl.of("npm", "", "lodash", "4.0");
        assertThat(purl.getType()).isEqualTo("npm");
        assertThat(purl.getNamespace()).isNull();
        assertThat(purl.getName()).isEqualTo("lodash");
        assertThat(purl.getVersion()).isEqualTo("4.0");
        assertThat(purl.getQualifiers()).isEmpty();
        assertThat(purl.getSubpath()).isNull();
    }

    @Test
    void emptyClassifierNotIncluded() {
        Purl purl = Purl.maven("org.acme", "acme", "1.0", "jar", "");
        assertThat(purl.toString()).isEqualTo("pkg:maven/org.acme/acme@1.0?type=jar");
        assertThat(purl.getType()).isEqualTo("maven");
        assertThat(purl.getNamespace()).isEqualTo("org.acme");
        assertThat(purl.getName()).isEqualTo("acme");
        assertThat(purl.getVersion()).isEqualTo("1.0");
        assertThat(purl.getQualifiers()).containsOnly(entry("type", "jar"));
        assertThat(purl.getSubpath()).isNull();
    }

    @Test
    void parseDebPurl() {
        Purl purl = Purl.parse("pkg:deb/debian/curl@7.50.3-1?arch=i386&distro=jessie");
        assertThat(purl.getType()).isEqualTo("deb");
        assertThat(purl.getNamespace()).isEqualTo("debian");
        assertThat(purl.getName()).isEqualTo("curl");
        assertThat(purl.getVersion()).isEqualTo("7.50.3-1");
        assertThat(purl.getQualifiers()).containsOnly(entry("arch", "i386"), entry("distro", "jessie"));
        assertThat(purl.getSubpath()).isNull();
    }

    @Test
    void parseDockerPurl() {
        Purl purl = Purl.parse("pkg:docker/cassandra@sha256:244fd47e07d1004f0aed9c");
        assertThat(purl.getType()).isEqualTo("docker");
        assertThat(purl.getNamespace()).isNull();
        assertThat(purl.getName()).isEqualTo("cassandra");
        assertThat(purl.getVersion()).isEqualTo("sha256:244fd47e07d1004f0aed9c");
        assertThat(purl.getQualifiers()).isEmpty();
        assertThat(purl.getSubpath()).isNull();
    }

    @Test
    void parseGemPurl() {
        Purl purl = Purl.parse("pkg:gem/jruby-launcher@1.1.2?platform=java");
        assertThat(purl.getType()).isEqualTo("gem");
        assertThat(purl.getNamespace()).isNull();
        assertThat(purl.getName()).isEqualTo("jruby-launcher");
        assertThat(purl.getVersion()).isEqualTo("1.1.2");
        assertThat(purl.getQualifiers()).containsOnly(entry("platform", "java"));
        assertThat(purl.getSubpath()).isNull();
    }

    @Test
    void parseGolangPurlWithSubpath() {
        Purl purl = Purl.parse("pkg:golang/google.golang.org/genproto#googleapis/api/annotations");
        assertThat(purl.getType()).isEqualTo("golang");
        assertThat(purl.getNamespace()).isEqualTo("google.golang.org");
        assertThat(purl.getName()).isEqualTo("genproto");
        assertThat(purl.getVersion()).isNull();
        assertThat(purl.getQualifiers()).isEmpty();
        assertThat(purl.getSubpath()).isEqualTo("googleapis/api/annotations");
    }

    @Test
    void parseMavenPurlWithRepositoryUrl() {
        Purl purl = Purl.parse(
                "pkg:maven/org.apache.xmlgraphics/batik-anim@1.9.1?packaging=sources&repository_url=repo.acme.org%2Frelease");
        assertThat(purl.getType()).isEqualTo("maven");
        assertThat(purl.getNamespace()).isEqualTo("org.apache.xmlgraphics");
        assertThat(purl.getName()).isEqualTo("batik-anim");
        assertThat(purl.getVersion()).isEqualTo("1.9.1");
        assertThat(purl.getQualifiers()).containsOnly(
                entry("packaging", "sources"),
                entry("repository_url", "repo.acme.org/release"));
        assertThat(purl.getSubpath()).isNull();
    }

    @Test
    void parseNpmScopedPurl() {
        Purl purl = Purl.parse("pkg:npm/%40angular/animation@12.3.1");
        assertThat(purl.getType()).isEqualTo("npm");
        assertThat(purl.getNamespace()).isEqualTo("@angular");
        assertThat(purl.getName()).isEqualTo("animation");
        assertThat(purl.getVersion()).isEqualTo("12.3.1");
        assertThat(purl.getQualifiers()).isEmpty();
        assertThat(purl.getSubpath()).isNull();
    }

    @Test
    void parseNugetPurl() {
        Purl purl = Purl.parse("pkg:nuget/EnterpriseLibrary.Common@6.0.1304");
        assertThat(purl.getType()).isEqualTo("nuget");
        assertThat(purl.getNamespace()).isNull();
        assertThat(purl.getName()).isEqualTo("EnterpriseLibrary.Common");
        assertThat(purl.getVersion()).isEqualTo("6.0.1304");
        assertThat(purl.getQualifiers()).isEmpty();
        assertThat(purl.getSubpath()).isNull();
    }

    @Test
    void parsePypiPurl() {
        Purl purl = Purl.parse("pkg:pypi/django@1.11.1");
        assertThat(purl.getType()).isEqualTo("pypi");
        assertThat(purl.getNamespace()).isNull();
        assertThat(purl.getName()).isEqualTo("django");
        assertThat(purl.getVersion()).isEqualTo("1.11.1");
        assertThat(purl.getQualifiers()).isEmpty();
        assertThat(purl.getSubpath()).isNull();
    }

    @Test
    void parseRpmFedoraPurl() {
        Purl purl = Purl.parse("pkg:rpm/fedora/curl@7.50.3-1.fc25?arch=i386&distro=fedora-25");
        assertThat(purl.getType()).isEqualTo("rpm");
        assertThat(purl.getNamespace()).isEqualTo("fedora");
        assertThat(purl.getName()).isEqualTo("curl");
        assertThat(purl.getVersion()).isEqualTo("7.50.3-1.fc25");
        assertThat(purl.getQualifiers()).containsOnly(entry("arch", "i386"), entry("distro", "fedora-25"));
        assertThat(purl.getSubpath()).isNull();
    }

    @Test
    void parseRpmOpensusePurl() {
        Purl purl = Purl.parse("pkg:rpm/opensuse/curl@7.56.1-1.1.?arch=i386&distro=opensuse-tumbleweed");
        assertThat(purl.getType()).isEqualTo("rpm");
        assertThat(purl.getNamespace()).isEqualTo("opensuse");
        assertThat(purl.getName()).isEqualTo("curl");
        assertThat(purl.getVersion()).isEqualTo("7.56.1-1.1.");
        assertThat(purl.getQualifiers()).containsOnly(entry("arch", "i386"), entry("distro", "opensuse-tumbleweed"));
        assertThat(purl.getSubpath()).isNull();
    }

    @Test
    void parseQualifierWithEncodedSlashes() {
        Purl purl = Purl.parse(
                "pkg:maven/org.apache.james/apache-mime4j-storage@0.8.9.redhat-00001?repository_url=https%3A%2F%2Fmaven.repository.redhat.com%2Fga%2F&type=jar");
        assertThat(purl.getType()).isEqualTo("maven");
        assertThat(purl.getNamespace()).isEqualTo("org.apache.james");
        assertThat(purl.getName()).isEqualTo("apache-mime4j-storage");
        assertThat(purl.getVersion()).isEqualTo("0.8.9.redhat-00001");
        assertThat(purl.getQualifiers()).containsOnly(
                entry("repository_url", "https://maven.repository.redhat.com/ga/"),
                entry("type", "jar"));
        assertThat(purl.getSubpath()).isNull();
    }

    @Test
    void parseQualifierWithUnencodedSlashes() {
        Purl purl = Purl.parse(
                "pkg:maven/org.apache.james/apache-mime4j-storage@0.8.9.redhat-00001?repository_url=https://maven.repository.redhat.com/ga/&type=jar");
        assertThat(purl.getType()).isEqualTo("maven");
        assertThat(purl.getNamespace()).isEqualTo("org.apache.james");
        assertThat(purl.getName()).isEqualTo("apache-mime4j-storage");
        assertThat(purl.getVersion()).isEqualTo("0.8.9.redhat-00001");
        assertThat(purl.getQualifiers()).containsOnly(
                entry("repository_url", "https://maven.repository.redhat.com/ga/"),
                entry("type", "jar"));
        assertThat(purl.getSubpath()).isNull();
    }

    @Test
    void parseBothSlashEncodingVariantsProduceSameResult() {
        Purl encoded = Purl.parse(
                "pkg:maven/org.apache.james/apache-mime4j-storage@0.8.9.redhat-00001?repository_url=https%3A%2F%2Fmaven.repository.redhat.com%2Fga%2F&type=jar");
        Purl unencoded = Purl.parse(
                "pkg:maven/org.apache.james/apache-mime4j-storage@0.8.9.redhat-00001?repository_url=https://maven.repository.redhat.com/ga/&type=jar");
        assertThat(encoded).isEqualTo(unencoded);
    }

    @Test
    void toStringDoesNotEncodeSlashesAndColonsInQualifierValues() {
        Purl purl = Purl.builder()
                .setType("maven")
                .setNamespace("org.apache.james")
                .setName("apache-mime4j-storage")
                .setVersion("0.8.9.redhat-00001")
                .addQualifier("repository_url", "https://maven.repository.redhat.com/ga/")
                .addQualifier("type", "jar")
                .build();
        assertThat(purl.toString()).isEqualTo(
                "pkg:maven/org.apache.james/apache-mime4j-storage@0.8.9.redhat-00001?repository_url=https://maven.repository.redhat.com/ga/&type=jar");
    }

    @Test
    void percentEncodeProducesTwoDigitHex() {
        assertThat(Purl.percentEncode("\t")).isEqualTo("%09");
        assertThat(Purl.percentEncode("\0")).isEqualTo("%00");
        assertThat(Purl.percentEncode(" ")).isEqualTo("%20");
    }

    @Test
    void percentDecodeHandlesMultiByteUtf8() {
        assertThat(Purl.percentDecode("%C3%A9")).isEqualTo("é");
        assertThat(Purl.percentDecode("cl%C3%A9ment")).isEqualTo("clément");
    }

    // --- Validation: empty name/type ---

    @Test
    void emptyNameThrows() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Purl.builder().setType("npm").setName("").build())
                .withMessageContaining("name must not be empty");
    }

    @Test
    void emptyTypeThrows() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Purl.builder().setType("").setName("foo").build())
                .withMessageContaining("type must not be empty");
    }

    @Test
    void parseTrailingSlashRejectsEmptyName() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Purl.parse("pkg:maven/com.foo/"))
                .withMessageContaining("name must not be empty");
    }

    // --- Validation: empty version after @ ---

    @Test
    void parseTrailingAtNormalizesVersionToNull() {
        Purl purl = Purl.parse("pkg:maven/com.foo/bar@");
        assertThat(purl.getVersion()).isNull();
    }

    // --- Validation: Maven factory args ---

    @Test
    void mavenNullGroupIdThrows() {
        assertThatNullPointerException()
                .isThrownBy(() -> Purl.maven(null, "artifact", "1.0"))
                .withMessageContaining("groupId");
    }

    @Test
    void mavenNullArtifactIdThrows() {
        assertThatNullPointerException()
                .isThrownBy(() -> Purl.maven("com.foo", null, "1.0"))
                .withMessageContaining("artifactId");
    }

    // --- Validation: type format ---

    @Test
    void typeStartingWithDigitThrows() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Purl.of("1bad", null, "foo", "1.0"))
                .withMessageContaining("must start with a letter");
    }

    @Test
    void typeWithInvalidCharThrows() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Purl.of("ba$d", null, "foo", "1.0"))
                .withMessageContaining("invalid character");
    }

    @Test
    void typeWithDotsAndPlusesAndDashesAllowed() {
        Purl purl = Purl.of("my.type+v2-beta", null, "foo", "1.0");
        assertThat(purl.getType()).isEqualTo("my.type+v2-beta");
    }

    @Test
    void parseTypeIsCaseInsensitive() {
        Purl purl = Purl.parse("pkg:Maven/io.quarkus/quarkus-core@1.0");
        assertThat(purl.getType()).isEqualTo("maven");
    }

    // --- Validation: percent-decoding ---

    @Test
    void percentDecodeMalformedThrows() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Purl.percentDecode("foo%ZZbar"))
                .withMessageContaining("Invalid percent-encoding");
    }

    @Test
    void percentDecodeLonePercentThrows() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Purl.percentDecode("foo%"))
                .withMessageContaining("Invalid percent-encoding");
    }

    @Test
    void percentDecodeTrailingPercentSingleHexThrows() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Purl.percentDecode("foo%2"))
                .withMessageContaining("Invalid percent-encoding");
    }

    // --- Qualifier parsing without split ---

    @Test
    void parseQualifierEmptyKeyThrows() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Purl.parse("pkg:maven/g/a@1.0?=value"))
                .withMessageContaining("empty key");
    }

    @Test
    void parseQualifierMissingEqualsThrows() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Purl.parse("pkg:maven/g/a@1.0?noequals"))
                .withMessageContaining("missing '='");
    }

    @Test
    void parseQualifierKeysAreLowercased() {
        Purl purl = Purl.parse("pkg:maven/g/a@1.0?Type=jar");
        assertThat(purl.getQualifiers()).containsKey("type");
    }

    // --- Encoding round-trips ---

    @Test
    void encodePathPreservesSlashes() {
        Purl purl = Purl.builder()
                .setType("golang")
                .setNamespace("google.golang.org")
                .setName("genproto")
                .setSubpath("googleapis/api/annotations")
                .build();
        assertThat(purl.toString()).contains("#googleapis/api/annotations");
    }

    @Test
    void encodePathEncodesSpecialCharsInSegments() {
        Purl purl = Purl.builder()
                .setType("generic")
                .setNamespace("org/@special")
                .setName("foo")
                .setVersion("1.0")
                .build();
        String s = purl.toString();
        assertThat(s).contains("org/%40special");
        Purl parsed = Purl.parse(s);
        assertThat(parsed.getNamespace()).isEqualTo("org/@special");
    }

    @Test
    void roundTripWithUnicodeInName() {
        Purl purl = Purl.builder()
                .setType("generic")
                .setName("clément")
                .setVersion("1.0")
                .build();
        Purl parsed = Purl.parse(purl.toString());
        assertThat(parsed.getName()).isEqualTo("clément");
        assertThat(parsed).isEqualTo(purl);
    }

    // --- pkg:// tolerance ---

    @Test
    void parsePkgDoubleSlash() {
        Purl purl = Purl.parse("pkg://maven/io.quarkus/quarkus-core@1.0?type=jar");
        assertThat(purl.getType()).isEqualTo("maven");
        assertThat(purl.getNamespace()).isEqualTo("io.quarkus");
        assertThat(purl.getName()).isEqualTo("quarkus-core");
        assertThat(purl.getVersion()).isEqualTo("1.0");
    }

    @Test
    void parsePkgTripleSlash() {
        Purl purl = Purl.parse("pkg:///maven/io.quarkus/quarkus-core@1.0");
        assertThat(purl.getType()).isEqualTo("maven");
        assertThat(purl.getNamespace()).isEqualTo("io.quarkus");
        assertThat(purl.getName()).isEqualTo("quarkus-core");
        assertThat(purl.getVersion()).isEqualTo("1.0");
    }

    @Test
    void parsePkgDoubleSlashEquivalent() {
        Purl canonical = Purl.parse("pkg:gem/ruby-advisory-db-check@0.12.4");
        Purl withSlashes = Purl.parse("pkg://gem/ruby-advisory-db-check@0.12.4");
        assertThat(withSlashes).isEqualTo(canonical);
    }

    // --- npm name lowercasing ---

    @Test
    void npmNameIsLowercased() {
        Purl purl = Purl.npm(null, "Lodash", "4.0");
        assertThat(purl.getName()).isEqualTo("lodash");
    }

    @Test
    void parseNpmNameIsLowercased() {
        Purl purl = Purl.parse("pkg:npm/Lodash@4.0");
        assertThat(purl.getName()).isEqualTo("lodash");
    }

    @Test
    void npmScopedNameIsLowercased() {
        Purl purl = Purl.npm("@Babel", "Core", "7.0");
        assertThat(purl.getName()).isEqualTo("core");
        assertThat(purl.getNamespace()).isEqualTo("@Babel");
    }

    // --- Maven namespace requirement ---

    @Test
    void mavenRequiresNamespace() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Purl.of("maven", null, "foo", "1.0"))
                .withMessageContaining("namespace");
    }

    @Test
    void mavenParseRequiresNamespace() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Purl.parse("pkg:maven/foo@1.0"))
                .withMessageContaining("namespace");
    }

    // --- Subpath validation ---

    @Test
    void subpathDotSegmentsDiscarded() {
        Purl purl = Purl.builder()
                .setType("generic")
                .setName("foo")
                .setVersion("1.0")
                .setSubpath("./src/../main")
                .build();
        assertThat(purl.getSubpath()).isEqualTo("src/main");
    }

    @Test
    void subpathAllDotsBecomesNull() {
        Purl purl = Purl.builder()
                .setType("generic")
                .setName("foo")
                .setVersion("1.0")
                .setSubpath(".")
                .build();
        assertThat(purl.getSubpath()).isNull();
    }

    @Test
    void subpathDoubleDotBecomesNull() {
        Purl purl = Purl.builder()
                .setType("generic")
                .setName("foo")
                .setVersion("1.0")
                .setSubpath("..")
                .build();
        assertThat(purl.getSubpath()).isNull();
    }

    @Test
    void subpathEmptySegmentsDiscarded() {
        Purl purl = Purl.builder()
                .setType("generic")
                .setName("foo")
                .setVersion("1.0")
                .setSubpath("src//main")
                .build();
        assertThat(purl.getSubpath()).isEqualTo("src/main");
    }

    @Test
    void subpathNormalSegmentsPreserved() {
        Purl purl = Purl.builder()
                .setType("generic")
                .setName("foo")
                .setVersion("1.0")
                .setSubpath("src/main/java")
                .build();
        assertThat(purl.getSubpath()).isEqualTo("src/main/java");
    }

    @Test
    void parseSubpathDotSegmentsDiscarded() {
        Purl purl = Purl.parse("pkg:generic/foo@1.0#./src/../main");
        assertThat(purl.getSubpath()).isEqualTo("src/main");
    }
}
