package io.quarkus.spring.data.deployment.generate;

import java.util.Collection;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

final class FragmentMethodsUtil {

    private FragmentMethodsUtil() {
    }

    /**
     * Returns the simple expected implementation of a Fragment interface or throws an
     * exception indicating the problem
     */
    static DotName getImplementationDotName(DotName customInterfaceToImplement, IndexView index) {
        Collection<ClassInfo> knownImplementors = index.getAllKnownImplementors(customInterfaceToImplement);

        if (knownImplementors.size() > 1) {
            DotName previouslyFound = null;
            for (ClassInfo knownImplementor : knownImplementors) {
                if (knownImplementor.name().toString().endsWith("Impl")) { // the default suffix that Spring Data JPA looks for is 'Impl'
                    if (previouslyFound != null) { // make sure we don't have multiple implementations suffixed with 'Impl'
                        throw new IllegalArgumentException(
                                "Interface " + customInterfaceToImplement
                                        + " must contain a single implementation whose name ends with 'Impl'. Multiple implementations were found: "
                                        + previouslyFound + "," + knownImplementor);
                    }
                    previouslyFound = knownImplementor.name();
                }
            }
            return previouslyFound;
        } else if (knownImplementors.size() == 1) {
            return knownImplementors.iterator().next().name();
        } else {
            throw new IllegalArgumentException(
                    "No implementation of interface " + customInterfaceToImplement + " was found");
        }
    }
}
