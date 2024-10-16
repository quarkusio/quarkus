package io.quarkus.runtime.logging;

import java.io.BufferedReader;
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
            try {
                List<String> contextLines = DecorateStackUtil.getRelatedLinesInSource(f, lineNumber, 2);
                if (contextLines != null) {
                    String header = "Exception in " + stackTraceElement.getFileName() + ":" + stackTraceElement.getLineNumber();
                    return header + "\n" + String.join("\n", contextLines);
                }
            } catch (IOException e) {
                // Could not find the source for some reason. Just return nothing then
            }
        }
        return null;
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
        String packageName = fullClassName.substring(0, lastDotIndex);
        String path = packageName.replace('.', '/');
        return path + "/" + fileName;
    }

}
