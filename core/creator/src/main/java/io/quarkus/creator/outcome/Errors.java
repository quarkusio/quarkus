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

/**
 *
 * @author Alexey Loubyansky
 */
public class Errors {

    public static String alternativeOutcomeProviders(Class<?> outcomeType, OutcomeProvider<?> phase1,
            OutcomeProvider<?> phase2) {
        return "Outcome of type " + outcomeType.getName() + " is provided by two phases: " + phase1.getClass().getName()
                + " and " + phase2.getClass().getName();
    }

    public static String promisedOutcomeNotProvided(OutcomeProvider<?> handler, Class<?> outcomeType) {
        return "Phase " + handler.getClass().getName() + " has not provided outcome of type " + outcomeType.getName();
    }

    public static String circularPhaseDependency(Class<?> outcomeType, OutcomeProvider<?> handler) {
        return "Failed to resolve outcome of type " + outcomeType.getName()
                + " due to circular phase dependencies originating from phase " + handler.getClass().getName();
    }

    public static String noProviderForOutcome(Class<?> outcomeType) {
        return "No phase found providing outcome of type " + outcomeType.getName();
    }
}
