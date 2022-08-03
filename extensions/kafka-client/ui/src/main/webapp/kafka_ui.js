import Navigator from './pages/navigator.js'
import {setLogo} from "./util/logo.js";

const navigator = new Navigator();
$(document).ready(
    () => {
        setLogo();
        navigator.navigateToDefaultPage();
    }
);

