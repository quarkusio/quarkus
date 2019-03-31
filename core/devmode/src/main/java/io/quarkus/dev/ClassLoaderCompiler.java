/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.dev;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Class that handles compilation of source files
 *
 * @author Stuart Douglas
 */
public class ClassLoaderCompiler {

    public static final String DEV_MODE_CLASS_PATH = "META-INF/dev-mode-class-path.txt";
    private final List<CompilationProvider> compilationProviders;
    private final CompilationProvider.Context compilationContext;
    private final Set<String> allHandledExtensions;

    public ClassLoaderCompiler(ClassLoader classLoader, File outputDirectory, List<CompilationProvider> compilationProviders)
            throws IOException {
        this.compilationProviders = compilationProviders;

        List<URL> urls = new ArrayList<>();
        ClassLoader c = classLoader;
        while (c != null) {
            if (c instanceof URLClassLoader) {
                urls.addAll(Arrays.asList(((URLClassLoader) c).getURLs()));
            }
            c = c.getParent();
        }

        try (InputStream devModeCp = classLoader.getResourceAsStream(DEV_MODE_CLASS_PATH)) {
            BufferedReader r = new BufferedReader(new InputStreamReader(devModeCp, StandardCharsets.UTF_8));
            String cp = r.readLine();
            for (String i : cp.split(" ")) {
                urls.add(new URI(i).toURL());
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        Set<String> parsedFiles = new HashSet<>();
        Deque<String> toParse = new ArrayDeque<>();
        for (URL url : urls) {
            toParse.add(new File(url.getPath()).getAbsolutePath());
        }
        Set<File> classPathElements = new HashSet<>();
        classPathElements.add(outputDirectory);
        while (!toParse.isEmpty()) {
            String s = toParse.poll();
            if (!parsedFiles.contains(s)) {
                parsedFiles.add(s);
                File file = new File(s);
                if (!file.exists()) {
                    continue;
                }
                if (file.isDirectory()) {
                    classPathElements.add(file);
                } else if (file.getName().endsWith(".jar")) {
                    classPathElements.add(file);
                    if (!file.isDirectory() && file.getName().endsWith(".jar")) {
                        try (JarFile jar = new JarFile(file)) {
                            Manifest mf = jar.getManifest();
                            if (mf == null || mf.getMainAttributes() == null) {
                                continue;
                            }
                            Object classPath = mf.getMainAttributes().get(Attributes.Name.CLASS_PATH);
                            if (classPath != null) {
                                for (String i : classPath.toString().split(" ")) {
                                    File f;
                                    try {
                                        URL u = new URL(i);
                                        f = new File(u.getPath());
                                    } catch (MalformedURLException e) {
                                        f = new File(file.getParentFile(), i);
                                    }
                                    if (f.exists()) {
                                        toParse.add(f.getAbsolutePath());
                                    }
                                }
                            }
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to open class path file " + file, e);
                        }
                    }
                }
            }
        }

        this.compilationContext = new CompilationProvider.Context(classPathElements, outputDirectory);
        this.allHandledExtensions = new HashSet<>();
        for (CompilationProvider compilationProvider : compilationProviders) {
            allHandledExtensions.add(compilationProvider.handledExtension());
        }
    }

    public Set<String> allHandledExtensions() {
        return allHandledExtensions;
    }

    public void compile(Map<String, Set<File>> extensionToChangedFiles) {
        for (String extension : extensionToChangedFiles.keySet()) {
            for (CompilationProvider compilationProvider : compilationProviders) {
                if (extension.equals(compilationProvider.handledExtension())) {
                    compilationProvider.compile(extensionToChangedFiles.get(extension), compilationContext);
                    break;
                }
            }
        }
    }
}
