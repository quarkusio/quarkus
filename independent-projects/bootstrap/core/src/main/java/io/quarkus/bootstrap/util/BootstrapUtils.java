/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.bootstrap.util;

import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 *
 * @author Alexey Loubyansky
 */
public class BootstrapUtils {

    public static int logUrls(ClassLoader cl) {
        int depth = 0;
        if(cl.getParent() != null) {
            depth += logUrls(cl.getParent());
        }
        final StringBuilder buf = new StringBuilder();
        final String offset;
        if(depth == 0) {
            offset = "";
        } else {
            for (int i = 0; i < depth; ++i) {
                buf.append("  ");
            }
            offset = buf.toString();
        }
        if(!(cl instanceof java.net.URLClassLoader)) {
            System.out.println(buf.append(cl.getClass().getName()).toString());
        } else {
            final java.net.URL[] urls = ((java.net.URLClassLoader) cl).getURLs();
            final String[] urlStrs;
            if (urls.length == 1) {
                final Path p = Paths.get(urls[0].getFile());
                if(Files.isDirectory(p)) {
                    urlStrs = new String[] {urls[0].toExternalForm()};
                } else {
                    try (FileSystem fs = FileSystems.newFileSystem(p, null)) {
                        Path path = fs.getPath("META-INF/MANIFEST.MF");
                        if (!Files.exists(path)) {
                            throw new IllegalStateException("Failed to locate the manifest");
                        }

                        final Manifest manifest;
                        try (InputStream input = Files.newInputStream(path)) {
                            manifest = new Manifest(input);
                        }
                        Attributes attrs = manifest.getMainAttributes();
                        urlStrs = attrs.getValue("Class-Path").split("\\s+");
                    } catch (Exception e1) {
                        throw new IllegalStateException("Failed to read MANIFEST.MF from " + urls[0]);
                    }
                }
            } else {
                urlStrs = new String[urls.length];
                for (int i = 0; i < urls.length; ++i) {
                    final java.net.URL url = urls[i];
                    urlStrs[i] = url.toExternalForm();
                }
            }
            java.util.Arrays.sort(urlStrs);
            int i = 0;
            System.out.println(offset + cl);
            while(i < urlStrs.length) {
                System.out.println(offset + (i + 1) + ") " + urlStrs[i++]);
            }

        }
        return depth + 1;
    }

    public static void logUrlDiff(ClassLoader cl1, String cl1Header, ClassLoader cl2, String cl2Header) {

        final Set<String> cl1Urls = new HashSet<>();
        collectUrls(cl1, cl1Urls);
/*
        URLClassLoader classLoader = (URLClassLoader) cl1;
        try(FileSystem fs = FileSystems.newFileSystem(Paths.get(classLoader.getURLs()[0].getFile()), null)) {
            Path path = fs.getPath("META-INF/MANIFEST.MF");
            if(!Files.exists(path)) {
                throw new IllegalStateException("Failed to locate the manifest");
            }

            final Manifest manifest;
            try(InputStream input = Files.newInputStream(path)) {
                manifest = new Manifest(input);
            }
            Attributes attrs = manifest.getMainAttributes();
            String[] urlStrs = attrs.getValue("Class-Path").split("\\s+");
            for(String urlStr : urlStrs) {
                cl1Urls.add(new URL(urlStr).getFile());
            }
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
*/
        final Set<String> cl2Urls = new HashSet<>();
        collectUrls(cl2, cl2Urls);

        int commonUrls = 0;
        Iterator<String> i = cl1Urls.iterator();
        while(i.hasNext()) {
            String next = i.next();
            if(cl2Urls.remove(next)) {
                i.remove();
                ++commonUrls;
            }
        }
        System.out.println("URLs not in " + cl2Header + ":");
        List<String> list = new ArrayList<>(cl1Urls);
        Collections.sort(list);
        for(String s: list) {
            System.out.println(s);
        }

        System.out.println("URLs not in " + cl1Header + ":");
        list = new ArrayList<>(cl2Urls);
        Collections.sort(list);
        for(String s : list) {
            System.out.println(s);
        }
        System.out.println("Common URLs: " + commonUrls);
    }

    private static void collectUrls(ClassLoader cl, Set<String> set) {
        final ClassLoader parent = cl.getParent();
        if(parent != null) {
            collectUrls(parent, set);
        }
        if(!(cl instanceof URLClassLoader)) {
            return;
        }
        final URL[] urls = ((URLClassLoader)cl).getURLs();
        for(URL url : urls) {
            set.add(url.getFile());
        }
    }
}
