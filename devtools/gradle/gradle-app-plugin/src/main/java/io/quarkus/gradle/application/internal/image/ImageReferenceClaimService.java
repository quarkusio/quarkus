package io.quarkus.gradle.application.internal.image;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.gradle.api.GradleException;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;

public abstract class ImageReferenceClaimService implements BuildService<BuildServiceParameters.None> {

    private final Map<String, ImageReferenceOwner> claims = new HashMap<>();

    public synchronized void claim(ImageReferenceOwner owner, List<String> references) {
        for (String reference : new LinkedHashSet<>(references)) {
            ImageReferenceOwner previous = claims.get(reference);
            if (previous != null && !previous.sameLogicalOwner(owner)) {
                ImageReferenceOwner first = previous.compareTo(owner) <= 0 ? previous : owner;
                ImageReferenceOwner second = previous.compareTo(owner) <= 0 ? owner : previous;
                throw new GradleException("Container image reference collision for '" + reference + "' between "
                        + first.displayName() + " and " + second.displayName()
                        + ". Configure distinct image references/tags or invoke the operations separately.");
            }
        }
        for (String reference : new LinkedHashSet<>(references)) {
            claims.putIfAbsent(reference, owner);
        }
    }
}
