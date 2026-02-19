package io.quarkus.deployment.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class NativeImageMetadataParserStepTest {

    @TempDir
    Path tempDir;

    @Test
    public void testParseArgsSimple() {
        List<String> args = NativeImageMetadataParserStep.parseArgs("-H:IncludeLocales=en -H:+ReportExceptionStackTraces");
        assertEquals(List.of("-H:IncludeLocales=en", "-H:+ReportExceptionStackTraces"), args);
    }

    @Test
    public void testParseArgsQuoted() {
        List<String> args = NativeImageMetadataParserStep
                .parseArgs("-H:IncludeLocales=\"en,ar\" -H:+ReportExceptionStackTraces");
        assertEquals(List.of("-H:IncludeLocales=\"en,ar\"", "-H:+ReportExceptionStackTraces"), args);
    }

    @Test
    public void testParseArgsSingleQuoted() {
        List<String> args = NativeImageMetadataParserStep.parseArgs("-H:IncludeLocales='en,ar' -H:+ReportExceptionStackTraces");
        assertEquals(List.of("-H:IncludeLocales='en,ar'", "-H:+ReportExceptionStackTraces"), args);
    }

    @Test
    public void testParseArgsComplex() {
        List<String> args = NativeImageMetadataParserStep
                .parseArgs("-H:IncludeLocales=\"en,ar\" -Dprop=\"value with spaces\" -H:+ReportExceptionStackTraces");
        assertEquals(List.of("-H:IncludeLocales=\"en,ar\"", "-Dprop=\"value with spaces\"", "-H:+ReportExceptionStackTraces"),
                args);
    }

    @Test
    public void testParseArgsEmpty() {
        List<String> args = NativeImageMetadataParserStep.parseArgs("");
        assertTrue(args.isEmpty());
    }

    @Test
    public void testParseArgsWhitespaceOnly() {
        List<String> args = NativeImageMetadataParserStep.parseArgs("   ");
        assertTrue(args.isEmpty());
    }

    @Test
    public void testParseArgsMultipleSpaces() {
        List<String> args = NativeImageMetadataParserStep.parseArgs("-H:IncludeLocales=en    -H:+ReportExceptionStackTraces");
        assertEquals(List.of("-H:IncludeLocales=en", "-H:+ReportExceptionStackTraces"), args);
    }

    @Test
    public void testParseArgsUnclosedQuote() {
        // This should still work - unclosed quotes are preserved as-is
        List<String> args = NativeImageMetadataParserStep.parseArgs("-H:IncludeLocales=\"en,ar -H:+ReportExceptionStackTraces");
        assertEquals(List.of("-H:IncludeLocales=\"en,ar -H:+ReportExceptionStackTraces"), args);
    }

    @Test
    public void testParseArgsMixedQuotes() {
        List<String> args = NativeImageMetadataParserStep.parseArgs("-Dkey1=\"value1\" -Dkey2='value2' -Dkey3=value3");
        assertEquals(List.of("-Dkey1=\"value1\"", "-Dkey2='value2'", "-Dkey3=value3"), args);
    }

    @Test
    public void testParseArgsTrailingSpaces() {
        List<String> args = NativeImageMetadataParserStep.parseArgs("-H:IncludeLocales=en ");
        assertEquals(List.of("-H:IncludeLocales=en"), args);
    }

    @Test
    public void testParseArgsLeadingSpaces() {
        List<String> args = NativeImageMetadataParserStep.parseArgs(" -H:IncludeLocales=en");
        assertEquals(List.of("-H:IncludeLocales=en"), args);
    }

    @Test
    public void testParseArgsEmptyArgs() {
        List<String> args = NativeImageMetadataParserStep.parseArgs("-H:IncludeLocales= ");
        assertEquals(List.of("-H:IncludeLocales="), args);
    }
}
