package io.quarkus.creator.outcome.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

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
public class SimplePhaseSequenceTestCase {

    public static class Input extends TestResult {
        public Input(String text) {
            super(text);
        }
    }

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

    @Test
    public void testMain() throws Exception {

        final OutcomeResolver<OutcomeMap> router = OutcomeResolverFactory.<OutcomeMap> getInstance()
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
                .addProvider(new OutcomeProvider<OutcomeMap>() {
                    @Override
                    public void register(OutcomeProviderRegistration registration) throws AppCreatorException {
                        registration.provides(TestResult.class);
                    }

                    @Override
                    public void provideOutcome(OutcomeMap ctx) throws AppCreatorException {
                        final PhaseOutcome2 test2 = ctx.resolveOutcome(PhaseOutcome2.class);
                        ctx.pushOutcome(new TestResult(test2.text + ">result"));
                    }
                })
                .addProvider(new OutcomeProvider<OutcomeMap>() {
                    @Override
                    public void register(OutcomeProviderRegistration registration) throws AppCreatorException {
                        registration.provides(PhaseOutcome1.class);
                    }

                    @Override
                    public void provideOutcome(OutcomeMap ctx) throws AppCreatorException {
                        final Input input = ctx.resolveOutcome(Input.class);
                        ctx.pushOutcome(new PhaseOutcome1(input.text + ">phase1"));
                    }
                })
                .build();

        final OutcomeMap resolver = new OutcomeMap(router);
        resolver.pushOutcome(new Input("input"));
        assertEquals(new TestResult("input>phase1>phase2>result"), resolver.resolveOutcome(TestResult.class));
    }
}
