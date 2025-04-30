package io.quarkus.deployment.types;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.quarkus.runtime.types.GenericArrayTypeImpl;
import io.quarkus.runtime.types.ParameterizedTypeImpl;
import io.quarkus.runtime.types.WildcardTypeImpl;

/**
 * Creates a {@link Type} by parsing the given string according to the following grammar:
 *
 * <pre>
 * Type -> VoidType | PrimitiveType | ReferenceType
 * VoidType -> 'void'
 * PrimitiveType -> 'boolean' | 'byte' | 'short' | 'int'
 *                | 'long' | 'float' | 'double' | 'char'
 * ReferenceType -> PrimitiveType ('[' ']')+
 *                | ClassType ('<' TypeArgument (',' TypeArgument)* '>')? ('[' ']')*
 * ClassType -> FULLY_QUALIFIED_NAME
 * TypeArgument -> ReferenceType | WildcardType
 * WildcardType -> '?' | '?' ('extends' | 'super') ReferenceType
 * </pre>
 *
 * Notice that the resulting type never contains type variables, only "proper" types.
 * Also notice that the grammar above does not support all kinds of nested types;
 * it should be possible to add that later, if there's an actual need.
 * <p>
 * Types produced by this parser can be transferred from build time to runtime
 * via the recorder mechanism.
 */
public class TypeParser {
    public static Type parse(String str) {
        return new TypeParser(str).parse();
    }

    private final String str;

    private int pos = 0;

    private TypeParser(String str) {
        this.str = Objects.requireNonNull(str);
    }

    private Type parse() {
        Type result;

        String token = nextToken();
        if (token.isEmpty()) {
            throw unexpected(token);
        } else if (token.equals("void")) {
            result = void.class;
        } else if (isPrimitiveType(token) && peekToken().isEmpty()) {
            result = parsePrimitiveType(token);
        } else {
            result = parseReferenceType(token);
        }

        expect("");
        return result;
    }

    private Type parseReferenceType(String token) {
        if (isPrimitiveType(token)) {
            Type primitive = parsePrimitiveType(token);
            return parseArrayType(primitive);
        } else if (isClassType(token)) {
            Type result = parseClassType(token);
            if (peekToken().equals("<")) {
                expect("<");
                List<Type> typeArguments = new ArrayList<>();
                typeArguments.add(parseTypeArgument());
                while (peekToken().equals(",")) {
                    expect(",");
                    typeArguments.add(parseTypeArgument());
                }
                expect(">");
                result = new ParameterizedTypeImpl(result, typeArguments.toArray(Type[]::new));
            }
            if (peekToken().equals("[")) {
                return parseArrayType(result);
            }
            return result;
        } else {
            throw unexpected(token);
        }
    }

    private Type parseArrayType(Type elementType) {
        expect("[");
        expect("]");
        int dimensions = 1;
        while (peekToken().equals("[")) {
            expect("[");
            expect("]");
            dimensions++;
        }

        if (elementType instanceof Class<?> clazz) {
            return parseClassType("[".repeat(dimensions)
                    + (clazz.isPrimitive() ? clazz.descriptorString() : "L" + clazz.getName() + ";"));
        } else {
            Type result = elementType;
            for (int i = 0; i < dimensions; i++) {
                result = new GenericArrayTypeImpl(result);
            }
            return result;
        }
    }

    private Type parseTypeArgument() {
        String token = nextToken();
        if (token.equals("?")) {
            if (peekToken().equals("extends")) {
                expect("extends");
                Type bound = parseReferenceType(nextToken());
                return WildcardTypeImpl.withUpperBound(bound);
            } else if (peekToken().equals("super")) {
                expect("super");
                Type bound = parseReferenceType(nextToken());
                return WildcardTypeImpl.withLowerBound(bound);
            } else {
                return WildcardTypeImpl.defaultInstance();
            }
        } else {
            return parseReferenceType(token);
        }
    }

    private boolean isPrimitiveType(String token) {
        return token.equals("boolean")
                || token.equals("byte")
                || token.equals("short")
                || token.equals("int")
                || token.equals("long")
                || token.equals("float")
                || token.equals("double")
                || token.equals("char");
    }

    private Type parsePrimitiveType(String token) {
        return switch (token) {
            case "boolean" -> boolean.class;
            case "byte" -> byte.class;
            case "short" -> short.class;
            case "int" -> int.class;
            case "long" -> long.class;
            case "float" -> float.class;
            case "double" -> double.class;
            case "char" -> char.class;
            default -> throw unexpected(token);
        };
    }

    private boolean isClassType(String token) {
        return !token.isEmpty() && Character.isJavaIdentifierStart(token.charAt(0));
    }

    private Type parseClassType(String token) {
        try {
            return Class.forName(token, true, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Unknown class: " + token, e);
        }
    }

    // ---

    private void expect(String expected) {
        String token = nextToken();
        if (!expected.equals(token)) {
            throw unexpected(token);
        }
    }

    private IllegalArgumentException unexpected(String token) {
        if (token.isEmpty()) {
            throw new IllegalArgumentException("Unexpected end of input: " + str);
        }
        return new IllegalArgumentException("Unexpected token '" + token + "' at position " + (pos - token.length())
                + ": " + str);
    }

    private String peekToken() {
        // skip whitespace
        while (pos < str.length() && Character.isWhitespace(str.charAt(pos))) {
            pos++;
        }

        // end of input
        if (pos == str.length()) {
            return "";
        }

        int pos = this.pos;

        // current char is a token on its own
        if (isSpecial(str.charAt(pos))) {
            return str.substring(pos, pos + 1);
        }

        // token is a keyword or fully qualified name
        int begin = pos;
        while (pos < str.length() && Character.isJavaIdentifierStart(str.charAt(pos))) {
            do {
                pos++;
            } while (pos < str.length() && Character.isJavaIdentifierPart(str.charAt(pos)));

            if (pos < str.length() && str.charAt(pos) == '.') {
                pos++;
            } else {
                return str.substring(begin, pos);
            }
        }

        if (pos == str.length()) {
            throw new IllegalArgumentException("Unexpected end of input: " + str);
        }
        throw new IllegalArgumentException("Unexpected character '" + str.charAt(pos) + "' at position " + pos + ": " + str);
    }

    private String nextToken() {
        String result = peekToken();
        pos += result.length();
        return result;
    }

    private boolean isSpecial(char c) {
        return c == ',' || c == '?' || c == '<' || c == '>' || c == '[' || c == ']';
    }
}
