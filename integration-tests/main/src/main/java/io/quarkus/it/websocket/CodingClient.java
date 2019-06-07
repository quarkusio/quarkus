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
package io.quarkus.it.websocket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

import javax.websocket.ClientEndpoint;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

@ClientEndpoint(decoders = ClientDtoDecoder.class, encoders = ClientDtoEncoder.class)
public class CodingClient {
    private static List<Session> sessions = Collections.synchronizedList(new ArrayList<>());

    static LinkedBlockingDeque<Dto> messageQueue = new LinkedBlockingDeque<>();

    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);

        Dto data = new Dto();
        data.setContent("initial data");
        session.getAsyncRemote().sendObject(data);
    }

    @OnMessage
    public void onMessage(Dto message) {
        messageQueue.add(message);
        close();
    }

    static void close() {
        for (Session session : sessions) {
            try {
                session.close();
            } catch (IOException ignored) {
            }
        }

    }
}
