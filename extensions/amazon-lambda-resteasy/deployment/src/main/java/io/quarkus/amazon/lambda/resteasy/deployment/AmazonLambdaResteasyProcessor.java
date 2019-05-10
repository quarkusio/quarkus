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
package io.quarkus.amazon.lambda.resteasy.deployment;

import java.util.Optional;

import io.quarkus.amazon.lambda.resteasy.runtime.AmazonLambdaResteasyConfig;
import io.quarkus.amazon.lambda.resteasy.runtime.AmazonLambdaResteasyTemplate;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.resteasy.server.common.deployment.ResteasyInjectionReadyBuildItem;
import io.quarkus.resteasy.server.common.deployment.ResteasyServerConfigBuildItem;

public class AmazonLambdaResteasyProcessor {

    AmazonLambdaResteasyConfig config;

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void setup(AmazonLambdaResteasyTemplate template, Optional<ResteasyServerConfigBuildItem> resteasyServerConfig,
            ResteasyInjectionReadyBuildItem resteasyInjectionReady) {
        if (resteasyServerConfig.isPresent()) {
            template.initHandler(resteasyServerConfig.get().getInitParameters(), config);
        }
    }
}
