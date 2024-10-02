/**
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
 *
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package io.quarkus.kubernetes.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * The configuration of environment variables prefix.
 * It is used with <code>quarkus.kubernetes.env.secrets</code> and <code>quarkus.kubernetes.env.configmaps</code> to specify the
 * prefix to use when adding the environment variable to the container.
 */
@ConfigGroup
public class EnvVarPrefixConfig {

    /**
     * The optional prefix to use when adding the environment variable to the container.
     */
    @ConfigItem
    Optional<String> forSecret;

    /**
     * The optional prefix to use when adding the environment variable to the container.
     */
    @ConfigItem
    Optional<String> forConfigmap;

    public boolean anyPresent() {
        return forSecret.isPresent() || forConfigmap.isPresent();
    }

    public boolean hasConfigmap() {
        return forConfigmap.isPresent();
    }

    public boolean hasSecret() {
        return forSecret.isPresent();
    }

    public boolean hasPrefixForSecret(String secret) {
        return forSecret.map(s -> s.equals(secret)).orElse(false);
    }

    public boolean hasPrefixForConfigmap(String configmap) {
        return forConfigmap.map(c -> c.equals(configmap)).orElse(false);
    }
}
