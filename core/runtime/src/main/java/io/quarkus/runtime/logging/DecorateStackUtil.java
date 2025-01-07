package io.quarkus.runtime.logging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class DecorateStackUtil {

    public static String getDecoratedString(final Throwable throwable, String srcMainJava, List<String> knowClasses) {
        if (srcMainJava != null) {
            return getDecoratedString(throwable, Path.of(srcMainJava), knowClasses);
        }
        return null;
    }

    public static String getDecoratedString(final Throwable throwable, Path srcMainJava, List<String> knowClasses) {
        if (knowClasses != null && !knowClasses.isEmpty() && throwable != null) {
            StackTraceElement[] stackTrace = throwable.getStackTrace();
            for (StackTraceElement elem : stackTrace) {
                if (knowClasses.contains(elem.getClassName())) {
                    String decoratedString = getDecoratedString(srcMainJava, elem);
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
            try {
                List<String> contextLines = getRelatedLinesInSource(f, lineNumber);
                if (contextLines != null) {
                    String header = "Exception in " + stackTraceElement.getFileName() + ":" + stackTraceElement.getLineNumber();
                    return header + "\n" + String.join("\n", contextLines);
                }
            } catch (IOException ignored) {
                // Could not find the source for some reason. Just return nothing then
            }
        }
        return null;
    }

    private static List<String> getRelatedLinesInSource(Path filePath, int lineNumber) throws IOException {
        if (Files.exists(filePath)) {
            Deque<String> contextQueue = new ArrayDeque<>(2 * 2 + 1);
            String line;
            int currentLine = 1;
            while ((line = Files.newBufferedReader(filePath).readLine()) != null) {
                if (currentLine >= lineNumber - 2) {
                    String ln = String.valueOf(currentLine);
                    if (currentLine == lineNumber) {
                        ln = "→ " + ln + "  ";
                    } else {
                        ln = "  " + ln + "  ";
                    }

                    contextQueue.add("\t" + ln + line);
                }
                if (currentLine >= lineNumber + 2) {
                    break;
                }
                currentLine++;
            }
            return new ArrayList<>(contextQueue);
        }
        return null;
    }

    private static String getFullPath(String fullClassName, String fileName) {
        int lastDotIndex = fullClassName.lastIndexOf(".");
        String packageName = fullClassName.substring(0, lastDotIndex);
        String path = packageName.replace('.', '/');
        return path + "/" + fileName;
    }

}
