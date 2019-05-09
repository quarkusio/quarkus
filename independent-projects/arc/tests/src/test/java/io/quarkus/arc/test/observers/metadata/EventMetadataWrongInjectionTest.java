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

package io.quarkus.arc.test.observers.metadata;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.quarkus.arc.test.ArcTestContainer;
import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.inject.spi.DeploymentException;
import javax.enterprise.inject.spi.EventMetadata;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.junit.Rule;
import org.junit.Test;

public class EventMetadataWrongInjectionTest {

    @Rule
    public ArcTestContainer container = ArcTestContainer.builder().beanClasses(WrongBean.class).shouldFail().build();

    @Test
    public void testMetadata() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertTrue(error instanceof DeploymentException);
        assertNotNull(error.getCause());
        assertTrue(error.getCause() instanceof DefinitionException);
    }

    @Singleton
    static class WrongBean {

        @Inject
        EventMetadata metadata;

    }

}
