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

/**
 * A formatter which uses a text pattern to format messages.
 */
public class PatternFormatter extends MultistepFormatter {

    private volatile String pattern;

    private volatile ColorMap colors;
    /**
     * Construct a new instance.
     */
    public PatternFormatter() {
        this.colors = ColorMap.DEFAULT_COLOR_MAP;
    }

    /**
     * Construct a new instance.
     *
     * @param pattern the initial pattern
     */
    public PatternFormatter(String pattern) {
        super(FormatStringParser.getSteps(pattern, ColorMap.DEFAULT_COLOR_MAP));
        this.colors = ColorMap.DEFAULT_COLOR_MAP;
        this.pattern = pattern;
    }

    /**
     * Construct a new instance.
     *
     * @param pattern the initial pattern
     * @param colors the color map to use
     */
    public PatternFormatter(String pattern, String colors) {
        ColorMap colorMap = ColorMap.create(colors);
        this.colors = colorMap;
        this.pattern = pattern;
        setSteps(FormatStringParser.getSteps(pattern, colorMap));
    }

    /**
     * Get the current format pattern.
     *
     * @return the pattern
     */
    public String getPattern() {
        return pattern;
    }

    /**
     * Set the format pattern.
     *
     * @param pattern the pattern
     */
    public void setPattern(final String pattern) {
        if (pattern == null) {
            setSteps(null);
        } else {
            setSteps(FormatStringParser.getSteps(pattern, colors));
        }
        this.pattern = pattern;
    }

    /**
     * Set the color map to use for log levels when %K{level} is used.
     *
     * <p>The format is level:color,level:color,...
     *
     * <p>Where level is either a numerical value or one of the following constants:</p>
     *
     * <table>
     *     <tr><td>fatal</td></tr>
     *     <tr><td>error</td></tr>
     *     <tr><td>severe</td></tr>
     *     <tr><td>warn</td></tr>
     *     <tr><td>warning</td></tr>
     *     <tr><td>info</td></tr>
     *     <tr><td>config</td></tr>
     *     <tr><td>debug</td></tr>
     *     <tr><td>trace</td></tr>
     *     <tr><td>fine</td></tr>
     *     <tr><td>finer</td></tr>
     *     <tr><td>finest</td></tr>
     * </table>
     *
     * <p>Color is one of the following constants:</p>
     *
     * <table>
     *     <tr><td>clear</td></tr>
     *     <tr><td>black</td></tr>
     *     <tr><td>red</td></tr>
     *     <tr><td>green</td></tr>
     *     <tr><td>yellow</td></tr>
     *     <tr><td>blue</td></tr>
     *     <tr><td>magenta</td></tr>
     *     <tr><td>cyan</td></tr>
     *     <tr><td>white</td></tr>
     *     <tr><td>brightblack</td></tr>
     *     <tr><td>brightred</td></tr>
     *     <tr><td>brightgreen</td></tr>
     *     <tr><td>brightyellow</td></tr>
     *     <tr><td>brightblue</td></tr>
     *     <tr><td>brightmagenta</td></tr>
     *     <tr><td>brightcyan</td></tr>
     *     <tr><td>brightwhite</td></tr>
     * </table>
     *
     * @param colors a colormap expression string described above
     */
    public void setColors(String colors) {
        ColorMap colorMap = ColorMap.create(colors);
        this.colors = colorMap;
        if (pattern != null) {
            setSteps(FormatStringParser.getSteps(pattern, colorMap));
        }
    }

    public String getColors() {
        return this.colors.toString();
    }
}
