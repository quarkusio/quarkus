package io.quarkus.creator.outcome;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Alexey Loubyansky
 */
public class OutcomeProviderDescription<C> {

    static final int PROCESSING = 0b00001;
    static final int PROCESSED = 0b00010;

    protected final int id;
    protected final OutcomeProvider<C> provider;
    protected List<Class<?>> providedTypes = Collections.emptyList();
    private int flags;

    protected OutcomeProviderDescription(int id, OutcomeProvider<C> provider) {
        this.id = id;
        this.provider = provider;
    }

    protected void addProvidedType(Class<?> providedType) {
        if (providedTypes.isEmpty()) {
            providedTypes = new ArrayList<>(1);
        }
        providedTypes.add(providedType);
    }

    boolean isFlagOn(int flag) {
        return (flags & flag) > 0;
    }

    boolean setFlag(int flag) {
        if ((flags & flag) > 0) {
            return false;
        }
        flags ^= flag;
        return true;
    }

    void clearFlag(int flag) {
        if ((flags & flag) > 0) {
            flags ^= flag;
        }
    }
}
