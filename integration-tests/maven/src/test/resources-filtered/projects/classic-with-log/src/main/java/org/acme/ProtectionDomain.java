package org.acme;

import org.apache.commons.io.IOUtils;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.function.Supplier;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

@Path("/protectionDomain")
public class ProtectionDomain {

    private static final String SUCCESS = "success";

    @GET
    public String useProtectionDomain() {
        return runAssertions(
                () -> assertReadManifestFromJar()
        );
    }

    private String runAssertions(Supplier<String>... assertions) {
        String result;
        for (Supplier<String> assertion : assertions) {
            result = assertion.get();
            if (!SUCCESS.equals(result)) {
                return result;
            }
        }
        return SUCCESS;
    }

    private String assertReadManifestFromJar() {
        final String testType = "manifest-from-jar";
        try {
            URL location = org.apache.commons.io.Charsets.class.getProtectionDomain().getCodeSource().getLocation();
            if (location == null) {
                return errorResult(testType, "location should not be null");
            }

            try (InputStream inputStream = location.openStream()) {
                try (JarInputStream jarInputStream = new JarInputStream(inputStream)) {
                    Manifest manifest = jarInputStream.getManifest();
                    if (manifest == null) {
                        return errorResult(testType, "manifest should not be null");
                    }
                    String implementationVersion = manifest.getMainAttributes().getValue("Implementation-Version");
                    if (implementationVersion == null) {
                        return errorResult(testType, "implementation-version should not be null");
                    }
                }
            }
            return SUCCESS;
        } catch (Exception e) {
            e.printStackTrace();
            return errorResult(testType, "exception during resolution of resource");
        }
    }

    private String errorResult(String testType, String message) {
        return testType + " / " + message;
    }
}
