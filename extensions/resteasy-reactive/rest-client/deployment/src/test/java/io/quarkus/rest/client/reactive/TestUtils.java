/*
 * Copyright (c) 2011-2019 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */

package io.quarkus.rest.client.reactive;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class TestUtils {

    public static String randomAlphaString(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            char c = (char) (65 + 25 * Math.random());
            builder.append(c);
        }
        return builder.toString();
    }

    public static byte[] compressGzip(String source) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPOutputStream gos = new GZIPOutputStream(baos);
        gos.write(source.getBytes());
        gos.close();
        return baos.toByteArray();
    }

    public static byte[] decompressGzip(byte[] source) throws IOException {
        GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(source));
        byte[] result = gis.readAllBytes();
        gis.close();
        return result;
    }
}
