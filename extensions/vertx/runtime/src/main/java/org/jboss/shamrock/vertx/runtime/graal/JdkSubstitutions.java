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

package io.quarkus.vertx.runtime.graal;

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.URL;
import java.security.AccessControlContext;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "sun.misc.URLClassPath$Loader")
final class Target_sun_misc_URLClassPath$Loader {

    @Alias
    public Target_sun_misc_URLClassPath$Loader(URL url) {
    }
}

@TargetClass(className = "sun.misc.URLClassPath$FileLoader")
final class Target_sun_misc_URLClassPath$FileLoader {

    @Alias
    public Target_sun_misc_URLClassPath$FileLoader(URL url) {
    }
}

@TargetClass(className = "sun.misc.URLClassPath")
final class Target_sun_misc_URLClassPath {

    @Alias
    private AccessControlContext acc;

    @Substitute
    private Target_sun_misc_URLClassPath$Loader getLoader(final URL url) throws IOException {
        try {
            return java.security.AccessController.doPrivileged(
                    new java.security.PrivilegedExceptionAction<Target_sun_misc_URLClassPath$Loader>() {
                        @Override
                        public Target_sun_misc_URLClassPath$Loader run() throws IOException {
                            String file = url.getFile();
                            if (file != null && file.endsWith("/")) {
                                if ("file".equals(url.getProtocol())) {
                                    return (Target_sun_misc_URLClassPath$Loader) (Object) new Target_sun_misc_URLClassPath$FileLoader(
                                            url);
                                } else {
                                    return new Target_sun_misc_URLClassPath$Loader(url);
                                }
                            } else {
                                // that must be wrong, but JarLoader is deleted by SVM
                                return (Target_sun_misc_URLClassPath$Loader) (Object) new Target_sun_misc_URLClassPath$FileLoader(
                                        url);
                            }
                        }
                    }, acc);
        } catch (java.security.PrivilegedActionException pae) {
            throw (IOException) pae.getException();
        }
    }

    @Substitute
    private int[] getLookupCache(String name) {
        return null;
    }
}

@TargetClass(className = "sun.nio.ch.DatagramChannelImpl")
final class Target_sun_nio_ch_DatagramChannelImpl {

    @Substitute
    private static void disconnect0(FileDescriptor fd, boolean isIPv6)
            throws IOException {
        throw new RuntimeException("Unimplemented: sun.nio.ch.DatagramChannelImpl.disconnect0(FileDescriptor, boolean)");
    }
}

class JdkSubstitutions {

}
