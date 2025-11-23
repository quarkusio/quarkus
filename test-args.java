import java.util.List;
import java.util.ArrayList;

class TestArgs {
    public static void main(String[] args) {
        System.out.println("Testing argument parsing...");

        // Test simple args
        List<String> result1 = parseArgs("-H:IncludeLocales=en -H:+ReportExceptionStackTraces");
        System.out.println("Test 1: " + result1);
        System.out.println("Expected: [-H:IncludeLocales=en, -H:+ReportExceptionStackTraces]");
        System.out.println("Match: " + result1.equals(List.of("-H:IncludeLocales=en", "-H:+ReportExceptionStackTraces")));

        // Test quoted args
        List<String> result2 = parseArgs("-H:IncludeLocales=\"en,ar\" -H:+ReportExceptionStackTraces");
        System.out.println("Test 2: " + result2);
        System.out.println("Expected: [-H:IncludeLocales=\"en,ar\", -H:+ReportExceptionStackTraces]");
        System.out.println("Match: " + result2.equals(List.of("-H:IncludeLocales=\"en,ar\"", "-H:+ReportExceptionStackTraces")));
    }

    public static List<String> parseArgs(String argsValue) {
        // Simple argument parsing - split on spaces but preserve quoted strings
        List<String> args = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = '"';

        for (char c : argsValue.toCharArray()) {
            if (!inQuotes && Character.isWhitespace(c)) {
                if (current.length() > 0) {
                    args.add(current.toString());
                    current.setLength(0);
                }
            } else if ((c == '"' || c == '\'') && (!inQuotes || c == quoteChar)) {
                inQuotes = !inQuotes;
                if (inQuotes) {
                    quoteChar = c;
                }
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            args.add(current.toString());
        }

        return args;
    }
}
