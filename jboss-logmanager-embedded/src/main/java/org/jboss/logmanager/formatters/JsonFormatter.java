/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2017 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.logmanager.formatters;

import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;

import org.jboss.logmanager.PropertyValues;

/**
 * A formatter that outputs the record into JSON format optionally printing details.
 * <p>
 * Note that including details can be expensive in terms of calculating the caller.
 * </p>
 * <p>The details include;</p>
 * <ul>
 * <li>{@link org.jboss.logmanager.ExtLogRecord#getSourceClassName() source class name}</li>
 * <li>{@link org.jboss.logmanager.ExtLogRecord#getSourceFileName() source file name}</li>
 * <li>{@link org.jboss.logmanager.ExtLogRecord#getSourceMethodName() source method name}</li>
 * <li>{@link org.jboss.logmanager.ExtLogRecord#getSourceLineNumber() source line number}</li>
 * <li>{@link org.jboss.logmanager.ExtLogRecord#getSourceModuleName() source module name}</li>
 * <li>{@link org.jboss.logmanager.ExtLogRecord#getSourceModuleVersion() source module version}</li>
 * </ul>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class JsonFormatter extends StructuredFormatter {

    private final Map<String, Object> config;

    private JsonGeneratorFactory factory;

    /**
     * Creates a new JSON formatter.
     */
    public JsonFormatter() {
        config = new HashMap<>();
        factory = Json.createGeneratorFactory(config);
    }

    /**
     * Creates a new JSON formatter.
     *
     * @param keyOverrides a string representation of a map to override keys
     *
     * @see PropertyValues#stringToEnumMap(Class, String)
     */
    public JsonFormatter(final String keyOverrides) {
        super(keyOverrides);
        config = new HashMap<>();
        factory = Json.createGeneratorFactory(config);
    }

    /**
     * Creates a new JSON formatter.
     *
     * @param keyOverrides a map of overrides for the default keys
     */
    public JsonFormatter(final Map<Key, String> keyOverrides) {
        super(keyOverrides);
        config = new HashMap<>();
        factory = Json.createGeneratorFactory(config);
    }

    /**
     * Indicates whether or not pretty printing is enabled.
     *
     * @return {@code true} if pretty printing is enabled, otherwise {@code false}
     */
    public boolean isPrettyPrint() {
        synchronized (config) {
            return (config.containsKey(javax.json.stream.JsonGenerator.PRETTY_PRINTING) ? (Boolean) config.get(javax.json.stream.JsonGenerator.PRETTY_PRINTING) : false);
        }
    }

    /**
     * Turns on or off pretty printing.
     *
     * @param prettyPrint {@code true} to turn on pretty printing or {@code false} to turn it off
     */
    public void setPrettyPrint(final boolean prettyPrint) {
        synchronized (config) {
            if (prettyPrint) {
                config.put(JsonGenerator.PRETTY_PRINTING, true);
            } else {
                config.remove(JsonGenerator.PRETTY_PRINTING);
            }
            factory = Json.createGeneratorFactory(config);
        }
    }

    @Override
    protected Generator createGenerator(final Writer writer) {
        final JsonGeneratorFactory factory;
        synchronized (config) {
            factory = this.factory;
        }
        return new FormatterJsonGenerator(factory.createGenerator(writer));
    }

    private class FormatterJsonGenerator implements Generator {
        private final JsonGenerator generator;

        private FormatterJsonGenerator(final JsonGenerator generator) {
            this.generator = generator;
        }

        @Override
        public Generator begin() {
            generator.writeStartObject();
            return this;
        }

        @Override
        public Generator add(final String key, final int value) {
            generator.write(key, value);
            return this;
        }

        @Override
        public Generator add(final String key, final long value) {
            generator.write(key, value);
            return this;
        }

        @Override
        public Generator add(final String key, final Map<String, ?> value) {
            generator.writeStartObject(key);
            if (value != null) {
                for (Map.Entry<String, ?> entry : value.entrySet()) {
                    writeObject(entry.getKey(), entry.getValue());
                }
            }
            generator.writeEnd();
            return this;
        }

        @Override
        public Generator add(final String key, final String value) {
            if (value == null) {
                generator.writeNull(key);
            } else {
                generator.write(key, value);
            }
            return this;
        }

        @Override
        public Generator startObject(final String key) throws Exception {
            if (key == null) {
                generator.writeStartObject();
            } else {
                generator.writeStartObject(key);
            }
            return this;
        }

        @Override
        public Generator endObject() throws Exception {
            generator.writeEnd();
            return this;
        }

        @Override
        public Generator startArray(final String key) throws Exception {
            if (key == null) {
                generator.writeStartArray();
            } else {
                generator.writeStartArray(key);
            }
            return this;
        }

        @Override
        public Generator endArray() throws Exception {
            generator.writeEnd();
            return this;
        }

        @Override
        public Generator end() {
            generator.writeEnd(); // end record
            generator.flush();
            generator.close();
            return this;
        }

        private void writeObject(final String key, final Object obj) {
            if (obj == null) {
                if (key == null) {
                    generator.writeNull();
                } else {
                    generator.writeNull(key);
                }
            } else if (obj instanceof Boolean) {
                final Boolean value = (Boolean) obj;
                if (key == null) {
                    generator.write(value);
                } else {
                    generator.write(key, value);
                }
            } else if (obj instanceof Integer) {
                final Integer value = (Integer) obj;
                if (key == null) {
                    generator.write(value);
                } else {
                    generator.write(key, value);
                }
            } else if (obj instanceof Long) {
                final Long value = (Long) obj;
                if (key == null) {
                    generator.write(value);
                } else {
                    generator.write(key, value);
                }
            } else if (obj instanceof Double) {
                final Double value = (Double) obj;
                if (key == null) {
                    generator.write(value);
                } else {
                    generator.write(key, value);
                }
            } else if (obj instanceof BigInteger) {
                final BigInteger value = (BigInteger) obj;
                if (key == null) {
                    generator.write(value);
                } else {
                    generator.write(key, value);
                }
            } else if (obj instanceof BigDecimal) {
                final BigDecimal value = (BigDecimal) obj;
                if (key == null) {
                    generator.write(value);
                } else {
                    generator.write(key, value);
                }
            } else if (obj instanceof String) {
                final String value = (String) obj;
                if (key == null) {
                    generator.write(value);
                } else {
                    generator.write(key, value);
                }
            } else if (obj instanceof JsonValue) {
                final JsonValue value = (JsonValue) obj;
                if (key == null) {
                    generator.write(value);
                } else {
                    generator.write(key, value);
                }
            } else {
                final String value = String.valueOf(obj);
                if (key == null) {
                    generator.write(value);
                } else {
                    generator.write(key, value);
                }
            }
        }
    }
}
