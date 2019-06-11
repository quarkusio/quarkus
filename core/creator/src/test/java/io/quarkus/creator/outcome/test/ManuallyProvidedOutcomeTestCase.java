package io.quarkus.creator.outcome.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.quarkus.creator.AppCreatorException;
import io.quarkus.creator.outcome.OutcomeMap;
import io.quarkus.creator.outcome.OutcomeProvider;
import io.quarkus.creator.outcome.OutcomeProviderRegistration;
import io.quarkus.creator.outcome.OutcomeResolver;
import io.quarkus.creator.outcome.OutcomeResolverFactory;

/**
 *
 * @author Alexey Loubyansky
 */
public class ManuallyProvidedOutcomeTestCase {

    @Test
    public void testNoHandler() throws Exception {
        final OutcomeResolver<OutcomeMap> router = OutcomeResolverFactory.<OutcomeMap> getInstance().build();
        final OutcomeMap resolver = new OutcomeMap(router);
        final TestResult outcome = new TestResult("manual");
        resolver.pushOutcome(outcome);
        assertEquals(outcome, resolver.resolveOutcome(TestResult.class));
    }

    @Test
    public void testSkippedHandler() throws Exception {
        final OutcomeResolver<OutcomeMap> router = OutcomeResolverFactory.<OutcomeMap> getInstance()
                .addProvider(new OutcomeProvider<OutcomeMap>() {
                    @Override
                    public void register(OutcomeProviderRegistration registration) throws AppCreatorException {
                        registration.provides(TestResult.class);
                    }

                    @Override
                    public void provideOutcome(OutcomeMap ctx) throws AppCreatorException {
                        throw new UnsupportedOperationException();
                    }
                })
                .build();

        final OutcomeMap resolver = new OutcomeMap(router);
        final TestResult outcome = new TestResult("manual");
        resolver.pushOutcome(outcome);
        assertEquals(outcome, resolver.resolveOutcome(TestResult.class));
    }
}
