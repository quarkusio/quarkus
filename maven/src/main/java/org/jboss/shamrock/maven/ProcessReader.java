package org.jboss.shamrock.maven;

import java.io.IOException;
import java.io.InputStream;

final class ProcessReader implements Runnable {

    private final InputStream inputStream;

    ProcessReader(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    public void run() {
        byte[] b = new byte[100];
        int i;
        try {
            while ((i = inputStream.read(b)) > 0) {
                System.out.print(new String(b, 0, i));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
