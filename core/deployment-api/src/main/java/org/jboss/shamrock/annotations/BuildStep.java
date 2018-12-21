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

package org.jboss.shamrock.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a given method is a build step that is run at deployment time to
 * create the runtime output.
 *
 * BuildStep's are run concurrently at augmentation time to augment the application. They use a producer/consumer
 * model, where a step is guaranteed not to be run until all items that it is consuming have been created.
 *
 * Producing and consuming is done via injection. This can be done via field injection via {@link javax.inject.Inject},
 * or via method parameter injection.
 *
 * The following types are eligible for injection:
 * - Subclasses of {@link org.jboss.builder.item.SimpleBuildItem}
 * - Lists of subclasses of {@link org.jboss.builder.item.MultiBuildItem}
 * - {@link BuildProducer} instances
 * - Recorder template classes (if the method is annotated {@link Record})
 * - An instance of {@code BytecodeRecorder} (if the method is annotated {@link Record})
 *
 * Injecting a {@code SimpleBuildItem} or a List of {@code MultiBuildItem} makes this step a consumer of
 * these items, and as such will not be run until all producers of the relevant items has been run.
 *
 * Injecting a {@code BuildProducer} makes this class a producer of this item. Alternatively items can be produced
 * by simply returning them from the method.
 *
 * If field injection is used then every {@code BuildStep} method on the class will be a producer/consumer of these
 * items, while method parameter injection is specific to an individual build step. In general method parameter injection
 * should be the preferred approach as it is more fine grained.
 *
 * Note that a {@code BuildStep}will only be run if there is a consumer for items it produces. If nothing is
 * interested in the produced item then it will not be run. A consequence of this is that it must be capable of producing
 * at least one item (it does not actually have to produce anything, but it must have the ability to). A build step that
 * cannot produce anything will never be run.
 *
 * {@code BuildItem}instances must be immutable, as the producer/consumer model does not allow for mutating
 * artifacts. Injecting a build item and modifying it is a bug waiting to happen, as this operation would not be accounted
 * for in the dependency graph.
 *
 * As injection is done by an annotation processor and not reflection injected fields must not be private. In addition
 * to this {@code BuildStep} methods must also not be private.
 *
 *
 * @see Record
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface BuildStep {

    /**
     *
     * A list of capabilitites that are provided by this build step.
     *
     * @return The capabilitities provided by this build step
     */
    String[] providesCapabilities() default {};

    /**
     * Indicates that the provided file names should be considered to be application index markers
     *
     * If these are present in library on the class path then the library will be indexed, and this index will be
     * used when evaluating application components
     *
     * TODO: this this be a different annotation?
     */
    String[] applicationArchiveMarkers() default {};
}
