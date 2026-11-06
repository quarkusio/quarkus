package io.quarkus.jdbc.oracle.runtime;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import oracle.sql.CharacterSet;

public class OracleInitRecorder {

    public static void setupCharSets(boolean addAllCharsets) {
        if (addAllCharsets) {
            for (short id : reflectivelyReadAllCharacterSetIdentifiers()) {
                oracle.sql.CharacterSet.make(id);
            }
        } else {
            //By default, support at least the following charsets;
            //without these, we're unable to connect to an Oracle container image which is using default settings.
            oracle.sql.CharacterSet.make(CharacterSet.AL32UTF8_CHARSET);
            oracle.sql.CharacterSet.make(CharacterSet.AL16UTF16_CHARSET);
        }
    }

    private static List<Short> reflectivelyReadAllCharacterSetIdentifiers() {
        final Field[] fields = CharacterSet.class.getFields();
        final ArrayList<Short> collectedIds = new ArrayList<>(fields.length);
        for (Field field : fields) {
            if (field.getType() == short.class && field.getName().endsWith("_CHARSET")) {
                try {
                    collectedIds.add(field.getShort(null));
                } catch (IllegalAccessException e) {
                    //ignore
                }
            }
        }
        return collectedIds;
    }

}
