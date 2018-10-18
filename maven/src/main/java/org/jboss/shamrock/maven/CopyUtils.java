package org.jboss.shamrock.maven;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;

public final class CopyUtils {

    private CopyUtils() {
        //Not to be constructed
    }

    public static byte[] readFileContentNoIOExceptions(final Path path) {
        try {
            return readFileContent(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] readFileContent(final Path path) throws IOException {
        final File file = path.toFile();
        final long fileLength = file.length();
        if (fileLength > Integer.MAX_VALUE) {
            throw new RuntimeException("Can't process class files larger than Integer.MAX_VALUE bytes");
        }
        final int intLength = (int) fileLength;
        try (FileInputStream in = new FileInputStream(file)) {
            //Might be large but we need a single byte[] at the end of things, might as well allocate it in one shot:
            ByteArrayOutputStream out = new ByteArrayOutputStream(intLength);
            final int reasonableBufferSize = Math.min(intLength, 2048);
            byte[] buf = new byte[reasonableBufferSize];
            int r;
            while ((r = in.read(buf)) > 0) {
               out.write(buf, 0, r);
            }
            return out.toByteArray();
        }
    }

}
