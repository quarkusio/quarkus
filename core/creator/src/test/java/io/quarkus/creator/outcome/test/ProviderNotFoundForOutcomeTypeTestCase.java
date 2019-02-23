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

package io.quarkus.creator.outcome.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import io.quarkus.creator.AppCreatorException;
import io.quarkus.creator.outcome.Errors;
import io.quarkus.creator.outcome.OutcomeMap;
import io.quarkus.creator.outcome.OutcomeProvider;
import io.quarkus.creator.outcome.OutcomeProviderRegistration;
import io.quarkus.creator.outcome.OutcomeResolver;
import io.quarkus.creator.outcome.OutcomeResolverFactory;

/**
 *
 * @author Alexey Loubyansky
 */
public class ProviderNotFoundForOutcomeTypeTestCase {

    @Test
    public void testMain() throws Exception {

        final OutcomeResolver<OutcomeMap> router = OutcomeResolverFactory.<OutcomeMap> getInstance()
                .addProvider(new OutcomeProvider<OutcomeMap>() {
                    @Override
                    public void register(OutcomeProviderRegistration registration) throws AppCreatorException {
                    }

                    @Override
                    public void provideOutcome(OutcomeMap ctx) throws AppCreatorException {
                    }
                })
                .build();

        final OutcomeMap resolver = new OutcomeMap(router);
        try {
            resolver.resolveOutcome(TestResult.class);
            fail();
        } catch (AppCreatorException e) {
            assertEquals(Errors.noProviderForOutcome(TestResult.class), e.getMessage());
        }
    }
}
