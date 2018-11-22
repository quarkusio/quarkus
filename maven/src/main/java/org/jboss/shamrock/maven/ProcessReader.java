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

package org.jboss.shamrock.maven;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class ProcessReader implements Runnable {

    private final InputStream inputStream;
    private final boolean error;

    public ProcessReader(InputStream inputStream, boolean error) {
        this.inputStream = inputStream;
        this.error = error;
    }

    @Override
    public void run() {
        byte[] buf = new byte[100];
        int i;
        try {
            while ((i = inputStream.read(buf)) > 0) {
                String charSequence = new String(buf, 0, i, StandardCharsets.UTF_8);
                if (error) {
                    System.err.print(charSequence);
                } else {
                    System.out.print(charSequence);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
