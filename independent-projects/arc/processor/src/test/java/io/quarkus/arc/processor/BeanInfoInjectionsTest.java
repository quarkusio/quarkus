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

package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.Basics.index;
import static io.quarkus.arc.processor.Basics.name;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.Collection;
import java.util.List;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import io.quarkus.arc.processor.types.Bar;
import io.quarkus.arc.processor.types.Foo;
import io.quarkus.arc.processor.types.FooQualifier;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Martin Kouba
 */
public class BeanInfoInjectionsTest {

    @Test
    public void testInjections() throws IOException {

        Index index = index(Foo.class, Bar.class, FooQualifier.class, AbstractList.class, AbstractCollection.class, Collection.class, List.class,
                Iterable.class);
        DotName barName = name(Bar.class);
        ClassInfo barClass = index.getClassByName(barName);
        Type fooType = Type.create(name(Foo.class), Kind.CLASS);
        Type listStringType = ParameterizedType.create(name(List.class), new Type[] { Type.create(name(String.class), Kind.CLASS) }, null);

        BeanDeployment deployment = new BeanDeployment(index, null, null);
        BeanInfo barBean = deployment.getBeans().stream().filter(b -> b.getTarget().get().equals(barClass)).findFirst().get();
        List<Injection> injections = barBean.getInjections();
        assertEquals(3, injections.size());
        for (Injection injection : injections) {
            if (injection.target.kind().equals(org.jboss.jandex.AnnotationTarget.Kind.FIELD) && injection.target.asField().name().equals("foo")) {
                assertEquals(1, injection.injectionPoints.size());
                assertEquals(fooType, injection.injectionPoints.get(0).getRequiredType());
                assertEquals(1, injection.injectionPoints.get(0).getRequiredQualifiers().size());
            } else if (injection.target.kind().equals(org.jboss.jandex.AnnotationTarget.Kind.METHOD) && injection.target.asMethod().name().equals("<init>")) {
                // Constructor
                assertEquals(2, injection.injectionPoints.size());
                assertEquals(listStringType, injection.injectionPoints.get(0).getRequiredType());
                assertEquals(fooType, injection.injectionPoints.get(1).getRequiredType());
                assertEquals(1, injection.injectionPoints.get(1).getRequiredQualifiers().size());
            } else if (injection.target.kind().equals(org.jboss.jandex.AnnotationTarget.Kind.METHOD) && injection.target.asMethod().name().equals("init")) {
                // Initializer
                assertEquals(2, injection.injectionPoints.size());
                assertEquals(listStringType, injection.injectionPoints.get(1).getRequiredType());
                assertEquals(fooType, injection.injectionPoints.get(0).getRequiredType());
            } else {
                Assert.fail();
            }

        }

    }

}
