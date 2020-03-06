package io.quarkus.utilities.depchecker;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public class MavenModule {

	public final String name;
	private final Set<String> dependsOn = new TreeSet<>();
	public final Set<MavenModule> dependents = new HashSet<>();
	
	public MavenModule(String name) {
		Objects.requireNonNull(name);
		this.name = name;
	}
	
	public Set<String> dependsOn() {
		return Collections.unmodifiableSet(dependsOn);
	}
	
	public void addDependsOn(String module) {
		// Only add groupId:artifactId string (strip version and type)
		int first = module.indexOf(':');
		int second = module.indexOf(':', first + 1);
		if (second > 0)
			module = module.substring(0, second);
		module = QuarkusModules.sanitizeModule(module);
		dependsOn.add(module);
//			System.out.println("  dependsOn: " + module);
	}
	
	public boolean isIntegrationTest() {
		return name.contains("-integration-test-");
	}
	
	@Override
	public String toString() {
		return super.toString() + "{name=" + name + ", dependsOn=" + dependsOn + "}";
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof MavenModule))
			return false;
		MavenModule other = (MavenModule) obj;
		return Objects.equals(name, other.name);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(name);
	}
}
