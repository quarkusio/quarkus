package io.quarkus.jdbc.oracle.runtime;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.runtime.annotations.Recorder;
import oracle.sql.CharacterSet;

@Recorder
public class OracleInitRecorder {

    public void setupCharSets(boolean addAllCharsets) {
        if (addAllCharsets) {
            for (short id : reflectivelyReadAllCharacterSetIdentifiers()) {
                oracle.sql.CharacterSet.make(id);
            }
        } else {
            // By default, support at least the following charsets;
            // without these, we're unable to connect to an Oracle container image which is using default settings.
            oracle.sql.CharacterSet.make(CharacterSet.AL32UTF8_CHARSET);
            oracle.sql.CharacterSet.make(CharacterSet.AL16UTF16_CHARSET);
        }
    }

    private List<Short> reflectivelyReadAllCharacterSetIdentifiers() {
        final Field[] fields = CharacterSet.class.getFields();
        final ArrayList<Short> collectedIds = new ArrayList<>(fields.length);
        for (Field field : fields) {
            if (field.getType() == short.class && field.getName().endsWith("_CHARSET")) {
                try {
                    collectedIds.add(field.getShort(null));
                } catch (IllegalAccessException e) {
                    // ignore
                }
            }
        }
        return collectedIds;
    }

}
