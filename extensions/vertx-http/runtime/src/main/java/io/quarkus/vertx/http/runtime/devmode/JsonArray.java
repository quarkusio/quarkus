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
import java.util.ArrayList;

class JsonArray {

    private final ArrayList<Object> list;

    JsonArray() {
        this.list = new ArrayList<Object>();
    }

    JsonArray put(JsonObject value) {
        list.add(value);
        return this;
    }

    void write(StringWriter writer, int indentFactor, int indent) {
        boolean needsComma = false;
        int length = list.size();
        writer.write('[');

        if (length == 1) {
            JsonObject.writeValue(writer, list.get(0), indentFactor, indent);
        } else if (length != 0) {
            int newIndent = indent + indentFactor;
            for (int i = 0; i < length; i += 1) {
                if (needsComma) {
                    writer.write(',');
                }
                if (indentFactor > 0) {
                    writer.write('\n');
                }
                JsonObject.indent(writer, newIndent);
                JsonObject.writeValue(writer, list.get(i), indentFactor, newIndent);
                needsComma = true;
            }
            if (indentFactor > 0) {
                writer.write('\n');
            }
            JsonObject.indent(writer, indent);
        }
        writer.write(']');
    }
}
