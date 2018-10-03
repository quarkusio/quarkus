/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2014 Red Hat, Inc., and individual contributors
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.jboss.logmanager.Level;

/**
 * @author Jason T. Greene
 */
public class ColorMap {
    private static final String DARK_BLACK = "\033[30m";
    private static final String DARK_RED = "\033[31m";
    private static final String DARK_GREEN = "\033[32m";
    private static final String DARK_YELLOW = "\033[33m";
    private static final String DARK_BLUE = "\033[34m";
    private static final String DARK_MAGENTA = "\033[35m";
    private static final String DARK_CYAN = "\033[36m";
    private static final String DARK_WHITE = "\033[37m";

    private static final String BRIGHT_BLACK = "\033[1;30m";
    private static final String BRIGHT_RED = "\033[1;31m";
    private static final String BRIGHT_GREEN = "\033[1;32m";
    private static final String BRIGHT_YELLOW = "\033[1;33m";
    private static final String BRIGHT_BLUE = "\033[1;34m";
    private static final String BRIGHT_MAGENTA = "\033[1;35m";
    private static final String BRIGHT_CYAN = "\033[1;36m";
    private static final String BRIGHT_WHITE = "\033[1;37m";

    private static final String CLEAR = "\033[0m";


    static final boolean SUPPORTS_COLOR;

    private static final Map<String, String> codes;
    private static final Map<String, String> reverseCodes;
    private static final Map<String, Integer> levels = new HashMap<String, Integer>();
    private static final Map<Integer, String> reverseLevels = new HashMap<Integer, String>();

    private static final NavigableMap<Integer, String> defaultLevelMap = new TreeMap<Integer, String>();

    static final ColorMap DEFAULT_COLOR_MAP = new ColorMap(defaultLevelMap);

    private final NavigableMap<Integer, String> levelMap;

    private ColorMap(NavigableMap<Integer,String> levelMap) {
        this.levelMap = levelMap;
    }

    private static final int SEVERE_NUM = Level.SEVERE.intValue();
    private static final int FATAL_NUM = Level.FATAL.intValue();
    private static final int ERROR_NUM = Level.ERROR.intValue();
    private static final int WARN_NUM = Level.WARN.intValue();
    private static final int INFO_NUM = Level.INFO.intValue();
    private static final int CONFIG_NUM = Level.CONFIG.intValue();
    private static final int DEBUG_NUM = Level.DEBUG.intValue();
    private static final int TRACE_NUM = Level.TRACE.intValue();
    private static final int FINE_NUM = Level.FINE.intValue();
    private static final int FINER_NUM = Level.FINER.intValue();
    private static final int FINEST_NUM = Level.FINEST.intValue();

    static final String LEVEL_NAME = "level";
    static final String SEVERE_NAME = "severe";
    static final String FATAL_NAME = "fatal";
    static final String ERROR_NAME = "error";
    static final String WARN_NAME = "warn";
    static final String WARNING_NAME = "warning";
    static final String INFO_NAME = "info";
    static final String DEBUG_NAME = "debug";
    static final String TRACE_NAME = "trace";
    static final String CONFIG_NAME = "config";
    static final String FINE_NAME = "fine";
    static final String FINER_NAME = "finer";
    static final String FINEST_NAME = "finest";

    static final String BLACK_NAME = "black";
    static final String GREEN_NAME = "green";
    static final String RED_NAME = "red";
    static final String YELLOW_NAME = "yellow";
    static final String BLUE_NAME = "blue";
    static final String MAGENTA_NAME = "magenta";
    static final String CYAN_NAME = "cyan";
    static final String WHITE_NAME = "white";

    static final String BRIGHT_BLACK_NAME = "brightblack";
    static final String BRIGHT_RED_NAME = "brightred";
    static final String BRIGHT_GREEN_NAME = "brightgreen";
    static final String BRIGHT_BLUE_NAME = "brightblue";
    static final String BRIGHT_YELLOW_NAME = "brightyellow";
    static final String BRIGHT_MAGENTA_NAME = "brightmagenta";
    static final String BRIGHT_CYAN_NAME = "brightcyan";
    static final String BRIGHT_WHITE_NAME = "brightwhite";

    static final String CLEAR_NAME = "clear";

