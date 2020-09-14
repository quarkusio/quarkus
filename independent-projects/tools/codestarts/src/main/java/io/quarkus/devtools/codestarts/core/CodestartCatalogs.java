package io.quarkus.devtools.codestarts.core;

import io.quarkus.devtools.codestarts.Codestart;
import io.quarkus.devtools.codestarts.CodestartException;
import io.quarkus.devtools.codestarts.CodestartStructureException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class CodestartCatalogs {

    private CodestartCatalogs() {
    }

    public static Collection<Codestart> select(Collection<Codestart> codestarts, Set<String> selection) {
        final List<Codestart> selectedCodestarts = new ArrayList<>();
        selectedCodestarts.addAll(getBaseSelection(codestarts, selection));
        selectedCodestarts.addAll(getExtraSelection(codestarts, selection));
        return selectedCodestarts;
    }

    static Collection<Codestart> getBaseSelection(Collection<Codestart> codestarts, Set<String> selection) {
        return codestarts.stream()
                .filter(c -> c.getSpec().getType().isBase())
                .filter(c -> c.getSpec().isFallback() || c.isSelected(selection))
                .collect(Collectors.toMap(c -> c.getSpec().getType(), c -> c, (a, b) -> {
                    // When there is multiple matches for one key, one should be selected and the other a fallback.
                    if (a.getSpec().isFallback() && b.getSpec().isFallback()) {
                        throw new CodestartStructureException(
                                "Multiple fallback found for a base codestart of type: '" + a.getSpec().getType()
                                        + "' that should be unique. Only one of '" + a.getSpec().getName() + "' and '"
                                        + b.getSpec().getName() + "' should be a fallback");
                    }
                    if (!a.getSpec().isFallback() && !b.getSpec().isFallback()) {
                        throw new CodestartException(
                                "Multiple selection for base codestart of type: '" + a.getSpec().getType()
                                        + "' that should be unique. Only one of '" + a.getSpec().getName() + "' and '"
                                        + b.getSpec().getName() + "' should be selected at once.");
                    }
                    // The selected is picked.
                    return !a.getSpec().isFallback() ? a : b;
                })).values();
    }

    static Collection<Codestart> getExtraSelection(Collection<Codestart> codestarts, Set<String> selection) {
        return codestarts.stream()
                .filter(c -> !c.getSpec().getType().isBase())
                .filter(c -> c.getSpec().isPreselected() || c.isSelected(selection))
                .collect(Collectors.toList());
    }

}
