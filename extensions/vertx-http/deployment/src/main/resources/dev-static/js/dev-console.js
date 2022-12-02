
var devUiPanelHeight;
var devUiLocalstoragekey = "quarkus_dev_ui_state";

$('document').ready(function () {
    loadDevUiSettings();
    
    devUiFooterOpenButton.addEventListener("click", openFooter);
    devUiFooterCloseButton.addEventListener("click", closeFooter);
    
    devUiFooterResizeButton.addEventListener("mousedown", function(e){
        m_pos = e.y;
        document.addEventListener("mousemove", resize, false);   
    }, false);

    document.addEventListener("mouseup", function(){
        document.removeEventListener("mousemove", resize, false);
        saveDevUiSettings();
    }, false);
    
    $('[data-toggle="tooltip"]').tooltip();    

    // save settings on hide
    document.addEventListener('visibilitychange', function() {
        if (document.visibilityState == 'hidden') { 
            saveDevUiSettings();
        }
    });
});

function loadDevUiSettings(){
    if (devUiLocalstoragekey in localStorage) {
        var state = JSON.parse(localStorage.getItem(devUiLocalstoragekey));

        if(state.panelHeight !== null && typeof(state.panelHeight) !== 'undefined'){
            devUiPanelHeight = state.panelHeight;
            openFooter();
        }else{
            closeFooter();
            devUiPanelHeight = null;
        }
    }    
}

function saveDevUiSettings(){
    // Running state
    var state = {
        "panelHeight": devUiPanelHeight
    };

    localStorage.setItem(devUiLocalstoragekey, JSON.stringify(state));
}

function openFooter(){
    if (devUiPanelHeight === null || devUiPanelHeight === 'undefined') {
        devUiPanelHeight = "33vh";
    }
    $("#devUiFooter").css("height", devUiPanelHeight);
    $(".hideOnOpenFooter").hide();
    $(".showOnOpenFooter").show();
    
    var element = document.getElementById("devUiFooterContent");
    element.scrollIntoView({block: "end"});
    
    saveDevUiSettings();
}

function closeFooter(){
    devUiPanelHeight = null;
    $("#devUiFooter").css("height", "40px");
    $(".hideOnOpenFooter").show();
    $(".showOnOpenFooter").hide();
    saveDevUiSettings()
}

function resize(e){
    const dx = m_pos - e.y;
    m_pos = e.y;
    const panel = document.getElementById("devUiFooter");
    const content = document.getElementById("devUiFooterContent");
    
    if(panel.style.height === "unset"){
        devUiPanelHeight = null;
    }else{    
        devUiPanelHeight = parseInt(getComputedStyle(panel, '').height) + dx;
        devUiPanelHeight = "" + devUiPanelHeight;
        if(!devUiPanelHeight.endsWith("vh") && !devUiPanelHeight.endsWith("px")){
            devUiPanelHeight = devUiPanelHeight + "px";
        }
        panel.style.height = devUiPanelHeight;
        content.style.height = devUiPanelHeight - 40;
    }
}