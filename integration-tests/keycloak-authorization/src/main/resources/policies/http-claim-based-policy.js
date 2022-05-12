var context = $evaluation.getContext();
var attributes = context.getAttributes();
if (attributes.containsValue('user-name', 'alice')) {
    $evaluation.grant();
}