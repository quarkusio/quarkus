package io.quarkus.amazon.lambda.runtime;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import io.quarkus.runtime.annotations.Recorder;

/**
 * The class pre-initialization recorder.
 */
@Recorder
public class PreInitRecorder {
    public PreInitRecorder() {
    }

    public void preInit() {
        final InputStream is = PreInitRecorder.class.getResourceAsStream("/META-INF/pre-init-classes.txt");
        if (is != null)
            try (is) {
                try (BufferedInputStream bis = new BufferedInputStream(is)) {
                    try (InputStreamReader isr = new InputStreamReader(bis, StandardCharsets.UTF_8)) {
                        try (BufferedReader br = new BufferedReader(isr)) {
                            int idx;
                            String line;
                            while ((line = br.readLine()) != null) {
                                idx = line.indexOf('#');
                                if (idx != -1) {
                                    line = line.substring(0, idx);
                                }
                                final String className = line.stripTrailing();
                                if (!className.isBlank())
                                    try {
                                        Class.forName(className, true, PreInitRecorder.class.getClassLoader());
                                    } catch (Exception ignored) {
                                        // it's OK
                                    }
                            }
                        }
                    }
                }
            } catch (IOException ignored) {
                // it's OK
                return;
            }
    }
}
