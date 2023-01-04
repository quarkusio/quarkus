import {api} from "../config.js"

export function doPost(data, successCallback, errorCallback) {
    $.ajax({
        url: api(),
        type: 'POST',
        data: JSON.stringify(data),
        contentType: "application/json; charset=utf-8",
        dataType: 'json',
        context: this,
        success: (data) => successCallback(data),
        error: (data, errorType, errorObj) => errorCallback(data, errorType, errorObj)
    });
}

export function errorPopUp() {
    let message = "";
    for (let i = 0; i < arguments.length; i++) {
        message += arguments[i] + " ";
    }
    alert(message);
}
