/*******************************************************************************
 * Copyright (c) 2023 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package io.quarkus.qute.debug.client;

/**
 * Language server trace level used to show the LSP requests/responses/notifications in the LSP console.
 */
public enum ServerTrace {
    off, // don't show any messages
    messages, // show only message without detail
    verbose; // show message with detail

    public static ServerTrace getDefaultValue() {
        // Return verbose here to enable DAP trace output in the console.
        return off;
    }

    public static ServerTrace get(String value) {
        try {
            return ServerTrace.valueOf(value);
        } catch (Exception e) {
            return getDefaultValue();
        }
    }
}
