export function createTableItem(text) {
    return $("<td/>", {
        text: text
    });
}

export function createIcon(iconClass) {
    return $("<i/>")
        .addClass("bi")
        .addClass(iconClass);
}

export function showItem(selector){
    selector.addClass("shown")
        .removeClass("hidden");
}

export function hideItem(selector){
    selector.addClass("hidden")
        .removeClass("shown");
}

export function toggleItem(selector) {
    if (selector.hasClass("shown")) {
        hideItem(selector);
    } else {
        showItem(selector);
    }
}