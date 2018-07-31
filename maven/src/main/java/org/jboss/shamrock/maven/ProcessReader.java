package org.jboss.shamrock.maven;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class ProcessReader implements Runnable {

    private final InputStream inputStream;
    private final boolean error;

    public ProcessReader(InputStream inputStream, boolean error) {
        this.inputStream = inputStream;
        this.error = error;
    }

    @Override
    public void run() {
        byte[] buf = new byte[100];
        int i;
        try {
            while ((i = inputStream.read(buf)) > 0) {
                String charSequence = new String(buf, 0, i, StandardCharsets.UTF_8);
                if (error) {
                    System.err.print(charSequence);
                } else {
                    System.out.print(charSequence);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
