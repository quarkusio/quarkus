import {faviconLogo, logo} from "../config.js"

export function setLogo(){
    $("#navbar-logo")
        .attr("src", logo);
    $("#favicon")
        .attr("href", faviconLogo);
}