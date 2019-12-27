package io.quarkus.utilities.depchecker;

import java.util.HashMap;

import io.quarkus.utilities.depchecker.DependencyOutputHandler.MavenModule;

@SuppressWarnings("serial")
public class QuarkusModules extends HashMap<String, MavenModule> {
	
	@Override
	public MavenModule put(String key, MavenModule value) {
		return super.put(sanitizeModule(key), value);
	}
	
	@Override
	public MavenModule get(Object key) {
		return super.get(sanitizeModule((String) key));
	}

	// Quarkus-specific behavior: merge -deployment modules with their
	// non-deployment counterparts
	public static String sanitizeModule(String name) {
		if (name != null && name.endsWith("-deployment"))
			name = name.substring(0, name.length() - "-deployment".length());
		return name;
	}

}
