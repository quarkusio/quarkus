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

package io.quarkus.resteasy.common.runtime.graal;

import java.awt.color.ICC_Profile;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "sun.java2d.cmm.lcms.LCMS")
final class LCMSSubstitutions {

    @Substitute
    private long loadProfileNative(byte[] data, Object ref) {
        throw new RuntimeException("Not Implemented");
    }

    @Substitute
    private int getProfileSizeNative(long ptr) {
        throw new RuntimeException("Not Implemented");
    }

    @Substitute
    private void getProfileDataNative(long ptr, byte[] data) {
        throw new RuntimeException("Not Implemented");
    }

    @Substitute
    static byte[] getTagNative(long profileID, int signature) {
        throw new RuntimeException("Not Implemented");
    }

    /**
     * Writes supplied data as a tag into the profile.
     * Destroys old profile, if new one was successfully
     * created.
     * <p>
     * Returns valid pointer to new profile.
     * <p>
     * Throws CMMException if operation fails, preserve old profile from
     * destruction.
     */
    @Substitute
    private void setTagDataNative(long ptr, int tagSignature,
            byte[] data) {
        throw new RuntimeException("Not Implemented");
    }

    @Substitute
    private static long createNativeTransform(
            long[] profileIDs, int renderType,
            int inFormatter, boolean isInIntPacked,
            int outFormatter, boolean isOutIntPacked,
            Object disposerRef) {
        throw new RuntimeException("Not Implemented");
    }

    @Substitute
    public static void initLCMS(Class<?> Trans, Class<?> IL, Class<?> Pf) {
        throw new RuntimeException("Not Implemented");
    }

    @Substitute
    public static synchronized LCMSProfile getProfileID(ICC_Profile profile) {
        throw new RuntimeException("Not Implemented");
    }

    @Substitute
    public static void colorConvert(LCMSTransform trans,
            LCMSImageLayout src,
            LCMSImageLayout dest) {
        throw new RuntimeException("Not Implemented");
    }

    @TargetClass(className = "sun.java2d.cmm.lcms.LCMSProfile")
    static final class LCMSProfile {

    }

    @TargetClass(className = "sun.java2d.cmm.lcms.LCMSImageLayout")
    static final class LCMSImageLayout {
    }

    @TargetClass(className = "sun.java2d.cmm.lcms.LCMSTransform")
    static final class LCMSTransform {

    }

}
