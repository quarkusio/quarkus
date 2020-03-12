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

package io.quarkus.qute.generator;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;

import io.quarkus.gizmo.ClassOutput;

public class TestClassOutput implements ClassOutput {

    @Override
    public void write(String name, byte[] data) {
        try {
            File dir = new File("target/test-classes/", name.substring(0, name.lastIndexOf("/")));
            dir.mkdirs();
            File output = new File("target/test-classes/", name + ".class");
            Files.write(output.toPath(), data);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot dump the class: " + name, e);
        }
    }

    public Writer getSourceWriter(final String className) {
        File dir = new File("target/generated-test-sources/gizmo/", className.substring(0, className.lastIndexOf('/')));
        dir.mkdirs();
        File output = new File("target/generated-test-sources/gizmo/", className + ".zig");
        try {
            return Files.newBufferedWriter(output.toPath());
        } catch (IOException e) {
            throw new IllegalStateException("Cannot write .zig file for " + className, e);
        }
    }

}
