package io.quarkus.test.common;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

class ProcessReader implements Runnable {

    private final InputStream inputStream;

    ProcessReader(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    public void run() {
        handleStart();
        byte[] b = new byte[100];
        int i;
        try {
            while ((i = inputStream.read(b)) > 0) {
                String str = new String(b, 0, i, StandardCharsets.UTF_8);
                System.out.print(str);
                handleString(str);
            }
        } catch (IOException e) {
            handleError(e);
        }
    }

    protected void handleStart() {

    }

    protected void handleString(String str) {

    }

    protected void handleError(IOException e) {

    }
}
