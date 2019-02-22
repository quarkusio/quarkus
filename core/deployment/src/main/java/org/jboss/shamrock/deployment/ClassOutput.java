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

package io.quarkus.deployment;

import java.io.IOException;

/**
 * Interface that represents a target for generated bytecode
 */
public interface ClassOutput {

    /**
     * Writes some generate bytecode to an output target
     *
     * @param className The class name
     * @param data The bytecode bytes
     * @throws IOException If the class cannot be written
     */
    void writeClass(boolean applicationClass, String className, byte[] data) throws IOException;

    void writeResource(String name, byte[] data) throws IOException;

    //TODO: we should not need both these classes
    static org.jboss.protean.gizmo.ClassOutput gizmoAdaptor(ClassOutput out, boolean applicationClass) {
        return new org.jboss.protean.gizmo.ClassOutput() {
            @Override
            public void write(String name, byte[] data) {
                try {
                    out.writeClass(applicationClass, name, data);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
}
