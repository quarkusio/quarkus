/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
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

package org.jboss.logmanager.formatters;

/**
 * This is a throwaway temp class.
 */
final class ColorUtil {
    private ColorUtil() {}

    static StringBuilder startFgColor(StringBuilder target, boolean trueColor, int r, int g, int b) {
        return startColor(target, 38, trueColor, r, g, b);
    }

    static StringBuilder startBgColor(StringBuilder target, boolean trueColor, int r, int g, int b) {
        return startColor(target, 48, trueColor, r, g, b);
    }

    static StringBuilder startColor(StringBuilder target, int mode, boolean trueColor, int r, int g, int b) {
        if (trueColor) {
            return target.appendCodePoint(27).append('[').append(mode).append(';').append(2).append(';').append(clip(r)).append(';').append(clip(g)).append(';').append(clip(b)).append('m');
        } else {
            int ar = (5 * clip(r)) / 255;
            int ag = (5 * clip(g)) / 255;
            int ab = (5 * clip(b)) / 255;
            int col = 16 + 36 * ar + 6 * ag + ab;
            return target.appendCodePoint(27).append('[').append(mode).append(';').append('5').append(';').append(col).append('m');
        }
    }

    private static int clip(int color) {
        return Math.min(Math.max(0, color), 255);
    }

    static StringBuilder endFgColor(StringBuilder target) {
        return endColor(target, 39);
    }

    static StringBuilder endBgColor(StringBuilder target) {
        return endColor(target, 49);
    }

    static StringBuilder endColor(StringBuilder target, int mode) {
        return target.appendCodePoint(27).append('[').append(mode).append('m');
    }
}
