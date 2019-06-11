package io.quarkus.arc.processor;

import java.util.function.Predicate;
import org.jboss.jandex.DotName;

public class TruePredicate implements Predicate<DotName> {

    public static TruePredicate INSTANCE = new TruePredicate();

    @Override
    public boolean test(DotName dotName) {
        return true;
    }
}
