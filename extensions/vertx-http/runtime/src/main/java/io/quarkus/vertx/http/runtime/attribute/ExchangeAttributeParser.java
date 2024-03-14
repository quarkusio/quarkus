package io.quarkus.vertx.http.runtime.attribute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

import org.jboss.logging.Logger;

/**
 * Attribute parser for exchange attributes. This builds an attribute from a string definition.
 * <p>
 * This uses a service loader mechanism to allow additional token types to be loaded. Token definitions are loaded
 * from the provided class loader.
 *
 */
public class ExchangeAttributeParser {

    private static final Logger log = Logger.getLogger(ExchangeAttributeParser.class);

    private final List<ExchangeAttributeBuilder> builders;
    private final List<ExchangeAttributeWrapper> wrappers;

    public ExchangeAttributeParser(List<ExchangeAttributeWrapper> wrappers) {
        this(ExchangeAttributeParser.class.getClassLoader(), wrappers);
    }

    public ExchangeAttributeParser(final ClassLoader classLoader, List<ExchangeAttributeWrapper> wrappers) {
        this.wrappers = wrappers;
        ServiceLoader<ExchangeAttributeBuilder> loader = ServiceLoader.load(ExchangeAttributeBuilder.class, classLoader);
        final List<ExchangeAttributeBuilder> builders = new ArrayList<>();
        for (ExchangeAttributeBuilder instance : loader) {
            builders.add(instance);
        }
        //sort with the highest priority first
        builders.sort(new Comparator<ExchangeAttributeBuilder>() {
            @Override
            public int compare(ExchangeAttributeBuilder o1, ExchangeAttributeBuilder o2) {
                return Integer.compare(o2.priority(), o1.priority());
            }
        });
        this.builders = Collections.unmodifiableList(builders);

    }

    /**
     * Parses the provided value string, and turns it into a list of exchange attributes.
     * <p>
     * Tokens are created according to the following rules:
     * <p>
     * %<?a - % followed by an optional < and a single character. %% is an escape for a literal %
     * %{.*}a? - % plus curly braces with any amount of content inside, followed by an optional character
     * ${.*} - $ followed by a curly braces to reference an item from the predicate context
     *
     * @param valueString
     * @return
     */
    public ExchangeAttribute parse(final String valueString) {
        final List<ExchangeAttribute> attributes = new ArrayList<>();
        int pos = 0;
        int state = 0; //0 = literal, 1 = %, 2 = %{, 3 = $, 4 = ${, 5 = %<
        for (int i = 0; i < valueString.length(); ++i) {
            char c = valueString.charAt(i);
            switch (state) {
                case 0: {
                    if (c == '%' || c == '$') {
                        if (pos != i) {
                            attributes.add(wrap(parseSingleToken(valueString.substring(pos, i))));
                            pos = i;
                        }
                        if (c == '%') {
                            state = 1;
                        } else {
                            state = 3;
                        }
                    }
                    break;
                }
                case 1: {
                    if (c == '{') {
                        state = 2;
                    } else if (c == '<') {
                        state = 5;
                    } else if (c == '%') {
                        //literal percent
                        attributes.add(wrap(new ConstantExchangeAttribute("%")));
                        pos = i + 1;
                        state = 0;
                    } else {
                        attributes.add(wrap(parseSingleToken(valueString.substring(pos, i + 1))));
                        pos = i + 1;
                        state = 0;
                    }
                    break;
                }
                case 2: {
                    if (c == '}') {
                        attributes.add(wrap(parseSingleToken(valueString.substring(pos, i + 1))));
                        pos = i + 1;
                        state = 0;
                    }
                    break;
                }
                case 3: {
                    if (c == '{') {
                        state = 4;
                    } else if (c == '$') {
                        //literal dollars
                        attributes.add(wrap(new ConstantExchangeAttribute("$")));
                        pos = i + 1;
                        state = 0;
                    } else {
                        attributes.add(wrap(parseSingleToken(valueString.substring(pos, i + 1))));
                        pos = i + 1;
                        state = 0;
                    }
                    break;
                }
                case 4: {
                    if (c == '}') {
                        attributes.add(wrap(parseSingleToken(valueString.substring(pos, i + 1))));
                        pos = i + 1;
                        state = 0;
                    }
                    break;
                }
                case 5: {
                    attributes.add(wrap(parseSingleToken(valueString.substring(pos, i + 1))));
                    pos = i + 1;
                    state = 0;
                    break;
                }

            }
        }
        switch (state) {
            case 0:
            case 1:
            case 3:
            case 5: {
                if (pos != valueString.length()) {
                    attributes.add(wrap(parseSingleToken(valueString.substring(pos))));
                }
                break;
            }
            case 2:
            case 4: {
                throw new RuntimeException("Mismatched braces: " + valueString);
            }
        }
        if (attributes.size() == 1) {
            return attributes.get(0);
        }
        return new CompositeExchangeAttribute(attributes.toArray(new ExchangeAttribute[attributes.size()]));
    }

    public ExchangeAttribute parseSingleToken(final String token) {
        for (final ExchangeAttributeBuilder builder : builders) {
            ExchangeAttribute res = builder.build(token);
            if (res != null) {
                return res;
            }
        }
        if (token.startsWith("%")) {
            log.errorf("Unknown token %s", token);
        }
        return new ConstantExchangeAttribute(token);
    }

    private ExchangeAttribute wrap(ExchangeAttribute attribute) {
        ExchangeAttribute res = attribute;
        for (ExchangeAttributeWrapper w : wrappers) {
            res = w.wrap(res);
        }
        return res;
    }

}
