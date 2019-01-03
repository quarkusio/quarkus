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

package org.jboss.shamrock.example.reactive;

import io.reactivex.Flowable;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/reactive")
public class ReactiveStreamOpsResource {

    @GET
    public String foo() {
        return "hello";
    }

    @GET
    @Path("/stream-regular")
    public String stream1() {
        StringBuilder builder = new StringBuilder();
        ReactiveStreams.of("a", "b", "c")
                .map(String::toUpperCase)
                .forEach(builder::append)
                .run();
        return builder.toString();
    }

    @GET
    @Path("/stream-rx")
    public String stream2() {
        StringBuilder builder = new StringBuilder();
        ReactiveStreams.fromPublisher(Flowable.fromArray("d", "e", "f"))
                .map(String::toUpperCase)
                .forEach(builder::append)
                .run();
        return builder.toString();
    }

}
