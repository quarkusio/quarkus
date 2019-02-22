/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.creator.outcome;

import io.quarkus.creator.AppCreatorException;

/**
 * Phase registration callback.
 *
 * Allows phase handlers to declare what they consume and what they provide.
 *
 * @author Alexey Loubyansky
 */
public interface OutcomeProviderRegistration {

    /**
     * Invoked by a phase handler to declare it provides an outcome
     * of a specific type.
     *
     * @param outcomeType outcome type the handler provides
     * @throws AppCreatorException in case of a failure
     */
    void provides(Class<?> outcomeType) throws AppCreatorException;
}
