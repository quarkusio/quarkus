package io.quarkus.qute;

/**
 * Parser configuration.
 * <p>
 * The set of possible expression commands is limited.
 * Only ASCII characters are allowed.
 * Some characters are reserved: {@code #}, {@code /}, {@code @}, {@code _}, {@code |}, {@code !}.
 * Digit/alphabetic chars are also disallowed.
 *
 * @param expressionCommand the command characted used to identify an output expression
 */
public record ParserConfig(Character expressionCommand) {

    /**
     * By default, no special expression command is used.
     */
    public static final ParserConfig DEFAULT = new ParserConfig(null);

    public ParserConfig {
        if (expressionCommand != null) {
            char command = expressionCommand.charValue();
            if (command < 0 || command > 127) {
                throw new IllegalArgumentException("Expresion command must be an ASCII char: " + command);
            }
            if (Parser.Tag.isCommand(command, null)
                    || command == Parser.COMMENT_DELIMITER
                    || command == Parser.CDATA_START_DELIMITER
                    || command == Parser.UNDERSCORE) {
                throw new IllegalArgumentException("Expresion command is reserved: " + command);
            }
            if (Character.isDigit(command)
                    || Character.isAlphabetic(command)) {
                throw new IllegalArgumentException("Expresion command must not be a digit/alphabetic: " + command);
            }
        }
    }

}