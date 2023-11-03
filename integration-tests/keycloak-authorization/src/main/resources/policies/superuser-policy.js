var identity = $evaluation.context.identity;
if (identity.hasRealmRole("superuser")) {
$evaluation.grant();
}