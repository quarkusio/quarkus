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

package io.quarkus.test.h2;

import java.sql.SQLException;

import org.h2.tools.Server;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class H2DatabaseTestResource implements QuarkusTestResourceLifecycleManager {

    private Server tcpServer;

    @Override
    public void start() {

        try {
            tcpServer = Server.createTcpServer();
            tcpServer.start();
            System.out.println("[INFO] H2 database started in TCP server mode");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized void stop() {
        if (tcpServer != null) {
            tcpServer.stop();
            System.out.println("[INFO] H2 database was shut down");
            tcpServer = null;
        }
    }
}
