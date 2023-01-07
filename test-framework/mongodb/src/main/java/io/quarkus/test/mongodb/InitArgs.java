package io.quarkus.test.mongodb;

import de.flapdoodle.embed.mongo.distribution.IFeatureAwareVersion;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.distribution.Versions;

import java.util.Map;
import java.util.Optional;

public abstract class InitArgs {
	public static final String PORT = "port";
	public static final String VERSION = "version";

	private static final int DEFAULT_PORT = 27017;

	public static int port(Map<String, String> initArgs) {
		return Optional.ofNullable(initArgs.get(PORT)).map(Integer::parseInt).orElse(DEFAULT_PORT);
	}

	public static IFeatureAwareVersion version(Map<String, String> initArgs) {
		IFeatureAwareVersion version = Optional.ofNullable(initArgs.get(VERSION))
			.map(versionStr -> Versions.withFeatures(de.flapdoodle.embed.process.distribution.Version.of(versionStr)))
			.orElse(Version.Main.V4_0);

		return version;
	}
}
