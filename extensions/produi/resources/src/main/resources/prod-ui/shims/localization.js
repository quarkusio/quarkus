// Localization shim for Prod UI
// Dev UI uses @lit/localize for i18n. In Prod UI, we pass strings through as-is.

export function msg(str, options) {
    return str;
}

export function str(strings, ...values) {
    let result = '';
    for (let i = 0; i < strings.length; i++) {
        result += strings[i];
        if (i < values.length) result += values[i];
    }
    return result;
}

export function updateWhenLocaleChanges(component) {
    // no-op in production
}

export function dynamicMsg(namespace, key) {
    return key;
}
