package io.quarkus.mongodb.runtime.graal;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.bson.json.JsonParseException;
import org.bson.json.JsonReader;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.quarkus.arc.impl.Reflections;

public final class BsonSubstitutions {
}

@TargetClass(JsonReader.class)
final class JsonReaderSubstitution {
    @Substitute
    private long visitISODateTimeConstructor() {
        Util.jsonReaderVerifyToken("LEFT_PAREN", this);
        // verifyToken(JsonTokenType.LEFT_PAREN);

        Object token = Util.jsonReaderPopToken(this);
        // JsonToken token = popToken();
        Object tokenType = Util.jsonTokenGetType(token);
        if (tokenType == Util.jsonTokenType("RIGHT_PAREN")) {
            return new Date().getTime();
        } else if (tokenType != Util.jsonTokenType("STRING")) {
            throw new JsonParseException("JSON reader expected a string but found '%s'.", Util.jsonTokenGetValue(token));
            // throw new JsonParseException("JSON reader expected a string but found '%s'.", token.getValue());
        }

        Util.jsonReaderVerifyToken("RIGHT_PAREN", this);
        // verifyToken(JsonTokenType.RIGHT_PAREN);
        String[] patterns = { "yyyy-MM-dd", "yyyy-MM-dd'T'HH:mm:ssz", "yyyy-MM-dd'T'HH:mm:ss.SSSz" };

        SimpleDateFormat format = new SimpleDateFormat(patterns[0], Locale.ENGLISH);
        ParsePosition pos = new ParsePosition(0);
        String s = Util.jsonTokenGetValueString(token);
        // String s = token.getValue(String.class);

        if (s.endsWith("Z")) {
            s = s.substring(0, s.length() - 1) + "GMT-00:00";
        }

        for (final String pattern : patterns) {
            format.applyPattern(pattern);
            format.setLenient(true);
            pos.setIndex(0);

            Date date = format.parse(s, pos);

            // Substitution: skip checking length, return as soon as a date is available
            if (date != null) {
                return date.getTime();
            }
        }
        throw new JsonParseException("Invalid date format.");
    }

    // Holder class to keep helper methods (they cannot be added directly to the substituted class)
    private static class Util {
        static String jsonTokenGetValueString(Object token) {
            final Class<?> jsonTokenClass = Util.classForName("org.bson.json.JsonToken");
            return (String) Reflections.invokeMethod(jsonTokenClass, "getValue", new Class<?>[] { Class.class }, token,
                    new Object[] { String.class });
        }

        static Object jsonTokenGetValue(Object token) {
            final Class<?> jsonTokenClass = Util.classForName("org.bson.json.JsonToken");
            return Reflections.invokeMethod(jsonTokenClass, "getValue", new Class<?>[] {}, token, new Object[] {});
        }

        static Object jsonTokenGetType(Object token) {
            final Class<?> jsonTokenClass = Util.classForName("org.bson.json.JsonToken");
            return Reflections.invokeMethod(jsonTokenClass, "getType", new Class<?>[] {}, token, new Object[] {});
        }

        static Object jsonReaderPopToken(Object reader) {
            return Reflections.invokeMethod(JsonReader.class, "popToken", new Class<?>[] {}, reader, new Object[] {});
        }

        static Object jsonTokenType(String name) {
            final Class<?> jsonTokenTypeClass = Util.classForName("org.bson.json.JsonTokenType");
            return Reflections.getEnumConstant(name, jsonTokenTypeClass);
        }

        static void jsonReaderVerifyToken(String tokenName, Object reader) {
            final Class<?> jsonTokenTypeClass = Util.classForName("org.bson.json.JsonTokenType");
            Reflections.invokeMethod(JsonReader.class, "verifyToken", new Class<?>[] { jsonTokenTypeClass }, reader,
                    new Object[] { Reflections.getEnumConstant(tokenName, jsonTokenTypeClass) });
        }

        static Class<?> classForName(String className) {
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
