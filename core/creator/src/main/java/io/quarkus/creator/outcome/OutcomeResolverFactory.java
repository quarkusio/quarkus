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

import java.util.HashMap;
import java.util.Map;

import io.quarkus.creator.AppCreatorException;

/**
 *
 * @author Alexey Loubyansky
 */
public class OutcomeResolverFactory<C> {

    public static <C> OutcomeResolverFactory<C> getInstance() {
        return new OutcomeResolverFactory<C>();
    }

    private class Registration implements OutcomeProviderRegistration {

        private int phasesTotal;
        OutcomeProviderDescription<C> phaseDescr;

        void register(OutcomeProvider<C> handler) throws AppCreatorException {
            this.phaseDescr = new OutcomeProviderDescription<C>(++phasesTotal, handler);
            handler.register(this);
        }

        @Override
        public void provides(Class<?> type) throws AppCreatorException {
            phaseDescr.addProvidedType(type);
            final OutcomeProviderDescription<C> duplicateDescr = providers.put(type, phaseDescr);
            if (duplicateDescr != null) {
                throw new AppCreatorException(
                        Errors.alternativeOutcomeProviders(type, duplicateDescr.provider, phaseDescr.provider));
            }
        }
    }

    private Registration registration = new Registration();
    Map<Class<?>, OutcomeProviderDescription<C>> providers = new HashMap<>();

    private OutcomeResolverFactory() {
    }

    public OutcomeResolverFactory<C> addProvider(OutcomeProvider<C> phaseHandler) throws AppCreatorException {
        registration.register(phaseHandler);
        return this;
    }

    public OutcomeResolver<C> build() {
        return new OutcomeResolver<C>(this);
    }
}
