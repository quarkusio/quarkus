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

package io.quarkus.arc.test.resolution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.AbstractList;
import java.util.List;

import javax.enterprise.util.TypeLiteral;
import javax.inject.Singleton;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;
import org.junit.Rule;
import org.junit.Test;

public class RuntimeResolutionTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(MyList.class);

    @SuppressWarnings("serial")
    @Test
    public void testResolution() throws IOException {
        ArcContainer arc = Arc.container();
        // MyList bean types: MyList, AbstractList<Integer>, List<Integer>, AbstractCollection<Integer>, Iterable<Integer>, Object
        InstanceHandle<List<? extends Number>> list = arc.instance(new TypeLiteral<List<? extends Number>>() {
        });
        assertTrue(list.isAvailable());
        assertEquals(Integer.valueOf(7), list.get().get(1));
    }

    @Singleton
    static class MyList extends AbstractList<Integer> {

        @Override
        public Integer get(int index) {
            return Integer.valueOf(7);
        }

        @Override
        public int size() {
            return 0;
        }

    }

}
