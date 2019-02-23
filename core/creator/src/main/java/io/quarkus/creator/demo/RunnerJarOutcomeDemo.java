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

package io.quarkus.creator.demo;

import io.quarkus.creator.AppCreator;
import io.quarkus.creator.phase.runnerjar.RunnerJarOutcome;

/**
 *
 * @author Alexey Loubyansky
 */
public class RunnerJarOutcomeDemo extends ConfigDemoBase {

    public static void main(String[] args) throws Exception {
        new RunnerJarOutcomeDemo().run();
    }

    @Override
    public void demo(AppCreator creator) throws Exception {
        creator.resolveOutcome(RunnerJarOutcome.class);
    }
}
