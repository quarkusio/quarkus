package io.quarkus.devtools.codestarts.core;

import io.quarkus.devtools.codestarts.Codestart;
import io.quarkus.devtools.codestarts.CodestartException;
import io.quarkus.devtools.codestarts.CodestartProjectInput;
import io.quarkus.devtools.codestarts.CodestartStructureException;
import io.quarkus.devtools.codestarts.CodestartType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class CodestartCatalogs {

    private CodestartCatalogs() {
    }

    public static Codestart findRequiredCodestart(Collection<Codestart> codestarts, CodestartType type) {
        return findCodestart(codestarts, type)
                .orElseThrow(() -> new IllegalArgumentException(type.toString().toLowerCase() + " Codestart is required"));
    }

    public static String findLanguageName(Collection<Codestart> codestarts) {
        return findRequiredCodestart(codestarts, CodestartType.LANGUAGE).getName();
    }

    public static Optional<Codestart> findCodestart(Collection<Codestart> codestarts, CodestartType type) {
        return codestarts.stream().filter(c -> c.getType() == type).findFirst();
    }

    public static Collection<Codestart> select(CodestartProjectInput projectInput, Collection<Codestart> codestarts) {
        final List<Codestart> selectedCodestarts = new ArrayList<>();
        selectedCodestarts.addAll(getBaseSelection(codestarts, projectInput.getSelection().getNames()));
        selectedCodestarts.addAll(getExtraSelection(codestarts, projectInput.getSelection().getNames()));
        return removeUnimplementedCodestarts(projectInput, selectedCodestarts);
    }

    public static Collection<Codestart> removeUnimplementedCodestarts(CodestartProjectInput projectInput,
            Collection<Codestart> codestarts) {
        final String languageName = findLanguageName(codestarts);
        return codestarts.stream().filter(c -> {
            if (!c.implementsLanguage(languageName)) {
                projectInput.log().warn(
                        c.getName() + " codestart will not be applied (doesn't implement language '" + languageName
                                + "' yet)");
                return false;
            }
            return true;
        }).collect(Collectors.toList());
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
