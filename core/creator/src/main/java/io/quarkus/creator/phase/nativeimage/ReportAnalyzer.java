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

package io.quarkus.creator.phase.nativeimage;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ReportAnalyzer {

    static Pattern PATTERN = Pattern.compile("(.*?)([^\\s(]+)\\.([^.]+\\(.*?\\):[^\\s])");

    /**
     * Analyze the contents of the call tree report produced by Substrate when using -H:+PrintAnalysisCallTree,
     * and does a more meaningful analysis of what is causing a type to be retained.
     *
     * In particular for virtual or interface methods that have multiple implementations what is calling this method
     * is not really important, its what caused this particular instance of the class to be created that is important
     * (e.g. if you have an instance of Runnable, you don't care about all the different parts that call runnable, you
     * care about what created this particular instance).
     *
     * If a virtual or interface call is detected with multiple implementations then printing the current call flow
     * is abandoned, and instead the call flow for the constructor of the current object is printed instead.
     *
     */
    public static String analyse(String report, String className, String methodName) throws Exception {
        Deque<String> lines = new ArrayDeque<>();
        try (BufferedReader in = Files.newBufferedReader(Paths.get(report))) {
            for (String re = in.readLine(); re != null; re = in.readLine()) {
                lines.add(re);
            }
        }

        String first = lines.pop();
        if (!first.equals("VM Entry Points")) {
            throw new IllegalArgumentException("Unexpected first line in file " + first);
        }
        List<Node> parents = new ArrayList<>();
        Map<String, List<Node>> byClassMap = new HashMap<>();
        Map<String, List<Node>> constructors = new HashMap<>();
        Node last = null;
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }
            int start = 0;
            int lc = 0;
            for (; start < line.length(); ++start) {
                char c = line.charAt(start);
                if (c == '├' || c == '└') {
                    break;
                } else {
                    lc++;
                }
            }
            if (line.length() < start + 3) {
                continue;
            }
            Matcher matcher = PATTERN.matcher(line.substring(start + 3));
            if (!matcher.find()) {
                throw new RuntimeException("Failed " + line);
            }
            String type = matcher.group(1).trim();
            String clz = matcher.group(2);
            String method = matcher.group(3);
            Node parent;
            if (last == null) {
                parent = null;
            } else if (last.indent < lc) {
                parent = last;
            } else {
                parent = last;
                while (parent != null) {
                    parent = parent.parent;
                    if (parent == null || parent.indent < lc) {
                        break;
                    }
                }
            }
            Node n = new Node(lc, type, clz, method, parent);
            if (parent == null) {
                parents.add(n);
            } else {
                n.parent.children.add(n);
            }
            byClassMap.computeIfAbsent(clz, (k) -> new ArrayList<>()).add(n);
            if (method.startsWith("<init>")) {
                constructors.computeIfAbsent(clz, (k) -> new ArrayList<>()).add(n);
            }
            last = n;
        }

        List<Node> dm = byClassMap.getOrDefault(className, new ArrayList<>()).stream()
                .filter((s) -> s.method.startsWith(methodName + "(")).collect(Collectors.toList());

        Deque<Node> runQueue = new ArrayDeque<>(dm);
        Set<String> attemptedClasses = new HashSet<>();
        if (methodName.equals("<init>")) {
            attemptedClasses.add(className);
        }
        StringBuilder ret = new StringBuilder();
        StringBuilder sb = new StringBuilder();
        while (!runQueue.isEmpty()) {
            Node current = runQueue.pop();
            sb.append("Possible path to " + current.className + "." + current.method);
            while (current != null) {
                sb.append("\t" + current.className + "." + current.method + '\n');

                String reason = null;
                if (current.parent == null || current.parent.children.size() > 1) {
                    if (current.type.equals("is overridden by")) {
                        reason = "This is an implementation of " + current.parent.className
                                + " printing path to constructors of " + current.className;
                    } else if (current.type.equals("is implemented by")) {
                        reason = "This is an implementation of " + current.parent.className
                                + " printing path to constructors of " + current.className;
                    }
                }
                if (reason != null) {
                    if (!attemptedClasses.contains(current.className)) {
                        attemptedClasses.add(current.className);
                        List<Node> toAdd = constructors.getOrDefault(current.className, new ArrayList<>());
                        runQueue.addAll(toAdd);
                        sb.append(reason + '\n');
                        sb.append("\n");
                        ret.append(sb);
                    }
                    //note that we discard the string builder if it is part of attemptedClasses, as this basically
                    //represents an alternate path that we have already displayed
                    sb.setLength(0);
                    break;
                }

                current = current.parent;

            }
        }
        ret.append(sb);
        return ret.toString();
    }

    public static class Node {
        final int indent;
        final String type;
        final String className;
        final String method;
        final Node parent;
        List<Node> children = new ArrayList<>();

        Node(int indent, String type, String className, String method, Node parent) {
            this.indent = indent;
            this.type = type;
            this.className = className;
            this.method = method;
            this.parent = parent;
        }

        @Override
        public String toString() {
            return className + '.' + method;
        }
    }
}
