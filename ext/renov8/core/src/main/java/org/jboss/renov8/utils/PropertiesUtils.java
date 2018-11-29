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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Locale;

/**
*
* @author Alexey Loubyansky
*/
public class PropertiesUtils {

   private PropertiesUtils() {
   }

   public static boolean isWindows() {
       return getSystemProperty("os.name").toLowerCase(Locale.ENGLISH).indexOf("windows") >= 0;
   }

   public static String getSystemProperty(final String name) {
       assert name != null : "name is null";
       final SecurityManager sm = System.getSecurityManager();
       if(sm != null) {
           return AccessController.doPrivileged(new PrivilegedAction<String>(){
               public String run() {
                   return System.getProperty(name);
               }});
       } else {
           return System.getProperty(name);
       }
   }
}
