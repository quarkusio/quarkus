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

package io.quarkus.runtime;

import java.lang.reflect.Method;

/**
 * The main entry point class, calling main allows you to bootstrap quarkus
 * <p>
 * Note that at native image generation time this is replaced by {@link io.quarkus.runtime.graal.QuarkusReplacement}
 * which will avoid the need for reflection.
 * <p>
 * TODO: how do we deal with static init
 */
public class Quarkus {

    public static void main(String... args) throws Exception {
        Class main = Class.forName("io.quarkus.runner.GeneratedMain");
        Method mainMethod = main.getDeclaredMethod("main", String[].class);
        mainMethod.invoke(null, (Object) args);
    }
}
