var context = $evaluation.getContext();
var attributes = context.getAttributes();
if (attributes.containsValue('grant', 'true')) {
    $evaluation.grant();
}