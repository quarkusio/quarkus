package io.quarkus.jfr.runtime.internal.runtime;

import java.util.List;

import io.quarkus.runtime.ImageMode;
import io.quarkus.runtime.configuration.ConfigUtils;

public interface QuarkusRuntimeInfo {

    String imageMode();

    String profiles();

    String version();

    List<String> features();

    static QuarkusRuntimeInfo create(String version, List<String> features) {
        return new QuarkusRuntimeInfo() {
            @Override
            public String imageMode() {
                return ImageMode.current().name();
            }

            @Override
            public String profiles() {
                return String.join(",", ConfigUtils.getProfiles());
            }

            @Override
            public String version() {
                return version;
            }

            @Override
            public List<String> features() {
                return features;
            }
        };
    }
}
