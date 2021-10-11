package io.quarkus.jsch.deployment;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.jsch.runtime.PortWatcherRunTime;

class JSchProcessor {

    @BuildStep
    ExtensionSslNativeSupportBuildItem sslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(Feature.JSCH);
    }

    @BuildStep
    RuntimeInitializedClassBuildItem runtimeInitialized() {
        return new RuntimeInitializedClassBuildItem(PortWatcherRunTime.class.getName());
    }

    @BuildStep
    ReflectiveClassBuildItem reflection() {
        //Classes that use reflection
        return new ReflectiveClassBuildItem(true, true,
                "com.jcraft.jsch.CipherNone",
                "com.jcraft.jsch.DHEC256",
                "com.jcraft.jsch.DHEC384",
                "com.jcraft.jsch.DHEC521",
                "com.jcraft.jsch.DHG1",
                "com.jcraft.jsch.DHG14",
                "com.jcraft.jsch.DHGEX",
                "com.jcraft.jsch.DHGEX256",
                "com.jcraft.jsch.jce.AES128CBC",
                "com.jcraft.jsch.jce.AES128CTR",
                "com.jcraft.jsch.jce.AES192CBC",
                "com.jcraft.jsch.jce.AES192CTR",
                "com.jcraft.jsch.jce.AES256CBC",
                "com.jcraft.jsch.jce.AES256CTR",
                "com.jcraft.jsch.jce.ARCFOUR",
                "com.jcraft.jsch.jce.ARCFOUR128",
                "com.jcraft.jsch.jce.ARCFOUR256",
                "com.jcraft.jsch.jce.BlowfishCBC",
                "com.jcraft.jsch.jce.DH",
                "com.jcraft.jsch.jce.ECDHN",
                "com.jcraft.jsch.jce.HMACMD5",
                "com.jcraft.jsch.jce.HMACMD596",
                "com.jcraft.jsch.jce.HMACSHA1",
                "com.jcraft.jsch.jce.HMACSHA1",
                "com.jcraft.jsch.jce.HMACSHA196",
                "com.jcraft.jsch.jce.HMACSHA256",
                "com.jcraft.jsch.jce.KeyPairGenDSA",
                "com.jcraft.jsch.jce.KeyPairGenECDSA",
                "com.jcraft.jsch.jce.KeyPairGenRSA",
                "com.jcraft.jsch.jce.MD5",
                "com.jcraft.jsch.jce.Random",
                "com.jcraft.jsch.jce.SHA1",
                "com.jcraft.jsch.jce.SHA256",
                "com.jcraft.jsch.jce.SHA384",
                "com.jcraft.jsch.jce.SHA512",
                "com.jcraft.jsch.jce.SignatureDSA",
                "com.jcraft.jsch.jce.SignatureECDSA256",
                "com.jcraft.jsch.jce.SignatureECDSA384",
                "com.jcraft.jsch.jce.SignatureECDSA521",
                "com.jcraft.jsch.jce.SignatureRSA",
                "com.jcraft.jsch.jce.TripleDESCBC",
                "com.jcraft.jsch.jce.TripleDESCTR",
                "com.jcraft.jsch.jcraft.Compression",
                "com.jcraft.jsch.jgss.GSSContextKrb5",
                "com.jcraft.jsch.UserAuthGSSAPIWithMIC",
                "com.jcraft.jsch.UserAuthKeyboardInteractive",
                "com.jcraft.jsch.UserAuthNone",
                "com.jcraft.jsch.UserAuthPassword",
                "com.jcraft.jsch.UserAuthPublicKey");
    }
}
