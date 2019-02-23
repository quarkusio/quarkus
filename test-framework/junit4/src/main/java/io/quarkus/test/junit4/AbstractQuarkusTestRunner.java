/*
 * Copyright 2019 Red Hat, Inc.
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

package io.quarkus.test.junit4;

import java.util.function.BiFunction;

import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

import io.quarkus.test.common.http.TestHttpResourceManager;

abstract class AbstractQuarkusTestRunner extends BlockJUnit4ClassRunner {

    private static boolean first = true;
    private static AbstractQuarkusRunListener quarkusRunListener;

    private final BiFunction<Class<?>, RunNotifier, AbstractQuarkusRunListener> quarkusRunListenerSupplier;

    public AbstractQuarkusTestRunner(Class<?> klass,
            BiFunction<Class<?>, RunNotifier, AbstractQuarkusRunListener> quarkusRunListenerSupplier)
            throws InitializationError {
        super(klass);
        this.quarkusRunListenerSupplier = quarkusRunListenerSupplier;
    }

    @Override
    public void run(final RunNotifier notifier) {
        if (first) {
            first = false;
            quarkusRunListener = quarkusRunListenerSupplier.apply(getTestClass().getJavaClass(), notifier);
            notifier.addListener(quarkusRunListener);
        }

        super.run(notifier);
    }

    @Override
    protected void runChild(final FrameworkMethod method, RunNotifier notifier) {
        if (!quarkusRunListener.isFailed()) {
            super.runChild(method, notifier);
        } else {
            notifier.fireTestIgnored(describeChild(method));
        }
    }

    @Override
    protected Object createTest() throws Exception {
        Object instance = super.createTest();
        TestHttpResourceManager.inject(instance);
        return instance;
    }
}
