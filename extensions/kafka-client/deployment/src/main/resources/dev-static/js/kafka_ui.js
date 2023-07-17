import Navigator from './pages/navigator.js'

const navigator = new Navigator();
$(document).ready(
    () => {
        navigator.navigateToDefaultPage();
    }
);

