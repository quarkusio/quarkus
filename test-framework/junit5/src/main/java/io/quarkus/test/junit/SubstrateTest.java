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

package io.quarkus.test.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Annotation that indicates that this test should be run using a native image,
 * rather than in the JVM. This must also be combined with {@link QuarkusTestExtension}.
 *
 * The standard usage pattern is expected to be a base test class that runs the
 * tests using the JVM version of Quarkus, with a subclass that extends the base
 * test and is annotated with this annotation to perform the same checks against
 * the native image.
 *
 * Note that it is not possible to mix JVM and native image tests in the same test
 * run, it is expected that the JVM tests will be standard unit tests that are
 * executed by surefire, while the native image tests will be integration tests
 * executed by failsafe.
 *
 */
@Target(ElementType.TYPE)
@ExtendWith({ QuarkusTestExtension.class, DisabledOnSubstrateCondition.class })
@Retention(RetentionPolicy.RUNTIME)
public @interface SubstrateTest {
}
