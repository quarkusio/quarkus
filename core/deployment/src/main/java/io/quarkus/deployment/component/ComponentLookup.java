package io.quarkus.deployment.component;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import io.quarkus.runtime.util.ProgrammingParadigm;
import io.quarkus.runtime.util.Reason;

/**
 * A lookup allowing to determine early whether a given component can be made available,
 * and (if relevant) why not.
 * <p>
 * Component here is meant in a very abstract way, for example "a datasource";
 * depending on context, this may translate into one or more CDI beans as well as other,
 * CDI-independent build-time or runtime objects.
 * <p>
 * Note: lookups are meant to take into account basic, critical configuration,
 * and provide information on a best-effort basis.
 * Subtle misconfiguration of a component can still result in its creation erroring out even if
 * the lookup advertises it as available.
 */
@FunctionalInterface
public interface ComponentLookup {

    /**
     * @param delegates Multiple component lookups from various sources.
     * @return A lookup that will aggregate "unavailable reasons" from all delegates,
     *         deeming a component available only if all the delegates deem it available.
     */
    static ComponentLookup of(List<ComponentLookup> delegates) {
        return new ComponentLookup() {
            @Override
            public List<Reason> unavailableReasons(String name, ProgrammingParadigm paradigm) {
                var result = new ArrayList<Reason>();
                for (var delegate : delegates) {
                    result.addAll(delegate.unavailableReasons(name, paradigm));
                }
                return result;
            }
        };
    }

    default Set<ProgrammingParadigm> availableParadigms(String name) {
        var result = EnumSet.allOf(ProgrammingParadigm.class);
        result.removeIf(paradigm -> !unavailableReasons(name, paradigm).isEmpty());
        return result;
    }

    List<Reason> unavailableReasons(String name, ProgrammingParadigm paradigm);

}
