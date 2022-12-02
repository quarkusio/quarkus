var context = $evaluation.getContext();
print(context.getAttributes().toMap());
var attributes = context.getAttributes();
if (attributes.containsValue('from-body', 'grant')) {
    $evaluation.grant();
}