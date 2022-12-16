package org.acme.example.extension.deployment;

import io.quarkus.builder.item.SimpleBuildItem;


import java.util.Optional;

public final class EnabledBuildItem extends SimpleBuildItem {

	private final Boolean enabled;

	public EnabledBuildItem(final Boolean enabled){
		this.enabled=enabled;
	}

	public Boolean getEnabled() {
		return enabled;
	}
}