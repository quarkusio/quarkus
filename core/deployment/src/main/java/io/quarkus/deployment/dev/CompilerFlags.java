package io.quarkus.deployment.dev;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A set of compiler flags for javac.
 *
 * Can combine system-provided default flags with user-supplied flags and <code>--release</code>
 * and <code>-source</code> and <code>-target</code> settings.
 */
public class CompilerFlags {

    private final Set<String> defaultFlags;
    private final List<String> userFlags;
    private final String releaseJavaVersion; //can be null
    private final String sourceJavaVersion; //can be null
    private final String targetJavaVersion; //can be null
    private final List<String> annotationProcessors; //can be null

    public CompilerFlags(
            Set<String> defaultFlags,
            Collection<String> userFlags,
            String releaseJavaVersion,
            String sourceJavaVersion,
            String targetJavaVersion,
            List<String> annotationProcessors) {

        this.defaultFlags = defaultFlags == null ? new LinkedHashSet<>() : new LinkedHashSet<>(defaultFlags);
        this.userFlags = userFlags == null ? new ArrayList<>() : new ArrayList<>(userFlags);
        this.releaseJavaVersion = releaseJavaVersion;
        this.sourceJavaVersion = sourceJavaVersion;
        this.targetJavaVersion = targetJavaVersion;
        this.annotationProcessors = annotationProcessors;
    }

    public List<String> toList() {
        List<String> flagList = new ArrayList<>();

        // The set of effective default flags is the set of default flags except the ones also
        // set by the user.  This ensures that we do not needlessly pass the default flags twice.
        Set<String> effectiveDefaultFlags = new LinkedHashSet<>(this.defaultFlags);
        effectiveDefaultFlags.removeAll(userFlags);

        flagList.addAll(effectiveDefaultFlags);

        // Prefer --release over -source and -target flags to make sure to not run into:
        // "error: option --source cannot be used together with --release"
        // This is *not* checking defaultFlags; it is not expected that defaultFlags ever contain --release etc.!
        if (releaseJavaVersion != null) {
            flagList.add("--release");
            flagList.add(releaseJavaVersion);
        } else {
            if (sourceJavaVersion != null) {
                flagList.add("-source");
                flagList.add(sourceJavaVersion);
            }
            if (targetJavaVersion != null) {
                flagList.add("-target");
                flagList.add(targetJavaVersion);
            }
        }

        if (annotationProcessors != null && !annotationProcessors.isEmpty()) {
            flagList.add("-processor");
            flagList.add(String.join(",", annotationProcessors));
        }

        flagList.addAll(userFlags);

        return flagList;
    }

    @Override
    public int hashCode() {
        return toList().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof CompilerFlags && toList().equals(((CompilerFlags) obj).toList());
    }

    @Override
    public String toString() {
        return "CompilerFlags@{" + String.join(", ", toList()) + "}";
    }
}