    static {
        // Turn color on by default for everything but Windows, unless ansicon is used
        String os = System.getProperty("os.name");
        final boolean dft = os != null && (!os.toLowerCase(Locale.ROOT).contains("win") || System.getenv("ANSICON") != null);
        final String nocolor = System.getProperty("org.jboss.logmanager.nocolor");
        SUPPORTS_COLOR = (nocolor == null ? dft : "false".equalsIgnoreCase(nocolor));

        levels.put(SEVERE_NAME, SEVERE_NUM);
        levels.put(FATAL_NAME, FATAL_NUM);
        levels.put(ERROR_NAME, ERROR_NUM);
        levels.put(WARN_NAME, WARN_NUM);
        levels.put(WARNING_NAME, WARN_NUM);
        levels.put(INFO_NAME, INFO_NUM);
        levels.put(CONFIG_NAME, CONFIG_NUM);
        levels.put(DEBUG_NAME, DEBUG_NUM);
        levels.put(TRACE_NAME, TRACE_NUM);
        levels.put(FINE_NAME, FINE_NUM);
        levels.put(FINER_NAME, FINER_NUM);
        levels.put(FINEST_NAME, FINEST_NUM);

        reverseLevels.put(SEVERE_NUM, SEVERE_NAME);
        reverseLevels.put(CONFIG_NUM, CONFIG_NAME);
        reverseLevels.put(FINE_NUM, FINE_NAME);
        reverseLevels.put(FINER_NUM, FINER_NAME);
        reverseLevels.put(FINEST_NUM, FINEST_NAME);
        reverseLevels.put(FATAL_NUM, FATAL_NAME);
        reverseLevels.put(ERROR_NUM, ERROR_NAME);
        reverseLevels.put(WARN_NUM, WARN_NAME);
        reverseLevels.put(INFO_NUM, INFO_NAME);
        reverseLevels.put(DEBUG_NUM, DEBUG_NAME);
        reverseLevels.put(TRACE_NUM, TRACE_NAME);

        if (SUPPORTS_COLOR) {
            codes = new HashMap<String, String>();
            codes.put(BLACK_NAME, DARK_BLACK);
            codes.put(RED_NAME, DARK_RED);
            codes.put(GREEN_NAME, DARK_GREEN);
            codes.put(YELLOW_NAME, DARK_YELLOW);
            codes.put(BLUE_NAME, DARK_BLUE);
            codes.put(MAGENTA_NAME, DARK_MAGENTA);
            codes.put(CYAN_NAME, DARK_CYAN);
            codes.put(WHITE_NAME, DARK_WHITE);
            codes.put(BRIGHT_BLACK_NAME, BRIGHT_BLACK);
            codes.put(BRIGHT_RED_NAME, BRIGHT_RED);
            codes.put(BRIGHT_GREEN_NAME, BRIGHT_GREEN);
            codes.put(BRIGHT_YELLOW_NAME, BRIGHT_YELLOW);
            codes.put(BRIGHT_BLUE_NAME, BRIGHT_BLUE);
            codes.put(BRIGHT_MAGENTA_NAME, BRIGHT_MAGENTA);
            codes.put(BRIGHT_CYAN_NAME, BRIGHT_CYAN);
            codes.put(BRIGHT_WHITE_NAME, BRIGHT_WHITE);
            codes.put(CLEAR_NAME, CLEAR);

            reverseCodes = new HashMap<String, String>();
            reverseCodes.put(DARK_BLACK, BLACK_NAME);
            reverseCodes.put(DARK_RED, RED_NAME);
            reverseCodes.put(DARK_GREEN, GREEN_NAME);
            reverseCodes.put(DARK_YELLOW, YELLOW_NAME);
            reverseCodes.put(DARK_BLUE, BLUE_NAME);
            reverseCodes.put(DARK_MAGENTA, MAGENTA_NAME);
            reverseCodes.put(DARK_CYAN, CYAN_NAME);
            reverseCodes.put(DARK_WHITE, WHITE_NAME);
            reverseCodes.put(BRIGHT_BLACK, BRIGHT_BLACK_NAME);
            reverseCodes.put(BRIGHT_RED, BRIGHT_RED_NAME);
            reverseCodes.put(BRIGHT_GREEN, BRIGHT_GREEN_NAME);
            reverseCodes.put(BRIGHT_YELLOW, BRIGHT_YELLOW_NAME);
            reverseCodes.put(BRIGHT_BLUE, BRIGHT_BLUE_NAME);
            reverseCodes.put(BRIGHT_MAGENTA, BRIGHT_MAGENTA_NAME);
            reverseCodes.put(BRIGHT_CYAN, BRIGHT_CYAN_NAME);
            reverseCodes.put(BRIGHT_WHITE, BRIGHT_WHITE_NAME);
            reverseCodes.put(CLEAR, CLEAR);

            defaultLevelMap.put(Level.ERROR.intValue(), DARK_RED);
            defaultLevelMap.put(Level.WARN.intValue(), DARK_YELLOW);
            defaultLevelMap.put(Level.INFO.intValue(), CLEAR);
            defaultLevelMap.put(Level.DEBUG.intValue(), DARK_GREEN);
        } else {
            reverseCodes = codes = Collections.emptyMap();
        }

    }

    static ColorMap create(String expression) {
        if (expression == null || expression.length() < 3) {
            return DEFAULT_COLOR_MAP;
        }

        NavigableMap<Integer, String> levelMap = new TreeMap<Integer, String>();

        for (String pair : expression.split(",")) {
            String[] parts = pair.split(":");
            if (parts.length != 2) {
                continue;
            }

            String color = codes.get(parts[1].toLowerCase(Locale.ROOT));
            if (color == null) {
                continue;
            }


            try {
                int i = Integer.parseInt(parts[0]);
                levelMap.put(i, color);
                continue;
            } catch (NumberFormatException e) {
                // eat
            }

            Integer i = levels.get(parts[0].toLowerCase(Locale.ROOT));
            if (i == null) {
                continue;
            }

            levelMap.put(i, color);
        }

        return new ColorMap(levelMap);
    }

    String getCode(String name, java.util.logging.Level level) {
        if (name == null || !SUPPORTS_COLOR) {
            return null;
        }

        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.equals(LEVEL_NAME)) {
            Map.Entry<Integer,String> entry = levelMap.floorEntry(level.intValue());
            return entry != null ? entry.getValue() : null;
        }

        return codes.get(lower);
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<Integer, String> entry : levelMap.descendingMap().entrySet()) {
            Integer num = entry.getKey();
            String level = reverseLevels.get(num);
            builder.append(level == null ? num : level).append(":").append(reverseCodes.get(entry.getValue()))
                    .append(",");
        }
        if (builder.length() > 0) {
            builder.setLength(builder.length() - 1);
        }

        return builder.toString();
    }
}