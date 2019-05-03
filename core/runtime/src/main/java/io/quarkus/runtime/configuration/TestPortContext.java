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

public class TestPortContext {

    private static final ThreadLocal<Integer> randomPort = new ThreadLocal<>();

    /**
     * Generates a random private port for testing purposes.
     * The port is stored in a thread local so it is the same port for each thread.
     *
     * @return Random private port
     */
    public static int getRandomPort() {
        Integer randomPort = TestPortContext.randomPort.get();
        if (randomPort == null) {
            randomPort = RandomTestPortGenerator.generate();
            TestPortContext.randomPort.set(randomPort);
        }

        return randomPort;
    }
}
