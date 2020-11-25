package org.jboss.resteasy.reactive.common.util;

import java.util.HashMap;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class HeaderParameterParser {
    static int getEndName(String params, int start) {
        int equals = params.indexOf('=', start);
        int semicolon = params.indexOf(';', start);
        if (equals == -1 && semicolon == -1)
            return params.length();
        if (equals == -1)
            return semicolon;
        if (semicolon == -1)
            return equals;
        int end = (equals < semicolon) ? equals : semicolon;
        return end;
    }

    public static int setParam(HashMap<String, String> typeParams, String params, int start) {
        boolean quote = false;
        boolean backslash = false;

        int end = getEndName(params, start);
        String name = params.substring(start, end).trim();
        if (end < params.length() && params.charAt(end) == '=')
            end++;

        StringBuilder buffer = new StringBuilder(params.length() - end);
        int i = end;
        for (; i < params.length(); i++) {
            char c = params.charAt(i);

            switch (c) {
                case '"': {
                    if (backslash) {
                        backslash = false;
                        buffer.append(c);
                    } else {
                        quote = !quote;
                    }
                    break;
                }
                case '\\': {
                    if (backslash) {
                        backslash = false;
                        buffer.append(c);
                    }
                    break;
                }
                case ';': {
                    if (!quote) {
                        String value = buffer.toString().trim();
                        typeParams.put(name, value);
                        return i + 1;
                    } else {
                        buffer.append(c);
                    }
                    break;
                }
                default: {
                    buffer.append(c);
                    break;
                }
            }
        }
        String value = buffer.toString().trim();
        typeParams.put(name, value);
        return i;
    }
}
