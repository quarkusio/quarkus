/*
 * Copyright 2018 Red Hat, Inc.
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

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A file that if modified should result in a hot redeployment when in
 * dev mode.
 */
public final class HotDeploymentWatchedFileBuildItem extends MultiBuildItem {

    private final String location;

    public HotDeploymentWatchedFileBuildItem(String location) {
        this.location = location;
    }

    public String getLocation() {
        return location;
    }
}
