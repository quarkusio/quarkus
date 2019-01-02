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

package org.jboss.logmanager.formatters;

import static org.jboss.logmanager.formatters.ColorPatternFormatter.isTrueColor;

import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.util.Formattable;
import java.util.Locale;
import java.util.UUID;

import org.wildfly.common.format.GeneralFlags;
import org.wildfly.common.format.NumericFlags;
import org.wildfly.common.format.Printf;

/**
 */
class ColorPrintf extends Printf {
    ColorPrintf() {
        super(Locale.getDefault());
    }

    public StringBuilder formatDirect(final StringBuilder destination, final String format, final Object... params) {
        restoreColor(destination);
        super.formatDirect(destination, format, params);
        ColorUtil.endFgColor(destination);
        return destination;
    }

    private static void restoreColor(final StringBuilder destination) {
        ColorUtil.startFgColor(destination, isTrueColor(), 0xff, 0xff, 0xff);
    }

    protected void formatTimeTextField(final StringBuilder target, final TemporalAccessor ta, final TemporalField field, final String[] symbols, final GeneralFlags genFlags, final int width) {
        super.formatTimeTextField(target, ta, field, symbols, genFlags, width);
    }

    protected void formatTimeZoneId(final StringBuilder target, final TemporalAccessor ta, final GeneralFlags genFlags, final int width) {
        super.formatTimeZoneId(target, ta, genFlags, width);
    }

    protected void formatTimeZoneOffset(final StringBuilder target, final TemporalAccessor ta, final GeneralFlags genFlags, final int width) {
        super.formatTimeZoneOffset(target, ta, genFlags, width);
    }

    protected void formatTimeField(final StringBuilder target, final TemporalAccessor ta, final TemporalField field, final GeneralFlags genFlags, final int width, final int zeroPad) {
        super.formatTimeField(target, ta, field, genFlags, width, zeroPad);
    }


    protected void formatPercent(final StringBuilder target) {
        super.formatPercent(target);
    }

    protected void formatLineSeparator(final StringBuilder target) {
        super.formatLineSeparator(target);
    }

    protected void formatFormattableString(final StringBuilder target, final Formattable formattable, final GeneralFlags genFlags, final int width, final int precision) {
        super.formatFormattableString(target, formattable, genFlags, width, precision);
    }

    protected void formatPlainString(final StringBuilder target, final Object item, final GeneralFlags genFlags, final int width, final int precision) {
        if (item instanceof Class || item instanceof Executable || item instanceof Field) {
            ColorUtil.startFgColor(target, isTrueColor(), 0xff, 0xff, 0xdd);
        } else if (item instanceof UUID) {
            ColorUtil.startFgColor(target, isTrueColor(), 0xdd, 0xff, 0xdd);
        } else {
            ColorUtil.startFgColor(target, isTrueColor(), 0xdd, 0xdd, 0xdd);
        }
        super.formatPlainString(target, item, genFlags, width, precision);
        restoreColor(target);
    }

    protected void formatBoolean(final StringBuilder target, final Object item, final GeneralFlags genFlags, final int width, final int precision) {
        super.formatBoolean(target, item, genFlags, width, precision);
    }

    protected void formatHashCode(final StringBuilder target, final Object item, final GeneralFlags genFlags, final int width, final int precision) {
        super.formatHashCode(target, item, genFlags, width, precision);
    }

    protected void formatCharacter(final StringBuilder target, final int codePoint, final GeneralFlags genFlags, final int width, final int precision) {
        super.formatCharacter(target, codePoint, genFlags, width, precision);
    }

    protected void formatDecimalInteger(final StringBuilder target, final Number item, final GeneralFlags genFlags, final NumericFlags numFlags, final int width) {
        super.formatDecimalInteger(target, item, genFlags, numFlags, width);
    }

    protected void formatOctalInteger(final StringBuilder target, final Number item, final GeneralFlags genFlags, final NumericFlags numFlags, final int width) {
        super.formatOctalInteger(target, item, genFlags, numFlags, width);
    }

    protected void formatHexInteger(final StringBuilder target, final Number item, final GeneralFlags genFlags, final NumericFlags numFlags, final int width) {
        super.formatHexInteger(target, item, genFlags, numFlags, width);
    }

    protected void formatFloatingPointSci(final StringBuilder target, final Number item, final GeneralFlags genFlags, final NumericFlags numFlags, final int width, final int precision) {
        super.formatFloatingPointSci(target, item, genFlags, numFlags, width, precision);
    }

    protected void formatFloatingPointDecimal(final StringBuilder target, final Number item, final GeneralFlags genFlags, final NumericFlags numFlags, final int width, final int precision) {
        super.formatFloatingPointDecimal(target, item, genFlags, numFlags, width, precision);
    }

    protected void formatFloatingPointGeneral(final StringBuilder target, final Number item, final GeneralFlags genFlags, final NumericFlags numFlags, final int width, final int precision) {
        super.formatFloatingPointGeneral(target, item, genFlags, numFlags, width, precision);
    }
}
