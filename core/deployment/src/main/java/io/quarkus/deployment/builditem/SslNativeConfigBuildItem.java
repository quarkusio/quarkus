/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.deployment.builditem;

import java.util.Optional;

import org.jboss.builder.item.SimpleBuildItem;

public final class SslNativeConfigBuildItem extends SimpleBuildItem {

    private Optional<Boolean> enableSslNativeConfig;

    public SslNativeConfigBuildItem(Optional<Boolean> enableSslNativeConfig) {
        this.enableSslNativeConfig = enableSslNativeConfig;
    }

    public Optional<Boolean> get() {
        return enableSslNativeConfig;
    }

    public boolean isEnabled() {
        // default is to disable the SSL native support
        return enableSslNativeConfig.isPresent() && enableSslNativeConfig.get();
    }

    public boolean isExplicitlyDisabled() {
        return enableSslNativeConfig.isPresent() && !enableSslNativeConfig.get();
    }
}
