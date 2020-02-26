/*
 Copyright (c) 2002 JSON.org

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 The Software shall be used for Good, not Evil.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 */
package io.quarkus.vertx.http.runtime.devmode;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

class JsonObject {

    static void writeValue(StringWriter writer, Object value, int indentFactor, int indent) {
        if (value == null || value.equals(null)) {
            writer.write("null");
        } else if (value instanceof JsonArray) {
            ((JsonArray) value).write(writer, indentFactor, indent);
        } else if (value instanceof JsonObject) {
            ((JsonObject) value).write(writer, indentFactor, indent);
        } else {
            writer.write(quote(value.toString()));
        }
    }

    static String quote(String string) {
        if (string == null || string.isEmpty()) {
            return "\"\"";
        }

        char b;
        char c = 0;
        String hhhh;
        int i;
        int len = string.length();
        StringWriter w = new StringWriter();

        w.write('"');
        for (i = 0; i < len; i += 1) {
            b = c;
            c = string.charAt(i);
            switch (c) {
                case '\\':
                case '"':
                    w.write('\\');
                    w.write(c);
                    break;
                case '/':
                    if (b == '<') {
                        w.write('\\');
                    }
                    w.write(c);
                    break;
                case '\b':
                    w.write("\\b");
                    break;
                case '\t':
                    w.write("\\t");
                    break;
                case '\n':
                    w.write("\\n");
                    break;
                case '\f':
                    w.write("\\f");
                    break;
                case '\r':
                    w.write("\\r");
                    break;
                default:
                    if (c < ' ' || (c >= '\u0080' && c < '\u00a0')
                            || (c >= '\u2000' && c < '\u2100')) {
                        w.write("\\u");
                        hhhh = Integer.toHexString(c);
                        w.write("0000", 0, 4 - hhhh.length());
                        w.write(hhhh);
                    } else {
                        w.write(c);
                    }
            }
        }
        w.write('"');
        return w.toString();
    }

    static void indent(StringWriter writer, int indent) {
        for (int i = 0; i < indent; i += 1) {
            writer.write(' ');
        }
    }

    private final Map<String, Object> map;

    JsonObject() {
        this.map = new HashMap<String, Object>();
    }

    JsonObject put(String key, int value) {
        map.put(key, Integer.valueOf(value));
        return this;
    }

    JsonObject put(String key, String value) {
        map.put(key, value);
        return this;
    }

    JsonObject put(String key, JsonArray value) {
        map.put(key, value);
        return this;
    }

    JsonObject put(String key, JsonObject value) {
        map.put(key, value);
        return this;
    }

    public String toString(int indentFactor) {
        StringWriter w = new StringWriter();
        write(w, indentFactor, 0);
        return w.toString();
    }

    private void write(StringWriter writer, int indentFactor, int indent) {
        boolean needsComma = false;
        writer.write('{');

        int newIndent = indent + indentFactor;
        for (Entry<String, ?> entry : map.entrySet()) {
            if (needsComma) {
                writer.write(',');
            }
            if (indentFactor > 0) {
                writer.write('\n');
            }
            indent(writer, newIndent);
            String key = entry.getKey();
            writer.write(quote(key));
            writer.write(':');
            if (indentFactor > 0) {
                writer.write(' ');
            }
            writeValue(writer, entry.getValue(), indentFactor, newIndent);
            needsComma = true;
        }
        if (indentFactor > 0) {
            writer.write('\n');
        }
        indent(writer, indent);
        writer.write('}');
    }
}
