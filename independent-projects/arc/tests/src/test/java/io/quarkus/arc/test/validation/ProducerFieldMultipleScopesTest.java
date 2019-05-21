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

package io.quarkus.arc.test.validation;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.quarkus.arc.test.ArcTestContainer;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.DefinitionException;
import org.junit.Rule;
import org.junit.Test;

public class ProducerFieldMultipleScopesTest {

    @Rule
    public ArcTestContainer container = ArcTestContainer.builder().beanClasses(Alpha.class).shouldFail().build();

    @Test
    public void testFailure() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertTrue(error instanceof DefinitionException);
    }

    @Dependent
    static class Alpha {

        @Produces
        @ApplicationScoped
        @RequestScoped
        List<String> produced;

    }

}
