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

package io.quarkus.arc;

import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;

public class WeldMain {

    static WeldContainer weld;

    static {
        // This is needed for graal ahead-of-time compilation
        weld = new Weld().initialize();
        // Dynamic class loading is not supported in substratevm
        // We need to generate/load all proxies eagerly
        weld.select(Generator.class).get();
        weld.select(GeneratedStringObserver.class).get();
        weld.select(GeneratedStringProducer.class).get();
    }

    public static void main(String[] args) {
        weld.select(Generator.class).get().run();
        weld.shutdown();
    }

}
