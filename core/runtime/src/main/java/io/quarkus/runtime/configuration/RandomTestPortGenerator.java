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

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashSet;
import java.util.Set;

public class RandomTestPortGenerator {

    private static Set<Integer> used = new HashSet<>();
    private static final int MAX_ATTEMPTS = 100;

    /**
     * Generates a random private port for testing purposes.
     * 
     * @return Random private.
     */
    public static int generate() {
        synchronized (RandomTestPortGenerator.class) {
            int numberOfAttempts = 0;
            int port;
            do {

                try {
                    port = getRandomPrivatePort();
                } catch (IOException e) {
                    numberOfAttempts++;
                    if (numberOfAttempts > MAX_ATTEMPTS) {
                        throw new IllegalStateException(
                                String.format("After %d attempts, it has not been possible to allocate a free ephimeral port.",
                                        e));
                    }
                    port = -1;
                }

            } while (port < 0 || used.contains(port));

            used.add(port);
            return port;
        }
    }

    private static int getRandomPrivatePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }
}
