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

package org.jboss.shamrock.creator.util;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Locale;

/**
*
* @author Alexey Loubyansky
*/
public class PropertyUtils {

    private static final String OS_NAME = "os.name";
    private static final String USER_HOME = "user.home";
    private static final String WINDOWS = "windows";

    private PropertyUtils() {
    }

   public static boolean isWindows() {
       return getProperty(OS_NAME).toLowerCase(Locale.ENGLISH).indexOf(WINDOWS) >= 0;
   }

   public static String getUserHome() {
       return getProperty(USER_HOME);
   }

   public static String getProperty(final String name, String defValue) {
       final String value = getProperty(name);
       return value == null ? defValue : value;
   }

   public static String getProperty(final String name) {
       assert name != null : "name is null";
       final SecurityManager sm = System.getSecurityManager();
       if(sm != null) {
           return AccessController.doPrivileged(new PrivilegedAction<String>(){
               @Override
               public String run() {
                   return System.getProperty(name);
               }});
       } else {
           return System.getProperty(name);
       }
   }
}
