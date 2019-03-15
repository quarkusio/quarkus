/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.arc.test.contexts.application;

import static org.junit.Assert.assertTrue;

import io.quarkus.arc.test.ArcTestContainer;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.BeforeDestroyed;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class ApplicationInitializedTest {

    @Rule
    public RuleChain chain = RuleChain.outerRule(new TestRule() {
        @Override
        public Statement apply(final Statement base, Description description) {
            return new Statement() {

                @Override
                public void evaluate() throws Throwable {
                    base.evaluate();
                    assertTrue(Observer.DESTROYED.get());
                    assertTrue(Observer.BEFORE_DESTROYED.get());
                }
            };
        }
    }).around(new ArcTestContainer(Observer.class));

    @Test
    public void testEventWasFired() {
        Assert.assertTrue(Observer.INITIALIZED.get());
    }

    @Dependent
    static class Observer {

        static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);
        static final AtomicBoolean DESTROYED = new AtomicBoolean(false);
        static final AtomicBoolean BEFORE_DESTROYED = new AtomicBoolean(false);

        void onStart(@Observes @Initialized(ApplicationScoped.class) Object container) {
            INITIALIZED.set(true);
        }

        void beforeDestroyed(@Observes @BeforeDestroyed(ApplicationScoped.class) Object container) {
            BEFORE_DESTROYED.set(true);
        }

        void destroyed(@Observes @Destroyed(ApplicationScoped.class) Object container) {
            DESTROYED.set(true);
        }

    }
}
