/*
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

/**
 * The configuration of environment variables taking their value from a Secret or ConfigMap field identified by its key.
 */
public interface EnvVarFromKeyConfig {
    /**
     * The optional name of the Secret from which a value is to be extracted.
     * Mutually exclusive with {@link #fromConfigmap}.
     */
    Optional<String> fromSecret();

    /**
     * The optional name of the ConfigMap from which a value is to be extracted.
     * Mutually exclusive with {@link #fromSecret}.
     */
    Optional<String> fromConfigmap();

    /**
     * The key identifying the field from which the value is extracted.
     */
    String withKey();
}
