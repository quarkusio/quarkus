package io.quarkus.runtime.logging;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;

public class DecorateStackUtil {

    public static String getDecoratedString(final Throwable throwable, String srcMainJava, List<String> knowClasses) {
        if (srcMainJava != null) {
            return DecorateStackUtil.getDecoratedString(throwable, Path.of(srcMainJava), knowClasses);
        }
        return null;
    }

    public static String getDecoratedString(final Throwable throwable, Path srcMainJava, List<String> knowClasses) {
        if (knowClasses != null && !knowClasses.isEmpty() && throwable != null) {
            StackTraceElement[] stackTrace = throwable.getStackTrace();
            for (int i = 0; i < stackTrace.length; ++i) {
                StackTraceElement elem = stackTrace[i];
                if (knowClasses.contains(elem.getClassName())) {
                    String decoratedString = DecorateStackUtil.getDecoratedString(srcMainJava, elem);
                    if (decoratedString != null) {
                        return decoratedString;
                    }
                }
            }
        }

        return null;
    }

    public static String getDecoratedString(Path srcMainJava, StackTraceElement stackTraceElement) {
        int lineNumber = stackTraceElement.getLineNumber();
        if (lineNumber > 0 && srcMainJava != null) {
            String fullJavaFileName = getFullPath(stackTraceElement.getClassName(), stackTraceElement.getFileName());
            Path f = srcMainJava.resolve(fullJavaFileName);
            return getDecoratedString(stackTraceElement, f, lineNumber);
        }
        return null;
    }

    public static String getDecoratedString(StackTraceElement stackTraceElement, List<Path> workspacePaths) {
        if (stackTraceElement == null || workspacePaths == null || workspacePaths.isEmpty()) {
            return null;
        }

        int lineNumber = stackTraceElement.getLineNumber();

        if (lineNumber > 0) {
            // Convert the class name to a relative path: io.quarkiverse.chappie.sample.ChappieSimulateResource -> io/quarkiverse/chappie/sample/ChappieSimulateResource.java
            String fullJavaFileName = getFullPath(stackTraceElement.getClassName(), stackTraceElement.getFileName());
            Path affectedPath = findAffectedPath(fullJavaFileName, workspacePaths);
            return getDecoratedString(stackTraceElement, affectedPath, lineNumber);
        }
        return null;
    }

    public static String getDecoratedString(StackTraceElement stackTraceElement, Path f, int lineNumber) {
        try {
            List<String> contextLines = DecorateStackUtil.getRelatedLinesInSource(f, lineNumber, 2);
            if (contextLines != null) {
                String header = "Exception in " + stackTraceElement.getFileName() + ":" + stackTraceElement.getLineNumber();
                return header + "\n" + String.join("\n", contextLines);
            }
        } catch (IOException e) {
            // Could not find the source for some reason. Just return nothing then
        }
        return null;
    }

    public static Path findAffectedPath(String name, List<Path> workspacePaths) {
        // Search the workspace paths for a match that ends with the fullJavaFileName
        return workspacePaths.stream()
                .filter(path -> path.toString().endsWith(convertNameToRelativePath(name)))
                .sorted(Comparator.comparingInt(path -> path.toString().contains("src/main/java") ? 0 : 1)) // Prioritize src/main/java
                .findFirst()
                .orElse(null);
    }

    private static String convertNameToRelativePath(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Class name cannot be null or empty");
        }
        if (!isPathString(name)) {
            return name.replace('.', '/') + ".java";
        }
        return name;
    }

    private static boolean isPathString(String name) {
        return name.contains("/") && name.endsWith(".java");
    }

    private static List<String> getRelatedLinesInSource(Path filePath, int lineNumber, int contextRange) throws IOException {
        if (Files.exists(filePath)) {
            List<String> resultLines = new ArrayList<>();
            Deque<String> contextQueue = new ArrayDeque<>(2 * contextRange + 1);
            try (BufferedReader reader = Files.newBufferedReader(filePath)) {
                String line;
                int currentLine = 1;
                while ((line = reader.readLine()) != null) {
                    if (currentLine >= lineNumber - contextRange) {
                        String ln = String.valueOf(currentLine);
                        if (currentLine == lineNumber) {
                            ln = "→ " + ln + "  ";
                        } else {
                            ln = "  " + ln + "  ";
                        }

                        contextQueue.add("\t" + ln + line);
                    }
                    if (currentLine >= lineNumber + contextRange) {
                        break;
                    }
                    currentLine++;
                }
                resultLines.addAll(contextQueue);
            }
            return resultLines;
        }
        return null;
    }

    private static String getFullPath(String fullClassName, String fileName) {
        int lastDotIndex = fullClassName.lastIndexOf(".");
        if (lastDotIndex == -1) {
            return fileName;
        }
        String packageName = fullClassName.substring(0, lastDotIndex);
        String path = packageName.replace('.', '/');
        return path + "/" + fileName;
    }

}