package io.quarkus.it.lra;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.ClassDescriptor;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.ClassOrdererContext;

/**
 * Custom class orderer to ensure that tests with the "ProfileTest" suffix are run last.
 * This is necessary because other tests require the LRA Dev Service coordinator to be started first,
 * and we also want to test that the Dev Service coordinator is not started when the particular
 * config properties are defined.
 */
public class LRAITTestClassOrderer implements ClassOrderer {
    @Override
    public void orderClasses(ClassOrdererContext context) {
        // Get all class descriptors from the context and create a modifiable copy
        // This resolves the "incompatible types" issue by explicitly collecting into a List<ClassDescriptor>
        List<ClassDescriptor> classDescriptors = new ArrayList<>(context.getClassDescriptors());

        // Separate "ProfileTest" classes from other classes
        List<ClassDescriptor> profileTests = classDescriptors.stream()
                .filter(descriptor -> descriptor.getTestClass().getName().endsWith("ProfileTest"))
                .collect(Collectors.toList());

        List<ClassDescriptor> otherTests = classDescriptors.stream()
                .filter(descriptor -> !descriptor.getTestClass().getName().endsWith("ProfileTest"))
                .collect(Collectors.toList());

        // Clear the original list and add the sorted classes back
        // This effectively places "otherTests" first, followed by "profileTests"
        classDescriptors.clear();
        classDescriptors.addAll(otherTests);
        classDescriptors.addAll(profileTests);
    }
}
