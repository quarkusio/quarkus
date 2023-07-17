var identity = $evaluation.context.identity;
if (identity.hasRealmRole("admin")) {
$evaluation.grant();
}