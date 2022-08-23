export function toggleSpinner(containerId, spinnerContainerId) {
    const spinnerId = spinnerContainerId === undefined ? "#page-load-spinner" : "#" + spinnerContainerId;
    const toggleContainerId = "#" + containerId;
    let first;
    let second;

    if ($(spinnerId).hasClass("shown")) {
        first = toggleContainerId;
        second = spinnerId;
    } else {
        second = toggleContainerId;
        first = spinnerId;
    }

    $(first)
        .removeClass("hidden")
        .addClass("shown");
    $(second)
        .addClass("hidden")
        .removeClass("shown");
}