package io.quarkus.utilities.depchecker;

import java.util.HashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

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
	
	@Override
	public MavenModule getOrDefault(Object key, MavenModule defaultValue) {
		return super.getOrDefault(sanitizeModule((String) key), defaultValue);
	}
	
	@Override
	public MavenModule computeIfAbsent(String key, Function<? super String, ? extends MavenModule> mappingFunction) {
		return super.computeIfAbsent(sanitizeModule(key), mappingFunction);
	}
	
	@Override
	public boolean containsKey(Object key) {
		return super.containsKey(sanitizeModule((String) key));
	}
	
	@Override
	public MavenModule computeIfPresent(String key, BiFunction<? super String, ? super MavenModule, ? extends MavenModule> remappingFunction) {
		return super.computeIfPresent(sanitizeModule(key), remappingFunction);
	}
	
	@Override
	public MavenModule putIfAbsent(String key, MavenModule value) {
		return super.putIfAbsent(sanitizeModule(key), value);
	}
	
	@Override
	public MavenModule remove(Object key) {
		return super.remove(sanitizeModule((String) key));
	}
	
	@Override
	public boolean remove(Object key, Object value) {
		return super.remove(sanitizeModule((String) key), value);
	}

	// Quarkus-specific behavior: merge -deployment modules with their
	// non-deployment counterparts
	public static String sanitizeModule(String name) {
		if (name != null && name.endsWith("-deployment"))
			name = name.substring(0, name.length() - "-deployment".length());
		return name;
	}

}
