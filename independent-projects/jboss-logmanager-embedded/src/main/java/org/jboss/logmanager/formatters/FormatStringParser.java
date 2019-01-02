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

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;
import java.util.TimeZone;

/**
 * A parser which can translate a log4j-style format string into a series of {@code FormatStep} instances.
 */
public final class FormatStringParser {

    /**
     * The regular expression for format strings.  Ain't regex grand?
     */
    private static final Pattern pattern = Pattern.compile(
                // greedily match all non-format characters
                "([^%]++)" +
                // match a format string...
                "|(?:%" +
                    // optional minimum width plus justify flag
                    "(?:(-)?(\\d+))?" +
                    // optional maximum width
                    "(?:\\.(-)?(\\d+))?" +
                    // the actual format character
                    "(.)" +
                    // an optional argument string
                    "(?:\\{([^}]*)\\})?" +
                // end format string
                ")"
    );

    private FormatStringParser() {
    }

    /**
     * Compile a format string into a series of format steps.
     *
     * @param formatString the format string
     * @return the format steps
     */
    public static FormatStep[] getSteps(final String formatString, ColorMap colors) {
        final long time = System.currentTimeMillis();
        final ArrayList<FormatStep> stepList = new ArrayList<FormatStep>();
        final Matcher matcher = pattern.matcher(formatString);
        TimeZone timeZone = TimeZone.getDefault();

        boolean colorUsed = false;
        while (matcher.find()) {
            final String otherText = matcher.group(1);
            if (otherText != null) {
                stepList.add(Formatters.textFormatStep(otherText));
            } else {
                final String hyphen = matcher.group(2);
                final String minWidthString = matcher.group(3);
                final String widthHyphen = matcher.group(4);
                final String maxWidthString = matcher.group(5);
                final String formatCharString = matcher.group(6);
                final String argument = matcher.group(7);
                final int minimumWidth = minWidthString == null ? 0 : Integer.parseInt(minWidthString);
                final boolean leftJustify = hyphen != null;
                final boolean truncateBeginning = widthHyphen != null;
                final int maximumWidth = maxWidthString == null ? 0 : Integer.parseInt(maxWidthString);
                final char formatChar = formatCharString.charAt(0);
                switch (formatChar) {
                    case 'c': {
                        stepList.add(Formatters.loggerNameFormatStep(leftJustify, minimumWidth, truncateBeginning, maximumWidth, argument));
                        break;
                    }
                    case 'C': {
                        stepList.add(Formatters.classNameFormatStep(leftJustify, minimumWidth, truncateBeginning, maximumWidth, argument));
                        break;
                    }
                    case 'd': {
                        stepList.add(Formatters.dateFormatStep(timeZone, argument, leftJustify, minimumWidth, truncateBeginning, maximumWidth));
                        break;
                    }
                    case 'D': {
                        stepList.add(Formatters.moduleNameFormatStep(leftJustify, minimumWidth, truncateBeginning, maximumWidth, argument));
                        break;
                    }
                    case 'e': {
                        stepList.add(Formatters.exceptionFormatStep(leftJustify, minimumWidth, truncateBeginning, maximumWidth, argument));
                        break;
                    }
                    case 'E': {
                        stepList.add(Formatters.exceptionFormatStep(leftJustify, minimumWidth, truncateBeginning, maximumWidth, argument));
                        break;
                    }
                    case 'F': {
                        stepList.add(Formatters.fileNameFormatStep(leftJustify, minimumWidth, truncateBeginning, maximumWidth));
                        break;
                    }
                    case 'h': {
                        stepList.add(Formatters.hostnameFormatStep(leftJustify, minimumWidth, truncateBeginning, maximumWidth, false));
                        break;
                    }
                    case 'H': {
                        stepList.add(Formatters.hostnameFormatStep(leftJustify, minimumWidth, truncateBeginning, maximumWidth, argument));
                        break;
                    }
                    case 'i': {
                        stepList.add(Formatters.processIdFormatStep(leftJustify, minimumWidth, truncateBeginning, maximumWidth));
                        break;
                    }
                    case 'k': {
                        stepList.add(Formatters.resourceKeyFormatStep(leftJustify, minimumWidth, truncateBeginning, maximumWidth));
                        break;
                    }
                    case 'K': {
                        if (ColorMap.SUPPORTS_COLOR) {
                            colorUsed = true;
                            stepList.add(Formatters.formatColor(colors, argument));
                        }
                        break;
                    }
                    case 'l': {
                        stepList.add(Formatters.locationInformationFormatStep(leftJustify, minimumWidth, truncateBeginning, maximumWidth));
                        break;
                    }
                    case 'L': {
                        stepList.add(Formatters.lineNumberFormatStep(leftJustify, minimumWidth, truncateBeginning, maximumWidth));
                        break;
                    }
                    case 'm': {
                        stepList.add(Formatters.messageFormatStep(leftJustify, minimumWidth, truncateBeginning, maximumWidth));
                        break;
                    }
                    case 'M': {
                        stepList.add(Formatters.methodNameFormatStep(leftJustify, minimumWidth, truncateBeginning, maximumWidth));
                        break;
                    }
                    case 'n': {
                        stepList.add(Formatters.lineSeparatorFormatStep(leftJustify, minimumWidth, truncateBeginning, maximumWidth));
                        break;
                    }
                    case 'N': {
                        stepList.add(Formatters.processNameFormatStep(leftJustify, minimumWidth, truncateBeginning, maximumWidth));
                        break;
                    }
                    case 'p': {
                        stepList.add(Formatters.levelFormatStep(leftJustify, minimumWidth, truncateBeginning, maximumWidth));
                        break;
                    }
                    case 'P': {
                        stepList.add(Formatters.localizedLevelFormatStep(leftJustify, minimumWidth, truncateBeginning, maximumWidth));
                        break;
                    }
                    case 'r': {
                        stepList.add(Formatters.relativeTimeFormatStep(time, leftJustify, minimumWidth, truncateBeginning, maximumWidth));
                        break;
                    }
                    case 's': {
                        stepList.add(Formatters.simpleMessageFormatStep(leftJustify, minimumWidth, truncateBeginning, maximumWidth));
                        break;
                    }
                    case 't': {
                        stepList.add(Formatters.threadFormatStep(argument, leftJustify, minimumWidth, truncateBeginning, maximumWidth));
                        break;
                    }
                    case 'v': {
                        stepList.add(Formatters.moduleVersionFormatStep(leftJustify, minimumWidth, maximumWidth, argument));
                        break;
                    }
                    case 'x': {
                        final int count = argument == null ? 0 : Integer.parseInt(argument);
                        stepList.add(Formatters.ndcFormatStep(leftJustify, minimumWidth, truncateBeginning, maximumWidth, count));
                        break;
                    }
                    case 'X': {
                        stepList.add(Formatters.mdcFormatStep(argument, leftJustify, minimumWidth, truncateBeginning, maximumWidth));
                        break;
                    }
                    case 'z': {
                        timeZone = TimeZone.getTimeZone(argument);
                        break;
                    }
                    case '#':
                    case '$': {
                        stepList.add(Formatters.systemPropertyFormatStep(argument, leftJustify, minimumWidth, truncateBeginning, maximumWidth));
                        break;
                    }
                    case '%': {
                        stepList.add(Formatters.textFormatStep("%"));
                        break;
                    }
                    default: {
                        throw new IllegalArgumentException("Encountered an unknown format character");
                    }
                }
            }
        }
        if (colorUsed) {
            stepList.add(Formatters.formatColor(colors, ColorMap.CLEAR_NAME));
        }
        return stepList.toArray(new FormatStep[stepList.size()]);
    }
}
