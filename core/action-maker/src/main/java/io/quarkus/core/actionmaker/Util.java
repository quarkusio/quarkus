package io.quarkus.core.actionmaker;

final class Util {
    private Util() {
    }

    static String plural(String str, int cnt) {
        return cnt == 1 ? str : str.endsWith("y") ? str.substring(0, str.length() - 1) + "ies" : str + "s";
    }

    static String ord(int i) {
        return switch (i) {
            case 10 -> "tenth";
            case 11 -> "eleventh";
            case 12 -> "twelfth";
            case 13 -> "thirteenth";
            case 14 -> "fourteenth";
            case 15 -> "fifteenth";
            case 16 -> "sixteenth";
            case 17 -> "seventeenth";
            case 18 -> "eighteenth";
            case 19 -> "nineteenth";
            case 20 -> "twentieth";
            case 30 -> "thirtieth";
            case 40 -> "fortieth";
            case 50 -> "fiftieth";
            case 60 -> "sixtieth";
            case 70 -> "seventieth";
            case 80 -> "eightieth";
            case 90 -> "nineteenth";
            default -> switch (i / 10) {
                case 2 -> "twenty-";
                case 3 -> "thirty-";
                case 4 -> "forty-";
                case 5 -> "fifty-";
                case 6 -> "sixty-";
                case 7 -> "seventy-";
                case 8 -> "eighty-";
                case 9 -> "ninety-";
                default -> "";
            } + switch (i % 10) {
                case 1 -> "first";
                case 2 -> "second";
                case 3 -> "third";
                case 4 -> "fourth";
                case 5 -> "fifth";
                case 6 -> "sixth";
                case 7 -> "seventh";
                case 8 -> "eighth";
                case 9 -> "ninth";
                default -> throw new IllegalStateException();
            };
        };
    }
}
