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
 * Phase handler
 *
 * @author Alexey Loubyansky
 */
public interface OutcomeProvider<C> {

    /**
     * Invoked during when a handler is added to the phase router.
     * This method allows to register consumed and provided outcome types.
     *
     * @param registration registration callback
     * @throws AppCreatorException in case of a failure
     */
    void register(OutcomeProviderRegistration registration) throws AppCreatorException;

    /**
     * Invoked by the router to process the phase.
     *
     * @param ctx phase processing context
     * @throws AppCreatorException in case of a failure
     */
    void provideOutcome(C ctx) throws AppCreatorException;
}
