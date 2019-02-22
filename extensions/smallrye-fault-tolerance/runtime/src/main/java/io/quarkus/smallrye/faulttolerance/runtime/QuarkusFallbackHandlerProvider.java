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

package io.quarkus.smallrye.faulttolerance.runtime;

import javax.annotation.Priority;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;

import io.smallrye.faulttolerance.FallbackHandlerProvider;
import io.smallrye.faulttolerance.config.FallbackConfig;
import io.smallrye.faulttolerance.config.FaultToleranceOperation;

@Dependent
@Alternative
@Priority(1)
public class QuarkusFallbackHandlerProvider implements FallbackHandlerProvider {

    @Inject
    @Any
    Instance<Object> instance;

    @Override
    public <T> FallbackHandler<T> get(FaultToleranceOperation operation) {
        if (operation.hasFallback()) {
            return new FallbackHandler<T>() {
                @SuppressWarnings("unchecked")
                @Override
                public T handle(ExecutionContext context) {
                    Class<?> clazz = operation.getFallback().get(FallbackConfig.VALUE);
                    FallbackHandler<T> fallbackHandlerInstance = (FallbackHandler<T>) instance.select(clazz).get();
                    try {
                        return fallbackHandlerInstance.handle(context);
                    } finally {
                        // The instance exists to service a single invocation only
                        instance.destroy(fallbackHandlerInstance);
                    }
                }
            };
        }
        return null;
    }

}
