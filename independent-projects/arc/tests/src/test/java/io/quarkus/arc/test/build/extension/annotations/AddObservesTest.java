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

package io.quarkus.arc.test.build.extension.annotations;

import static org.junit.Assert.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.test.ArcTestContainer;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.enterprise.event.Observes;
import javax.inject.Singleton;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.junit.Rule;
import org.junit.Test;

public class AddObservesTest {

    @Rule
    public ArcTestContainer container = ArcTestContainer.builder().beanClasses(IWantToObserve.class)
            .annotationsTransformers(new AnnotationsTransformer() {

                @Override
                public boolean appliesTo(Kind kind) {
                    return Kind.METHOD == kind;
                }

                @Override
                public void transform(TransformationContext transformationContext) {
                    MethodInfo method = transformationContext.getTarget().asMethod();
                    if (method.name().equals("observe")) {
                        transformationContext.transform()
                                .add(AnnotationInstance.create(DotName.createSimple(Observes.class.getName()),
                                        MethodParameterInfo.create(method, (short) 0), new AnnotationValue[] {}))
                                .done();
                    }
                }
            }).build();

    @Test
    public void testObserved() {
        IWantToObserve.OBSERVED.set(false);
        Arc.container().beanManager().getEvent().select(String.class).fire("ok");
        assertTrue(IWantToObserve.OBSERVED.get());
    }

    @Singleton
    static class IWantToObserve {

        static final AtomicBoolean OBSERVED = new AtomicBoolean();

        public void observe(String event) {
            OBSERVED.set(true);
        }

    }

}
