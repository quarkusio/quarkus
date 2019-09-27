package io.quarkus.creator.outcome.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

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
public class CircularDependencyTestCase {

    public static class PhaseOutcome1 extends TestResult {
        public PhaseOutcome1(String text) {
            super(text);
        }
    }

    public static class PhaseOutcome2 extends TestResult {
        public PhaseOutcome2(String text) {
            super(text);
        }
    }

    public static class PhaseOutcome3 extends TestResult {
        public PhaseOutcome3(String text) {
            super(text);
        }
    }

    @Test
    public void testMain() throws Exception {

        final OutcomeProvider<OutcomeMap> phase3 = new OutcomeProvider<OutcomeMap>() {
            @Override
            public void register(OutcomeProviderRegistration registration) throws AppCreatorException {
                registration.provides(PhaseOutcome3.class);
            }

            @Override
            public void provideOutcome(OutcomeMap ctx) throws AppCreatorException {
                final PhaseOutcome2 test2 = ctx.resolveOutcome(PhaseOutcome2.class);
                ctx.pushOutcome(new PhaseOutcome3(test2.text + ">phase3"));
            }
        };

        final OutcomeResolver<OutcomeMap> router = OutcomeResolverFactory.<OutcomeMap> getInstance()
                .addProvider(new OutcomeProvider<OutcomeMap>() {
                    @Override
                    public void register(OutcomeProviderRegistration registration) throws AppCreatorException {
                        registration.provides(PhaseOutcome1.class);
                    }

                    @Override
                    public void provideOutcome(OutcomeMap ctx) throws AppCreatorException {
                        final PhaseOutcome3 input = ctx.resolveOutcome(PhaseOutcome3.class);
                        ctx.pushOutcome(new PhaseOutcome1(input.text + ">phase1"));
                    }
                })
                .addProvider(new OutcomeProvider<OutcomeMap>() {
                    @Override
                    public void register(OutcomeProviderRegistration registration) throws AppCreatorException {
                        registration.provides(PhaseOutcome2.class);
                    }

                    @Override
                    public void provideOutcome(OutcomeMap ctx) throws AppCreatorException {
                        final PhaseOutcome1 test1 = ctx.resolveOutcome(PhaseOutcome1.class);
                        ctx.pushOutcome(new PhaseOutcome2(test1.text + ">phase2"));
                    }
                })
                .addProvider(phase3)
                .addProvider(new OutcomeProvider<OutcomeMap>() {
                    @Override
                    public void register(OutcomeProviderRegistration registration) throws AppCreatorException {
                        registration.provides(TestResult.class);
                    }

                    @Override
                    public void provideOutcome(OutcomeMap ctx) throws AppCreatorException {
                        final PhaseOutcome3 test3 = ctx.resolveOutcome(PhaseOutcome3.class);
                        ctx.pushOutcome(new TestResult(test3.text + ">result"));
                    }
                })
                .build();

        final OutcomeMap resolver = new OutcomeMap(router);
        try {
            resolver.resolveOutcome(TestResult.class);
            fail();
        } catch (AppCreatorException e) {
            assertEquals(Errors.circularPhaseDependency(PhaseOutcome3.class, phase3), e.getMessage());
        }
    }
}
