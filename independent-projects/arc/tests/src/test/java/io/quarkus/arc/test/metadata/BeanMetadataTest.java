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

package org.jboss.quarkus.arc.test.metadata;

import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;

import org.jboss.quarkus.arc.Arc;
import org.jboss.quarkus.arc.ArcContainer;
import org.jboss.quarkus.arc.test.ArcTestContainer;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public class BeanMetadataTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(Controller.class);

    @Test
    public void testBeanMetadata() {
        ArcContainer arc = Arc.container();
        Assert.assertNull(arc.instance(Controller.class).get().bean);
    }

    @SuppressWarnings("serial")
    static class TestLiteral extends AnnotationLiteral<Qualifier> implements Qualifier {

    }

}
