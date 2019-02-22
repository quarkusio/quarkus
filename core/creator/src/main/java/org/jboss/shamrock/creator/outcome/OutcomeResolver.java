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

import java.util.Collections;
import java.util.Map;

import io.quarkus.creator.AppCreatorException;

/**
 *
 * @author Alexey Loubyansky
 */
public class OutcomeResolver<C> {

    private final Map<Class<?>, OutcomeProviderDescription<C>> outcomeProviders;

    protected OutcomeResolver(OutcomeResolverFactory<C> factory) {
        outcomeProviders = Collections.unmodifiableMap(factory.providers);
    }

    public void resolve(C ctx, Class<?> outcomeType) throws AppCreatorException {
        final OutcomeProviderDescription<C> phaseDescr = outcomeProviders.get(outcomeType);
        if (phaseDescr == null) {
            throw new AppCreatorException(Errors.noProviderForOutcome(outcomeType));
        }
        if (phaseDescr.isFlagOn(OutcomeProviderDescription.PROCESSED)) {
            throw new AppCreatorException(Errors.promisedOutcomeNotProvided(phaseDescr.provider, outcomeType));
        }
        if (!phaseDescr.setFlag(OutcomeProviderDescription.PROCESSING)) {
            throw new AppCreatorException(Errors.circularPhaseDependency(outcomeType, phaseDescr.provider));
        }
        phaseDescr.provider.provideOutcome(ctx);
        phaseDescr.clearFlag(OutcomeProviderDescription.PROCESSING);
        if (!phaseDescr.setFlag(OutcomeProviderDescription.PROCESSED)) {
            throw new AppCreatorException(
                    "Race condition: failed to mark provider " + phaseDescr.provider.getClass().getName() + " as processed");
        }
    }
}
