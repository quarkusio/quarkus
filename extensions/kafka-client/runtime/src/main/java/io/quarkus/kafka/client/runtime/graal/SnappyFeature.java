package io.quarkus.kafka.client.runtime.graal;

import java.io.File;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeResourceAccess;
import org.xerial.snappy.OSInfo;

public class SnappyFeature implements Feature {

    /**
     * This method uses code from org.xerial.snappy.SnappyLoader#findNativeLibrary
     * to load the Snappy native library. The original code is licensed under the
     * Apache License, Version 2.0 and includes the following notice:
     *
     * <pre>
     *--------------------------------------------------------------------------
     *  Copyright 2011 Taro L. Saito
     *
     *  Licensed under the Apache License, Version 2.0 (the "License");
     *  you may not use this file except in compliance with the License.
     *  You may obtain a copy of the License at
     *
     *     http://www.apache.org/licenses/LICENSE-2.0
     *
     *  Unless required by applicable law or agreed to in writing, software
     *  distributed under the License is distributed on an "AS IS" BASIS,
     *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     *  See the License for the specific language governing permissions and
     *  limitations under the License.
     *--------------------------------------------------------------------------
     * </pre>
     */
    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        final String KEY_SNAPPY_LIB_PATH = "org.xerial.snappy.lib.path";
        final String KEY_SNAPPY_LIB_NAME = "org.xerial.snappy.lib.name";

        // Try to load the library in org.xerial.snappy.lib.path  */
        String snappyNativeLibraryPath = System.getProperty(KEY_SNAPPY_LIB_PATH);
        String snappyNativeLibraryName = System.getProperty(KEY_SNAPPY_LIB_NAME);

        // Resolve the library file name with a suffix (e.g., dll, .so, etc.)
        if (snappyNativeLibraryName == null) {
            snappyNativeLibraryName = System.mapLibraryName("snappyjava");
        }

        if (snappyNativeLibraryPath != null) {
            File nativeLib = new File(snappyNativeLibraryPath, snappyNativeLibraryName);
            if (nativeLib.exists()) {
                RuntimeResourceAccess.addResource(OSInfo.class.getModule(),
                        snappyNativeLibraryPath + "/" + snappyNativeLibraryName);
                return;
            }
        }

        // Load an OS-dependent native library inside a jar file
        snappyNativeLibraryPath = "org/xerial/snappy/native/" + OSInfo.getNativeLibFolderPathForCurrentOS();
        String path = snappyNativeLibraryPath + "/" + snappyNativeLibraryName;
        RuntimeResourceAccess.addResource(OSInfo.class.getModule(), path);
    }

}
