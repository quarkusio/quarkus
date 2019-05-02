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

package io.quarkus.runtime.configuration;

import java.util.Random;

public class RandomTestPortGenerator {

    private static Random random = new Random();

    static final int FIRST_PRIVATE_PORT = 49152;
    static final int LAST_PRIVATE_PORT = 65535;

    /**
     * Generates a random private port for testing purposes.
     * 
     * @return Random port between 49152-65535
     */
    public static int generate() {
        final int offset = random.nextInt(LAST_PRIVATE_PORT - FIRST_PRIVATE_PORT);
        return FIRST_PRIVATE_PORT + offset;
    }

}
