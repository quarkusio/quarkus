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

package io.quarkus.arc.test.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.inject.spi.DeploymentException;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class InjectionPointMetadataTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(InvalidInjection.class))
            .assertException(t -> {
                assertEquals(DeploymentException.class, t.getClass());
                // There should be only one deployment problem
                assertEquals(DefinitionException.class, t.getCause().getClass());
            });

    @Test
    public void testValidationFailed() {
        // This method should not be invoked
        Assertions.fail();
    }

    @ApplicationScoped
    static class InvalidInjection {

        @Inject
        InjectionPoint injectionPoint;

    }

}
