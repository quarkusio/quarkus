var identity = $evaluation.context.identity;
if (identity.hasRealmRole("confidential")) {
    $evaluation.grant();
}