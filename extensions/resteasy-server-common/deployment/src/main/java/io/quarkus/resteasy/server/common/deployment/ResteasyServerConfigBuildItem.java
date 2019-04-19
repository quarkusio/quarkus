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

package io.quarkus.resteasy.server.common.deployment;

import java.util.Map;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * A build item that represents the configuration of the RESTEasy server.
 */
public final class ResteasyServerConfigBuildItem extends SimpleBuildItem {

    private final String path;

    private final Map<String, String> initParameters;

    public ResteasyServerConfigBuildItem(String path, Map<String, String> initParameters) {
        this.path = path;
        this.initParameters = initParameters;
    }

    public String getPath() {
        return path;
    }

    public Map<String, String> getInitParameters() {
        return initParameters;
    }
}
