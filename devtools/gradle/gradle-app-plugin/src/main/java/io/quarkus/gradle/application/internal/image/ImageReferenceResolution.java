package io.quarkus.gradle.application.internal.image;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

public record ImageReferenceResolution(String primaryReference, List<String> additionalReferences) {

    public static final String SCHEMA_VERSION = "1";

    public ImageReferenceResolution {
        primaryReference = requireReference(primaryReference, "primary");
        if (additionalReferences == null) {
            throw new IllegalArgumentException("Container image additional references must not be null");
        }
        TreeSet<String> unique = new TreeSet<>();
        for (String reference : additionalReferences) {
            unique.add(requireReference(reference, "additional"));
        }
        unique.remove(primaryReference);
        additionalReferences = List.copyOf(unique);
    }

    public List<String> allReferences() {
        List<String> references = new ArrayList<>(additionalReferences.size() + 1);
        references.add(primaryReference);
        references.addAll(additionalReferences);
        return List.copyOf(references);
    }

    public static ImageReferenceResolution of(String primaryReference, Collection<String> additionalReferences) {
        return new ImageReferenceResolution(primaryReference, List.copyOf(additionalReferences));
    }

    private static String requireReference(String reference, String kind) {
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("Container image " + kind + " reference must not be empty");
        }
        return reference;
    }
}
