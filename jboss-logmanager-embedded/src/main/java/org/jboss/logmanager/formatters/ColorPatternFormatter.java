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

import static java.lang.Math.abs;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.Level;
import org.wildfly.common.format.Printf;

/**
 * A pattern formatter that colorizes the pattern in a fixed manner.
 */
public class ColorPatternFormatter extends PatternFormatter {

    private final Printf printf = new ColorPrintf();

    public ColorPatternFormatter() {
    }

    public ColorPatternFormatter(final String pattern) {
        super();
        setPattern(pattern);
    }

    static final boolean trueColor = determineTrueColor();

    static boolean determineTrueColor() {
        final String colorterm = System.getenv("COLORTERM");
        return (colorterm != null && (colorterm.contains("truecolor") || colorterm.contains("24bit")));
    }

    static boolean isTrueColor() {
        return trueColor;
    }

    public void setSteps(final FormatStep[] steps) {
        FormatStep[] colorSteps = new FormatStep[steps.length];
        for (int i = 0; i < steps.length; i++) {
            colorSteps[i] = colorize(steps[i]);
        }
        super.setSteps(colorSteps);
    }

    private FormatStep colorize(final FormatStep step) {
        switch (step.getItemType()) {
            case LEVEL:
                return new LevelColorStep(step);
            case SOURCE_CLASS_NAME:
                return new ColorStep(step, 0xff, 0xff, 0x44);
            case DATE:
                return new ColorStep(step, 0xc0, 0xc0, 0xc0);
            case SOURCE_FILE_NAME:
                return new ColorStep(step, 0xff, 0xff, 0x44);
            case HOST_NAME:
                return new ColorStep(step, 0x44, 0xff, 0x44);
            case SOURCE_LINE_NUMBER:
                return new ColorStep(step, 0xff, 0xff, 0x44);
            case LINE_SEPARATOR:
                return step;
            case CATEGORY:
                return new ColorStep(step, 0x44, 0x88, 0xff);
            case MDC:
                return new ColorStep(step, 0x44, 0xff, 0xaa);
            case MESSAGE:
                return new ColorStep(step, 0xff, 0xff, 0xff);
            case EXCEPTION_TRACE:
                return new ColorStep(step, 0xff, 0x44, 0x44);
            case SOURCE_METHOD_NAME:
                return new ColorStep(step, 0xff, 0xff, 0x44);
            case SOURCE_MODULE_NAME:
                return new ColorStep(step, 0x88, 0xff, 0x44);
            case SOURCE_MODULE_VERSION:
                return new ColorStep(step, 0x44, 0xff, 0x44);
            case NDC:
                return new ColorStep(step, 0x44, 0xff, 0xaa);
            case PROCESS_ID:
                return new ColorStep(step, 0xdd, 0xbb, 0x77);
            case PROCESS_NAME:
                return new ColorStep(step, 0xdd, 0xdd, 0x77);
            case RELATIVE_TIME:
                return new ColorStep(step, 0xc0, 0xc0, 0xc0);
            case RESOURCE_KEY:
                return new ColorStep(step, 0x44, 0xff, 0x44);
            case SYSTEM_PROPERTY:
                return new ColorStep(step, 0x88, 0x88, 0x00);
            case TEXT:
                return new ColorStep(step, 0xd0, 0xd0, 0xd0);
            case THREAD_ID:
                return new ColorStep(step, 0x44, 0xaa, 0x44);
            case THREAD_NAME:
                return new ColorStep(step, 0x44, 0xaa, 0x44);
            case COMPOUND:
            case GENERIC:
            default:
                return new ColorStep(step, 0xb0, 0xd0, 0xb0);
        }
    }

    private String colorizePlain(final String str) {
        return ColorUtil.endFgColor(ColorUtil.startFgColor(new StringBuilder(), isTrueColor(), 0xff, 0xff, 0xff).append(str)).toString();
    }

    public String formatMessage(final LogRecord logRecord) {
        if (logRecord instanceof ExtLogRecord) {
            final ExtLogRecord record = (ExtLogRecord) logRecord;
            if (record.getFormatStyle() != ExtLogRecord.FormatStyle.PRINTF || record.getParameters() == null || record.getParameters().length == 0) {
                return colorizePlain(super.formatMessage(record));
            }
            return printf.format(record.getMessage(), record.getParameters());
        } else {
            return colorizePlain(super.formatMessage(logRecord));
        }
    }

    static final class ColorStep implements FormatStep {
        private final int r, g, b;
        private final FormatStep delegate;

        ColorStep(final FormatStep delegate, final int r, final int g, final int b) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.delegate = delegate;
        }

        public void render(final Formatter formatter, final StringBuilder builder, final ExtLogRecord record) {
            ColorUtil.startFgColor(builder, isTrueColor(), r, g, b);
            delegate.render(formatter, builder, record);
            ColorUtil.endFgColor(builder);
        }

        public void render(final StringBuilder builder, final ExtLogRecord record) {
            render(null, builder, record);
        }

        public int estimateLength() {
            return delegate.estimateLength() + 30;
        }

        public boolean isCallerInformationRequired() {
            return delegate.isCallerInformationRequired();
        }

        public FormatStep[] childSteps() {
            return delegate.childSteps();
        }

        public int childStepCount() {
            return delegate.childStepCount();
        }

        public FormatStep getChildStep(final int idx) {
            return delegate.getChildStep(idx);
        }

        public ItemType getItemType() {
            return delegate.getItemType();
        }
    }

    static final class LevelColorStep implements FormatStep {
        private static final int LARGEST_LEVEL = Level.ERROR.intValue();
        private static final int SMALLEST_LEVEL = Level.TRACE.intValue();
        private static final int SATURATION = 66;
        private final FormatStep delegate;

        LevelColorStep(final FormatStep delegate) {
            this.delegate = delegate;
        }

        public void render(final Formatter formatter, final StringBuilder builder, final ExtLogRecord record) {
            final int level = Math.max(Math.min(record.getLevel().intValue(), LARGEST_LEVEL), SMALLEST_LEVEL) - SMALLEST_LEVEL;
            // really crappy linear interpolation
            int r = (level < 300 ? 0 : (level - 300) * (255 - SATURATION) / 300) + SATURATION;
            int g = (300 - abs(level - 300)) * (255 - SATURATION) / 300 + SATURATION;
            int b = (level > 300 ? 0 : level * (255 - SATURATION) / 300) + SATURATION;
            ColorUtil.startFgColor(builder, isTrueColor(), r, g, b);
            delegate.render(formatter, builder, record);
            ColorUtil.endFgColor(builder);
        }

        public void render(final StringBuilder builder, final ExtLogRecord record) {
            render(null, builder, record);
        }

        public int estimateLength() {
            return delegate.estimateLength() + 30;
        }

        public boolean isCallerInformationRequired() {
            return false;
        }
    }
}

// delayed init on native image
final class TrueColorHolder {
    private TrueColorHolder() {}

    static final boolean trueColor = ColorPatternFormatter.determineTrueColor();
}

@TargetClass(ColorPatternFormatter.class)
final class Target_ColorPatternFormatter {

    @Delete
    static final boolean trueColor = false;

    @Substitute
    static boolean isTrueColor() {
        return TrueColorHolder.trueColor;
    }
}