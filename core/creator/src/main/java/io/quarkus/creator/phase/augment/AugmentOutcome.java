/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.creator.phase.augment;

import java.nio.file.Path;

/**
 * Represents an outcome of {@link AugmentPhase}
 *
 * @author Alexey Loubyansky
 */
public interface AugmentOutcome {

    /**
     * Directory containing original user application classes.
     *
     * @return directory containing original user application classes
     */
    Path getAppClassesDir();

    /**
     * Directory containing bytecode-transformed user application classes.
     * Depending on the application, this directory may be empty.
     *
     * @return directory containing transformed user application classes
     */
    Path getTransformedClassesDir();

    /**
     * Directory containing classes generated during augmentation.
     *
     * @return directory containing generated classes
     */
    Path getWiringClassesDir();
}
