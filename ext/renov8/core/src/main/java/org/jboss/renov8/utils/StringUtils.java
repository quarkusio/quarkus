/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.renov8.utils;

import java.util.Iterator;
import java.util.List;

/**
 *
 * @author Alexey Loubyansky
 */
public class StringUtils {

    public static void append(StringBuilder buf, Iterable<?> i) {
        final Iterator<?> it = i.iterator();
        if(!it.hasNext()) {
            buf.append("[]");
            return;
        }
        buf.append(it.next());
        while(it.hasNext()) {
            buf.append(',').append(it.next());
        }
    }

    public static void appendList(StringBuilder buf, List<?> list) {
        if(list.isEmpty()) {
            buf.append("[]");
            return;
        }
        buf.append(list.get(0));
        for(int i = 1; i < list.size(); ++i) {
            buf.append(',').append(list.get(i));
        }
    }

    public static String ensureValidFileName(String value) {
        // replace characters that are invalid in paths
        return value.replaceAll("[:\\(\\)\\[\\]\\,]", "_");
    }
}
