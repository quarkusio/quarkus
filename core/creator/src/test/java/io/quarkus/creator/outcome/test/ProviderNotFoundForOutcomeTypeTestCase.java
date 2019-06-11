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
