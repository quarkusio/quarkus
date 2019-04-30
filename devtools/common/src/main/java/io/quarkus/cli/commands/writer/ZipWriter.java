/**
 * 
 */
package io.quarkus.cli.commands.writer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author guillaumedufour
 *
 */
public class ZipWriter implements Writer {
    private final ZipOutputStream zos;
    private final Map<String, byte[]> contentByPath = new HashMap<>();

    public ZipWriter(final ZipOutputStream zip) {
        zos = zip;
    }

    @Override
    public String mkdirs(String path) throws IOException {
        String dirPath = path;
        if (!dirPath.endsWith("/")) {
            dirPath = dirPath + "/";
        }
        ZipEntry ze = new ZipEntry(dirPath);
        zos.putNextEntry(ze);
        zos.closeEntry();
        return dirPath;
    }

    @Override
    public void write(String path, String content) throws IOException {
        ZipEntry ze = new ZipEntry(path);
        zos.putNextEntry(ze);
        byte[] contentBytes = content.getBytes();
        zos.write(contentBytes);
        contentByPath.put(path, contentBytes);
        zos.closeEntry();
    }

    @Override
    public byte[] getContent(String path) {
        return contentByPath.get(path);
    }

    @Override
    public boolean exists(String path) {
        return contentByPath.containsKey(path);
    }

}
