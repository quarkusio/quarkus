package io.quarkus.dev;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import io.quarkus.runtime.util.StringUtil;

/**
 * A set of compiler flags for javac.
 *
 * Can combine system-provided default flags with user-supplied flags and <code>-source</code>
 * and <code>-target</code> settings.
 */
public class CompilerFlags {

    private final Set<String> defaultFlags;
    private final List<String> userFlags;
    private final String sourceJavaVersion; //can be null
    private final String targetJavaVersion; //can be null

    public CompilerFlags(
            Set<String> defaultFlags,
            Collection<String> userFlags,
            String sourceJavaVersion,
            String targetJavaVersion) {

        this.defaultFlags = defaultFlags == null ? new LinkedHashSet<>() : new LinkedHashSet<>(defaultFlags);
        this.userFlags = userFlags == null ? new ArrayList<>() : new ArrayList<>(userFlags);
        this.sourceJavaVersion = sourceJavaVersion;
        this.targetJavaVersion = targetJavaVersion;
    }

    public List<String> toList() {
        List<String> flagList = new ArrayList<>();

        // The set of effective default flags is the set of default flags except the ones also
        // set by the user.  This ensures that we do not needlessly pass the default flags twice.
        Set<String> effectiveDefaultFlags = new LinkedHashSet<>(this.defaultFlags);
        effectiveDefaultFlags.removeAll(userFlags);

        flagList.addAll(effectiveDefaultFlags);

        // Add -source and -target flags.
        if (sourceJavaVersion != null) {
            flagList.add("-source");
            flagList.add(sourceJavaVersion);
        }
        if (targetJavaVersion != null) {
            flagList.add("-target");
            flagList.add(targetJavaVersion);
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
        return "CompilerFlags@{" + StringUtil.join(", ", toList().iterator()) + "}";
    }
}
