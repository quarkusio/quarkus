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

package org.jboss.protean.arc.processor;

import static org.jboss.protean.arc.processor.Basics.index;
import static org.jboss.protean.arc.processor.Basics.name;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.jboss.protean.arc.processor.types.Bar;
import org.jboss.protean.arc.processor.types.Foo;
import org.jboss.protean.arc.processor.types.FooQualifier;
import org.junit.Test;

/**
 *
 * @author Martin Kouba
 */
public class BeanInfoTypesTest {

    @Test
    public void testResolver() throws IOException {

        Index index = index(Foo.class, Bar.class, FooQualifier.class, AbstractList.class, AbstractCollection.class, Collection.class, List.class,
                Iterable.class);

        BeanDeployment deployment = new BeanDeployment(index, null, null);
        DotName fooName = name(Foo.class);

        ClassInfo fooClass = index.getClassByName(fooName);
        BeanInfo fooBean = Beans.createClassBean(fooClass, deployment);
        Set<Type> types = fooBean.getTypes();
        // Foo, AbstractList<String>, AbstractCollection<String>, List<String>, Collection<String>, Iterable<String>
        assertEquals(6, types.size());
        assertTrue(types.contains(Type.create(fooName, Kind.CLASS)));
        assertTrue(types.contains(ParameterizedType.create(name(AbstractList.class), new Type[] { Type.create(name(String.class), Kind.CLASS) }, null)));
        assertTrue(types.contains(ParameterizedType.create(name(List.class), new Type[] { Type.create(name(String.class), Kind.CLASS) }, null)));
        assertTrue(types.contains(ParameterizedType.create(name(Collection.class), new Type[] { Type.create(name(String.class), Kind.CLASS) }, null)));
        assertTrue(types.contains(ParameterizedType.create(name(AbstractCollection.class), new Type[] { Type.create(name(String.class), Kind.CLASS) }, null)));
        assertTrue(types.contains(ParameterizedType.create(name(Iterable.class), new Type[] { Type.create(name(String.class), Kind.CLASS) }, null)));

    }

}
