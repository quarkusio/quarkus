package io.quarkus.gradle.model.pom;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.maven.model.building.ModelSource2;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.junit.jupiter.api.Test;

import io.quarkus.maven.dependency.GAV;

class MavenEffectiveModelResolverTest {

    @Test
    void shouldResolveEffectiveModelThroughPomResolver() throws Exception {
        var resolver = new MavenEffectiveModelResolver(new InMemoryPomResolver(Map.of(
                new GAV("org.acme", "parent", "1.0"), pom("""
                        <project>
                          <modelVersion>4.0.0</modelVersion>
                          <groupId>org.acme</groupId>
                          <artifactId>parent</artifactId>
                          <version>1.0</version>
                          <packaging>pom</packaging>
                          <properties>
                            <runtime.version>3.0</runtime.version>
                          </properties>
                          <dependencyManagement>
                            <dependencies>
                              <dependency>
                                <groupId>org.acme</groupId>
                                <artifactId>managed-lib</artifactId>
                                <version>2.0</version>
                              </dependency>
                            </dependencies>
                          </dependencyManagement>
                        </project>
                        """),
                new GAV("org.acme", "acme-bom", "1.0"), pom("""
                        <project>
                          <modelVersion>4.0.0</modelVersion>
                          <groupId>org.acme</groupId>
                          <artifactId>acme-bom</artifactId>
                          <version>1.0</version>
                          <packaging>pom</packaging>
                          <dependencyManagement>
                            <dependencies>
                              <dependency>
                                <groupId>org.acme</groupId>
                                <artifactId>bom-managed-lib</artifactId>
                                <version>4.0</version>
                              </dependency>
                            </dependencies>
                          </dependencyManagement>
                        </project>
                        """),
                new GAV("org.acme", "app", "1.0"), pom("""
                        <project>
                          <modelVersion>4.0.0</modelVersion>
                          <parent>
                            <groupId>org.acme</groupId>
                            <artifactId>parent</artifactId>
                            <version>1.0</version>
                          </parent>
                          <artifactId>app</artifactId>
                          <dependencyManagement>
                            <dependencies>
                              <dependency>
                                <groupId>org.acme</groupId>
                                <artifactId>acme-bom</artifactId>
                                <version>1.0</version>
                                <type>pom</type>
                                <scope>import</scope>
                              </dependency>
                            </dependencies>
                          </dependencyManagement>
                          <dependencies>
                            <dependency>
                              <groupId>org.acme</groupId>
                              <artifactId>managed-lib</artifactId>
                            </dependency>
                            <dependency>
                              <groupId>org.acme</groupId>
                              <artifactId>bom-managed-lib</artifactId>
                            </dependency>
                            <dependency>
                              <groupId>org.acme</groupId>
                              <artifactId>runtime-lib</artifactId>
                              <version>${runtime.version}</version>
                              <scope>runtime</scope>
                            </dependency>
                            <dependency>
                              <groupId>org.acme</groupId>
                              <artifactId>system-property-lib</artifactId>
                              <version>${external.version}</version>
                            </dependency>
                          </dependencies>
                        </project>
                        """))), () -> Map.of("external.version", "5.0"));

        var effectiveModel = resolver.resolveEffectiveModel("org.acme", "app", "1.0");

        assertThat(effectiveModel.getDependencies())
                .extracting(dependency -> dependency.getGroupId()
                        + ":" + dependency.getArtifactId()
                        + ":" + dependency.getVersion()
                        + ":" + dependency.getScope())
                .containsExactly(
                        "org.acme:managed-lib:2.0:compile",
                        "org.acme:bom-managed-lib:4.0:compile",
                        "org.acme:runtime-lib:3.0:runtime",
                        "org.acme:system-property-lib:5.0:compile");
    }

    private record InMemoryPomResolver(Map<GAV, String> poms) implements PomResolver {

        @Override
        public ModelSource2 resolvePom(GAV gav) throws UnresolvableModelException {
            String pom = poms.get(gav);
            if (pom == null) {
                throw new UnresolvableModelException("Could not resolve POM for " + gav,
                        gav.getGroupId(), gav.getArtifactId(), gav.getVersion());
            }
            return new StringModelSource(gav, pom);
        }
    }

    private record StringModelSource(GAV gav, String pom) implements ModelSource2 {

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(pom.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public String getLocation() {
            return gav.toString();
        }

        @Override
        public ModelSource2 getRelatedSource(String relPath) {
            return null;
        }

        @Override
        public URI getLocationURI() {
            return URI.create("memory:/" + gav.getGroupId() + "/" + gav.getArtifactId() + "/" + gav.getVersion());
        }
    }

    private static String pom(String body) {
        return body.replace("<project>", "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" "
                + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                + "xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 "
                + "https://maven.apache.org/xsd/maven-4.0.0.xsd\">");
    }
}
