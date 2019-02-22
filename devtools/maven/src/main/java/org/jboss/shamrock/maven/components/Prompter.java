/*
 *
 *   Copyright (c) 2016-2018 Red Hat, Inc.
 *
 *   Red Hat licenses this file to you under the Apache License, version
 *   2.0 (the "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *   implied.  See the License for the specific language governing
 *   permissions and limitations under the License.
 */
package io.quarkus.maven.components;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.plexus.component.annotations.Component;

import jline.console.ConsoleReader;

/**
 * Prompt implementation.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
@Component(role = Prompter.class, instantiationStrategy = "per-lookup")
public class Prompter {

    private final ConsoleReader console;

    public Prompter() throws IOException {
        this.console = new ConsoleReader();
        console.setHistoryEnabled(false);
        console.setExpandEvents(false);
    }

    public Prompter(InputStream in, OutputStream out) throws IOException {
        this.console = new ConsoleReader(in, out);
        console.setHistoryEnabled(false);
        console.setExpandEvents(false);
    }

    public ConsoleReader getConsole() {
        return console;
    }

    public String prompt(final String message, final Character mask) throws IOException {
        Objects.requireNonNull(message);

        final String prompt = String.format("%s: ", message);
        String value;
        do {
            value = console.readLine(prompt, mask);
        } while (StringUtils.isBlank(value));
        return value;
    }

    public String prompt(final String message) throws IOException {
        Objects.requireNonNull(message);
        return prompt(message, null);
    }

    public String promptWithDefaultValue(final String message, final String defaultValue) throws IOException {
        Objects.requireNonNull(message);
        Objects.requireNonNull(defaultValue);

        final String prompt = String.format("%s [%s]: ", message, defaultValue);
        String value = console.readLine(prompt);
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        return value;
    }

}
