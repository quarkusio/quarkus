package io.quarkus.runtime.util;

import java.util.List;
import java.util.Objects;

/**
 * An abstract concept of reason, used for example to explain why a component (datasource, persistence unit, client, ...) is
 * created.
 * <p>
 * Optionally includes causes, forming a tree that traces back
 * to one or more leaf reasons (e.g. CDI injection → persistence unit → datasource).
 */
public record Reason(String description, List<Reason> causes) {

    public static String format(List<Reason> reasons) {
        StringBuilder sb = new StringBuilder();
        format(sb, reasons, "");
        return sb.toString();
    }

    private static void format(StringBuilder sb, List<Reason> reasons, String indent) {
        for (Reason reason : reasons) {
            sb.append('\n').append(indent).append("- ").append(reason.description());
            if (reason.causes() != null && !reason.causes().isEmpty()) {
                sb.append('\n').append(indent).append("  Caused by:");
                format(sb, reason.causes(), indent + "  ");
            }
        }
    }

    public Reason(String reason) {
        this(reason, List.of());
    }

    public Reason(String reason, Reason... causes) {
        this(reason, List.of(causes));
    }

    public Reason {
        Objects.requireNonNull(description);
        Objects.requireNonNull(causes);
    }
}
