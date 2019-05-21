/**
 * 
 */
package io.quarkus.cli.commands.writer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * ProjectWriter implementation to create a zip.
 */
public class ZipProjectWriter implements ProjectWriter {
    private final ZipOutputStream zos;
    private final Map<String, byte[]> contentByPath = new LinkedHashMap<>();
    private final List<String> dirs = new ArrayList<>();

    public ZipProjectWriter(final ZipOutputStream zip) {
        zos = zip;
    }

    @Override
    public String mkdirs(String path) throws IOException {
        if (path.length() == 0) {
            return "";
        }
        String dirPath = path;
        if (!dirPath.endsWith("/")) {
            dirPath = dirPath + "/";
        }
        if (dirs.contains(dirPath)) {
            return dirPath;
        }
        ZipEntry ze = new ZipEntry(dirPath);
        zos.putNextEntry(ze);
        zos.closeEntry();
        dirs.add(dirPath);
        return dirPath;
    }

    @Override
    public void write(String path, String content) throws IOException {
        byte[] contentBytes = content.getBytes("UTF-8");
        contentByPath.put(path, contentBytes);
    }

    @Override
    public byte[] getContent(String path) {
        return contentByPath.get(path);
    }

    @Override
    public boolean exists(String path) {
        return contentByPath.containsKey(path);
    }

    @Override
    public void close() throws IOException {
        for (Entry<String, byte[]> entry : contentByPath.entrySet()) {
            ZipEntry ze = new ZipEntry(entry.getKey());
            zos.putNextEntry(ze);
            zos.write(entry.getValue());
            zos.closeEntry();
        }

    }

}
