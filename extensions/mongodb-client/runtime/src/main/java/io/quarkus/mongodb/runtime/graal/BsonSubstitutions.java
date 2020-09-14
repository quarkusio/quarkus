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
        final Class<?> jsonTokenTypeClass = classForName("org.bson.json.JsonTokenType");
        Reflections.invokeMethod(JsonReader.class, "verifyToken", new Class<?>[] {}, this,
                new Object[] { Reflections.getEnumConstant("LEFT_PAREN", jsonTokenTypeClass) });
        // verifyToken(JsonTokenType.LEFT_PAREN);

        Object token = Reflections.invokeMethod(JsonReader.class, "popToken", new Class<?>[] {}, this, new Object[] {});
        // JsonToken token = popToken();
        final Class<?> jsonTokenClass = classForName("org.bson.json.JsonToken");
        Object tokenType = Reflections.invokeMethod(jsonTokenClass, "getType", new Class<?>[] {}, token, new Object[] {});
        if (tokenType == Reflections.getEnumConstant("RIGHT_PAREN", jsonTokenTypeClass)) {
            return new Date().getTime();
        } else if (tokenType != Reflections.getEnumConstant("String", jsonTokenTypeClass)) {
            throw new JsonParseException("JSON reader expected a string but found '%s'.",
                    Reflections.invokeMethod(jsonTokenClass, "getValue", new Class<?>[] {}, token, new Object[] {}));
            // throw new JsonParseException("JSON reader expected a string but found '%s'.", token.getValue());
        }

        Reflections.invokeMethod(JsonReader.class, "verifyToken", new Class<?>[] {}, this,
                new Object[] { Reflections.getEnumConstant("RIGHT_PAREN", jsonTokenTypeClass) });
        // verifyToken(JsonTokenType.RIGHT_PAREN);
        String[] patterns = { "yyyy-MM-dd", "yyyy-MM-dd'T'HH:mm:ssz", "yyyy-MM-dd'T'HH:mm:ss.SSSz" };

        SimpleDateFormat format = new SimpleDateFormat(patterns[0], Locale.ENGLISH);
        ParsePosition pos = new ParsePosition(0);
        String s = (String) Reflections.invokeMethod(jsonTokenClass, "getValue", new Class<?>[] { Class.class }, token,
                new Object[] { String.class });
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

    private Class<?> classForName(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
