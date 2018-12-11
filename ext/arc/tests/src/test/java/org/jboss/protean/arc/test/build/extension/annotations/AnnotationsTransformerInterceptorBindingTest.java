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

package org.jboss.protean.arc.test.build.extension.annotations;

import static org.junit.Assert.assertEquals;

import javax.enterprise.context.Dependent;

import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.protean.arc.Arc;
import org.jboss.protean.arc.processor.AnnotationsTransformer;
import org.jboss.protean.arc.test.ArcTestContainer;
import org.junit.Rule;
import org.junit.Test;

public class AnnotationsTransformerInterceptorBindingTest {

    @Rule
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(IWantToBeIntercepted.class, Simple.class, SimpleInterceptor.class)
            .annotationsTransformers(new SimpleTransformer())
            .build();

    @Test
    public void testInterception() {
        IWantToBeIntercepted wantToBeIntercepted = Arc.container()
                .instance(IWantToBeIntercepted.class)
                .get();
        assertEquals(10, wantToBeIntercepted.size());
    }

    static class SimpleTransformer implements AnnotationsTransformer {

        @Override
        public boolean appliesTo(Kind kind) {
            return kind == Kind.METHOD;
        }

        @Override
        public void transform(TransformationContext context) {
            if (context.isMethod() && context.getTarget()
                    .asMethod()
                    .name()
                    .equals("size")) {
                context.transform().add(Simple.class).done();
            }
        }

    }

    @Dependent
    static class IWantToBeIntercepted {

        // => add @Simple here
        public int size() {
            return 0;
        }

    }

}
