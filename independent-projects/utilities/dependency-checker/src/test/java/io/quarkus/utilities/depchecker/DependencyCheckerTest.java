package io.quarkus.utilities.depchecker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.quarkus.utilities.depchecker.DependencyOutputHandler.MavenModule;

public class DependencyCheckerTest {
	
	private static Map<String,MavenModule> cachedModules;
	
//	/**
//	 * Perform some basic verification on doing an actual dependency check on the current repo
//	 */
//	//@Test
//	public void testGetMvnDependencies() throws Exception {
//		DependencyChecker depChecker = new DependencyChecker("foo", "bar");
//		
//		Map<String,MavenModule> mvnDeps = depChecker.getMvnDependencies();
//		assertTrue(mvnDeps.size() > 400,
//				"There should be at least 400 maven dependencies but only found " + mvnDeps.size());
//		assertNotNull(mvnDeps.get("io.quarkus:quarkus-core"));
//		assertNotNull(mvnDeps.get("io.quarkus:quarkus-core-deployment"));
//		assertNotNull(mvnDeps.get("io.quarkus:quarkus-core-parent"));
//		
//		assertNotNull(mvnDeps.get("io.quarkus:quarkus-hibernate-orm"));
//		assertNotNull(mvnDeps.get("io.quarkus:quarkus-hibernate-orm-deployment"));
//		assertNotNull(mvnDeps.get("io.quarkus:quarkus-hibernate-orm-parent"));
//		
//		assertNotNull(mvnDeps.get("io.quarkus:quarkus-integration-test-flyway"));
//		assertNotNull(mvnDeps.get("io.quarkus:quarkus-integration-test-jpa"));
//		assertNotNull(mvnDeps.get("io.quarkus:quarkus-integration-test-maven"));
//	}
	
	@Test
	public void testMavenDependenciesSnapshot() {
		Map<String,MavenModule> mavenModules = getSnapshotData();
		
		assertEquals(448, mavenModules.size());
		
		MavenModule core = mavenModules.get("io.quarkus:quarkus-core");
		assertNotNull(core);
		assertNotNull(mavenModules.get("io.quarkus:quarkus-core-deployment"));
		assertNotNull(mavenModules.get("io.quarkus:quarkus-core-parent"));
		assertEquals("io.quarkus:quarkus-core", core.name);
		assertEquals(2, core.dependsOn().size());
		
		MavenModule hibernate = mavenModules.get("io.quarkus:quarkus-hibernate-orm");
		assertNotNull(hibernate);
		assertNotNull(mavenModules.get("io.quarkus:quarkus-hibernate-orm-deployment"));
		assertNotNull(mavenModules.get("io.quarkus:quarkus-hibernate-orm-parent"));
		assertEquals("io.quarkus:quarkus-hibernate-orm", hibernate.name);
		assertEquals(15, hibernate.dependsOn().size());
		
		assertNotNull(mavenModules.get("io.quarkus:quarkus-integration-test-flyway"));
		assertNotNull(mavenModules.get("io.quarkus:quarkus-integration-test-jpa"));
		assertNotNull(mavenModules.get("io.quarkus:quarkus-integration-test-maven"));
		
		MavenModule jpaTest = mavenModules.get("io.quarkus:quarkus-integration-test-jpa");
		assertEquals("io.quarkus:quarkus-integration-test-jpa", jpaTest.name);
		assertEquals(25, jpaTest.dependsOn().size());
	}
	
	@Test
	public void testModifyIntegrationTest() {
		Set<String> files = set("integration-tests/jpa-postgresql/src/java/Foo.java");
		Set<MavenModule> mods = new DependencyChecker("foo", "bar")
				.getModifiedModules(getSnapshotData(), files);
		
		assertEquals(1, mods.size());
		assertEquals("io.quarkus:quarkus-integration-test-jpa-postgresql", mods.iterator().next().name);
	}
	
	@Test
	public void testModifyCore() {
		Set<String> files = set("core/runtime/src/main/java/io/quarkus/runtime/LaunchMode.java");
		Set<MavenModule> mods = new DependencyChecker("foo", "bar")
				.getModifiedModules(getSnapshotData(), files);
		
		assertEquals(1, mods.size());
		assertEquals("io.quarkus:quarkus-core", mods.iterator().next().name);
	}
	
	@Test
	public void testModifyBogusFile() {
		Set<String> files = set("bogus/does/not/exist.txt");
		Set<MavenModule> mods = new DependencyChecker("foo", "bar")
				.getModifiedModules(getSnapshotData(), files);
		assertEquals(1, mods.size());
		assertEquals(DependencyChecker.ALL_MODULES, mods.iterator().next());
	}
	
	@Test
	public void testModifyBom() {
		Set<String> files = set("bom/runtime/pom.xml");
		Set<MavenModule> mods = new DependencyChecker("foo", "bar")
				.getModifiedModules(getSnapshotData(), files);
		assertEquals(1, mods.size());
		assertEquals(DependencyChecker.ALL_MODULES, mods.iterator().next());
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
		return cachedModules = oh.mavenModules;
	}

}
