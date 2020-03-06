package io.quarkus.utilities.depchecker;

import static io.quarkus.utilities.depchecker.DependencyChecker.RUN_ALL_TESTS;
import static io.quarkus.utilities.depchecker.DependencyChecker.RUN_NO_TESTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class DependencyCheckerTest {
	
	private static Map<String,MavenModule> cachedModules;
	
	@Test
	public void testMavenDependenciesSnapshot() {
		Map<String,MavenModule> mavenModules = getSnapshotData();
		
		assertEquals(345, mavenModules.size());
		
		MavenModule core = mavenModules.get("io.quarkus:quarkus-core");
		assertNotNull(core);
		assertNotNull(mavenModules.get("io.quarkus:quarkus-core-deployment"));
		assertNotNull(mavenModules.get("io.quarkus:quarkus-core-parent"));
		assertEquals("io.quarkus:quarkus-core", core.name);
		assertThat(core.dependsOn())
			.contains("io.quarkus:quarkus-core")
			.contains("io.quarkus:quarkus-bootstrap-core")
			.hasSize(5);
		
		MavenModule hibernate = mavenModules.get("io.quarkus:quarkus-hibernate-orm");
		System.out.println(hibernate);
		assertNotNull(hibernate);
		assertEquals("io.quarkus:quarkus-hibernate-orm", hibernate.name);
		assertThat(hibernate.dependsOn())
			.hasSize(33);
		assertThat(mavenModules)
			.containsKey("io.quarkus:quarkus-hibernate-orm-deployment")
			.containsKey("io.quarkus:quarkus-hibernate-orm-parent");
		
		assertThat(mavenModules)
			.containsKey("io.quarkus:quarkus-integration-test-flyway")
			.containsKey("io.quarkus:quarkus-integration-test-jpa")
			.containsKey("io.quarkus:quarkus-integration-test-maven");
		
		MavenModule jpaTest = mavenModules.get("io.quarkus:quarkus-integration-test-jpa");
		assertEquals("io.quarkus:quarkus-integration-test-jpa", jpaTest.name);
		assertThat(jpaTest.dependsOn())
			.contains("io.quarkus:quarkus-hibernate-orm")
			.contains("io.quarkus:quarkus-resteasy-common")
			.contains("io.quarkus:quarkus-test-common")
			.hasSize(23);
	}
	
	@Test
	public void testModifyIntegrationTest() {
		assertThat(getTestedProjects("integration-tests/jpa-postgresql/src/java/Foo.java"))
			.isEqualTo("jpa-postgresql");
	}
	
	@Test
	public void testModifyCore() {
		// Should contain all of the integration test modules present in the snapshot
		assertThat(getTestedProjects("core/runtime/src/main/java/io/quarkus/runtime/LaunchMode.java"))
			.contains("amazon-dynamodb")
			.contains("amazon-lambda")
			.contains("amazon-lambda-http")
			.contains("amazon-lambda-http-resteasy")
			.contains("artemis-core")
			.contains("artemis-jms")
			.contains("cache")
			.contains("elytron-resteasy")
			.contains("elytron-security")
			.contains("elytron-security-oauth2")
			.contains("elytron-undertow")
			.contains("flyway")
			.contains("hibernate-orm-panache")
			.contains("hibernate-search-elasticsearch")
			.contains("hibernate-validator")
			.contains("infinispan-cache-jpa")
			.contains("infinispan-client")
			.contains("infinispan-embedded")
			.contains("jackson")
			.contains("jgit")
			.contains("jpa")
			.contains("jpa-derby")
			.contains("jpa-h2")
			.contains("jpa-mariadb")
			.contains("jpa-mssql")
			.contains("jpa-mysql")
			.contains("jpa-postgresql")
			.contains("jpa-without-entity")
			.contains("jsch")
			.contains("jsonb")
			.contains("kafka")
			.contains("kafka-streams")
			.contains("keycloak-authorization")
			.contains("kogito")
			.contains("kogito-maven")
			.contains("kotlin")
			.contains("kubernetes")
			.contains("kubernetes-client")
			.contains("logging-gelf")
			.contains("main")
			.contains("maven")
			.contains("mongodb-client")
			.contains("mongodb-panache")
			.contains("narayana-jta")
			.contains("narayana-stm")
			.contains("neo4j")
			.contains("oidc")
			.contains("oidc-code-flow")
			.contains("oidc-tenancy")
			.contains("quartz")
			.contains("qute")
			.contains("reactive-mysql-client")
			.contains("reactive-pg-client")
			.contains("resteasy-jackson")
			.contains("scala")
			.contains("spring-boot-properties")
			.contains("spring-data-jpa")
			.contains("spring-di")
			.contains("spring-web")
			.contains("tika")
			.contains("vault")
			.contains("vault-agroal")
			.contains("vault-app")
			.contains("vertx")
			.contains("vertx-graphql")
			.contains("vertx-http")
			.contains("virtual-http")
			.contains("virtual-http-resteasy");
	}
	
	@Test
	public void testModifyResteasyJsonb() {
		String TESTS1 = "cache,elytron-security,elytron-security-oauth2,hibernate-orm-panache,hibernate-validator,jpa-without-entity,kafka,"; 
		String TESTS2 = "keycloak-authorization,kubernetes-client,main,mongodb-client,mongodb-panache,neo4j,spring-data-jpa";
		assertThat(getTestedProjects("extensions/resteasy-jsonb/deployment/src/main/java/io/quarkus/Foo.java"))
			.contains(TESTS1)
			.contains(TESTS2);
		assertThat(getTestedProjects("extensions/resteasy-jsonb/runtime/src/main/java/io/quarkus/Foo.java"))
			.contains(TESTS1)
			.contains(TESTS2);
	}
	
	@Test
	@Disabled("TODO: Should modifying a parent pom require the ITs for sub-modules to run? Trying to support this would be a bit messy")
	public void testModifyParentPom() {
		String TESTS1 = "cache,elytron-security,elytron-security-oauth2,hibernate-orm-panache,hibernate-validator,jpa-without-entity,kafka,"; 
		String TESTS2 = "keycloak-authorization,kubernetes-client,main,mongodb-client,mongodb-panache,neo4j,spring-data-jpa";
		assertThat(getTestedProjects("extensions/resteasy-jsonb/pom.xml"))
			.contains(TESTS1)
			.contains(TESTS2);
	}
	
	@Test
	public void testModifyHibernateOrm() {
		assertThat(getTestedProjects("extensions/hibernate-orm/runtime/pom.xml"))
			.isEqualTo("flyway,hibernate-orm-panache,hibernate-search-elasticsearch,infinispan-cache-jpa,jpa,jpa-derby,jpa-h2," + 
					"jpa-mariadb,jpa-mssql,jpa-mysql,jpa-postgresql,jpa-without-entity,main,spring-data-jpa,vault-app");
	}
	
	@Test
	public void testModifyDerby() {
		assertThat(getTestedProjects("extensions/jdbc/jdbc-derby/deployment/quarkus-jdbc-derby-deployment.iml"))
			.isEqualTo("jpa-derby");
	}
	
	/**
	 * If we only modify unit tests for an extension project, no need to re-run native-image tests
	 */
	@Test
	public void testModifyUnitTest() {
		assertThat(getTestedProjects("extensions/hibernate-orm/deployment/src/test/java/Test.java"))
			.isEqualTo(RUN_NO_TESTS);
		assertThat(getTestedProjects("extensions/hibernate-orm/deployment/src/test/resources/persistence.xml"))
			.isEqualTo(RUN_NO_TESTS);
	}
	
	@Test
	public void testModifyBenignFiles() {
		assertThat(getTestedProjects("README.md")).isEqualTo(RUN_NO_TESTS);
		assertThat(getTestedProjects(".dependabot/_config.yml")).isEqualTo(RUN_NO_TESTS);
		assertThat(getTestedProjects(".github/foo.txt")).isEqualTo(RUN_NO_TESTS);
		assertThat(getTestedProjects("extensions/jdbc/jdbc-derby/deployment/.gitignore")).isEqualTo(RUN_NO_TESTS);
	}
	
	/**
	 * No need to run native tests if only doc changes. 
	 * A result of empty string indicates no tests will run
	 */
	@Test
	public void testModifyDocs() {
		assertThat(getTestedProjects("docs/src/main/info.adoc"))
			.isEqualTo(RUN_NO_TESTS);
	}
	
	/**
	 * If we modify a file from an unknown file then all tests should run
	 * A result of 'null' indicates all tests will run
	 */
	@Test
	public void testModifyBogusFile() {
		assertThat(getTestedProjects("bogus/does/not/exist.txt"))
			.isEqualTo(RUN_ALL_TESTS);
	}

	/**
	 * If the global BOM is modified we can't detect what sub-projects that will impact, 
	 * so run everything.
	 * A result of null indicates all tests will run
	 */
	@Test
	public void testModifyBom() {
		assertThat(getTestedProjects("bom/runtime/pom.xml"))
			.isEqualTo(RUN_ALL_TESTS);
	}
	
	/**
	 * Modifying the root pom.xml should cause everything to run
	 */
	@Test
	public void testModifyRootPom() {
		assertThat(getTestedProjects("pom.xml"))
			.isEqualTo(RUN_ALL_TESTS);
	}
	
	private static String getTestedProjects(String... f){
		DependencyChecker checker = new DependencyChecker("bogusSha1", "bogusSha2")
				.withMavenDependencies(getSnapshotData())
				.withModifiedFiles(set(f));
		try {
			return checker.computeTestsToRun();
		} catch (Exception e) {
			e.printStackTrace();
			fail(e);
			return null;
		}
	}
	
	private static Set<String> set(String... files) {
		return new HashSet<>(Arrays.asList(files));
	}
	
	private static Map<String,MavenModule> getSnapshotData() {
		if (cachedModules != null)
			return cachedModules;
		
		DependencyOutputHandler oh = new DependencyOutputHandler();
		try {
			Files.readAllLines(Paths.get("src", "test", "resources", "maven-deps-snapshot.txt"))
				.stream()
				.forEach(line -> {
					try {
						oh.consumeLine(line);
					} catch (IOException e) {
						fail(e);
					}
				});
		} catch (IOException e) {
			fail(e);
		}
		return cachedModules = Collections.unmodifiableMap(oh.mavenModules);
	}

}
