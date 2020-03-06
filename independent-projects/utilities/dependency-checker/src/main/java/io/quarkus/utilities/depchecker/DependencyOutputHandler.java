package io.quarkus.utilities.depchecker;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.shared.invoker.InvocationOutputHandler;

public class DependencyOutputHandler implements InvocationOutputHandler {
	
	// SAMPLE OUTPUT:
//	[INFO] ----------< io.quarkus:quarkus-integration-test-logging-gelf >----------
//	[INFO] Building Quarkus - Integration Tests - Logging - GELF 999-SNAPSHOT [433/434]
//	[INFO] --------------------------------[ jar ]---------------------------------
//	[INFO] 
//	[INFO] --- maven-dependency-plugin:3.1.1:tree (default-cli) @ quarkus-integration-test-logging-gelf ---
//	[INFO] io.quarkus:quarkus-integration-test-logging-gelf:jar:999-SNAPSHOT
//	[INFO] +- io.quarkus:quarkus-resteasy:jar:999-SNAPSHOT:compile
//	[INFO] |  +- io.quarkus:quarkus-vertx-http:jar:999-SNAPSHOT:compile
//	[INFO] |  |  \- io.quarkus.security:quarkus-security:jar:1.0.1.Final:compile
//	[INFO] |  \- io.quarkus:quarkus-resteasy-server-common:jar:999-SNAPSHOT:compile
//	[INFO] |     \- io.quarkus:quarkus-arc:jar:999-SNAPSHOT:compile
//	[INFO] |        \- io.quarkus.arc:arc:jar:999-SNAPSHOT:compile
//	[INFO] \- io.quarkus:quarkus-junit5:jar:999-SNAPSHOT:test
//	[INFO]    \- io.quarkus:quarkus-test-common:jar:999-SNAPSHOT:test
//	[INFO]       \- io.quarkus:quarkus-core-deployment:jar:999-SNAPSHOT:test
//	[INFO]          \- io.quarkus.gizmo:gizmo:jar:1.0.0.Final:test
	
	private static final boolean DEBUG = false;
	
	private static final Pattern MODULE_PATTERN = Pattern.compile("^\\[INFO\\] -+< (io\\.quarkus.*) >-+$");
	private static final Pattern DEPENDENCY_PATTERN = Pattern.compile("^\\[INFO\\] .*(io\\.quarkus.*:.*):.*$");
	
	public final QuarkusModules mavenModules = new QuarkusModules();
	private MavenModule currentModule;
	private int numModules = 0;

	@Override
	public void consumeLine(String line) throws IOException {
		if (!line.startsWith("[INFO] "))
			return;
		
//		System.out.println(line);
		
		// New maven module definition
		Matcher moduleMatcher = MODULE_PATTERN.matcher(line);
		if (moduleMatcher.matches()) {
			String module = moduleMatcher.group(1);
			if (DEBUG)
				System.out.println("found module (" + ++numModules + ") : " + module);
			currentModule = mavenModules.computeIfAbsent(QuarkusModules.sanitizeModule(module), m -> new MavenModule(m));
			return;
		} 
		
		// Dependency of a maven module
		Matcher dependencyMatcher = DEPENDENCY_PATTERN.matcher(line);
		if (dependencyMatcher.matches()) {
			String dependsOn = dependencyMatcher.group(1);
			currentModule.addDependsOn(dependsOn);
			return;
		}
	}
	
}
