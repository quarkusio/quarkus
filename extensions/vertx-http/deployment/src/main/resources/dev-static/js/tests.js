var testsPathname = window.location.pathname;
var testsFrameworkRootPath = testsPathname.substr(0, testsPathname.indexOf('/dev/'));
// Get the streaming path
var testsStreamingPath = "/dev/test";
var testsWebSocket;

var testBackendUrl = window.location.protocol + "//" + window.location.host + testsFrameworkRootPath + "/dev/io.quarkus.quarkus-vertx-http/tests/";
var testsInProgress = false;
var testsIsRunning = false;
var hasFailingTests = false;

const messageReceivedEvent = new Event('messageReceived');

$('document').ready(function () {

    testOpenSocket();
    // Make sure we stop the connection when the browser close
    window.onbeforeunload = function () {
        testCloseSocket();
    };

    addTestsKeyListeners();
    addTestsBtnListeners();
});

function testOpenSocket() {
    // Ensures only one connection is open at a time
    if (testsWebSocket !== undefined && testsWebSocket.readyState !== WebSocket.CLOSED) {
        return;
    }
    // Create a new instance of the websocket
    testsWebSocket = new WebSocket(getTestsWsUrl());

    testsWebSocket.onmessage = function (event) {
        var json = JSON.parse(event.data);
        
        testsInProgress = json.inProgress;
        
        if (json.running) {
            switchOnTesting(json);
        }else if (!json.running) {
            switchOffTesting();
        }
    };

    testsWebSocket.onclose = function () {
        switchOffTesting();
    };

}

function testCloseSocket() {
    testsWebSocket.close();
}

function getTestsWsUrl(){
    var newUri;
    if (window.location.protocol === "https:") {
        newUri = "wss:";
    } else {
        newUri = "ws:";
    }

    newUri += "//" + window.location.host + testsFrameworkRootPath + testsStreamingPath;
    return newUri;
}

function addTestsBtnListeners(){
    $(".btnPowerOnOffButton").on("click", function(e) {
        if(!testsIsRunning){
            startTests();
        }else{
            pauseTests();
        }
    });
    
    $(".btnRerunAllTests").on("click", function(e) {
        rerunAllTests();
    });
    
    $(".btnRerunFailedTests").on("click", function(e) {
        rerunFailedTests();
    });
    
    $(".btnBrokenOnlyTests").on("click", function(e) {
        toggleBrokenOnly();
    });
    
    $(".btnPrintFailuresTests").on("click", function(e) {
        printFailures();
    });
    
    $(".btnToggleTestOutput").on("click", function(e) {
        toggleTestOutput();
    });
    
    $(".btnToggleInstrumentationBasedReload").on("click", function(e) {
        toggleInstrumentationReload();
    });

    $(".btnToggleLiveReload").on("click", function(e) {
        toggleLiveReload();
    });

    $(".btnForceRestart").on("click", function(e) {
        forceRestart();
    });
    
    $(".btnDisplayTestHelp").on("click", function(e) {
        displayTestsHelp();
    });
}

function addTestsKeyListeners(){
    var r = 82; // Re-run all tests
    var f = 70; // Re-run failed tests
    var b = 66; // Toggle 'broken only' mode, where only failing tests are run
    var v = 86; // Print failures from the last test run
    var o = 79; // Toggle test output    
    var i = 73; // Toggle instrumentation based reload
    var d = 68; // Disable tests
    var h = 72; // Display this help
    var p = 80; // Pause tests
    var s = 83; // force restart
    var l = 76; // toggle live reload

    $(document).keydown(function (e) {
        if (e.target.tagName === "BODY") {
            if (e.keyCode === r){
                if(testsIsRunning){
                    rerunAllTests();
                }else{
                    startTests();
                }
            } else if (e.keyCode === f){
                rerunFailedTests();
            } else if (e.keyCode === b){
                toggleBrokenOnly();
            } else if (e.keyCode === v){
                printFailures();
            } else if (e.keyCode === o){
                toggleTestOutput();
            } else if (e.keyCode === i){
                toggleInstrumentationReload();
            } else if (e.keyCode === h){
                displayTestsHelp();
            } else if (e.keyCode === p){
                pauseTests();
            } else if (e.keyCode === s){
                forceRestart();
            } else if (e.keyCode === l){
                toggleLiveReload();
            }
        }
    });
}

function pauseTests(){
    if(testsIsRunning){
        toggleTests();
        testsIsRunning = false;
    }
}

function startTests(){
    if(!testsIsRunning){
        toggleTests();
        testsIsRunning = true;
    }
}

function toggleTests(){
    showLoading();
    $.post(testBackendUrl + "toggle"); 
}

function rerunAllTests(){
    if(!testsInProgress) {
        showLoading();
        $.post(testBackendUrl + "runall");
        testsIsRunning = true;
    }
}

function rerunFailedTests(){
    if(!testsInProgress && hasFailingTests){
        showLoading();
        $.post(testBackendUrl + "runfailed");
        testsIsRunning = true;
    }
}

function toggleBrokenOnly(){
    if(!testsInProgress) {
        $.post(testBackendUrl + "toggle-broken-only", function(data){
            if(data.brokenOnlyMode){
                switchOnBrokenOnly();
            }else{
                switchOffBrokenOnly();
            }
        });
    }
}

function printFailures(){
    if(!testsInProgress && hasFailingTests){
        $.post(testBackendUrl + "printfailures");
    }
}

function toggleTestOutput(){
    if(!testsInProgress){
        $.post(testBackendUrl + "toggle-test-output", function(data){
            if(data.isTestOutput){
                switchOnTestOutput();
            }else{
                switchOffTestOutput();
            }
        });
    }
}

function toggleInstrumentationReload(){
    if(!testsInProgress){
        $.post(testBackendUrl + "toggle-instrumentation", function(data){
            if(data.instrumentationEnabled){
                switchOnInstrumentation();
            }else{
                switchOffInstrumentation();
            }
        });
    }
}

function toggleLiveReload(){
    if(!testsInProgress){
        $.post(testBackendUrl + "toggle-live-reload", function(data){
            if(data.liveReloadEnabled){
                switchOnLiveReload();
            }else{
                switchOffLiveReload();
            }
        });
    }
}

function forceRestart(){
    if(!testsInProgress){
        $.post(testBackendUrl + "force-restart");
    }
}

function displayTestsHelp(){
    $('#testsHelpModal').modal('show');
}

function switchOffTesting(){
    hideRunningToolbar();
    hideLoading();
    $('.btnPowerOnOffButton').html("<i class='fas fa-power-off text-warning'></i> Tests not running");
    $('.btnPowerOnOffButton, .btnTestsResults i').removeClass("text-danger");
    $('.btnPowerOnOffButton, .btnTestsResults i').removeClass("text-success");
    $('.btnPowerOnOffButton, .btnTestsResults i').addClass("text-warning");
    $('.btnRerunFailedTests, .btnPrintFailuresTests').addClass("d-none");
    testsIsRunning = false;
}

function switchOnTesting(json){
    showRunningToolbar();
    hideLoading();
    
    if (json.failed === 0){
        hasFailingTests = false;
        $('.btnPowerOnOffButton').html("<i class='fas fa-power-off text-success'></i> All tests passed");
        $('.btnPowerOnOffButton, .btnTestsResults i').removeClass("text-warning");
        $('.btnPowerOnOffButton, .btnTestsResults i').removeClass("text-danger");
        $('.btnPowerOnOffButton, .btnTestsResults i').addClass("text-success");
        $('.btnRerunFailedTests, .btnPrintFailuresTests').addClass("d-none");
    } else {
        hasFailingTests = true;
        if(json.failed === 1){
            $('.btnPowerOnOffButton').html("<i class='fas fa-power-off text-danger'></i> " + json.failed + " test failed");
        }else{
            $('.btnPowerOnOffButton').html("<i class='fas fa-power-off text-danger'></i> " + json.failed + " tests failed");
        }
        $('.btnPowerOnOffButton, .btnTestsResults i').removeClass("text-warning");
        $('.btnPowerOnOffButton, .btnTestsResults i').removeClass("text-success");
        $('.btnPowerOnOffButton, .btnTestsResults i').addClass("text-danger");
        $('.btnRerunFailedTests, .btnPrintFailuresTests').removeClass("d-none");
    }

    setBrokenOnly(json.isBrokenOnly);
    setTestOutput(json.isTestOutput);
    setInstrumentationBasedReload(json.isInstrumentationBasedReload);
    setLiveReload(json.isLiveReload);
    document.dispatchEvent(messageReceivedEvent);
    testsIsRunning = true;
}

function showRunningToolbar(){
    $(".btnRerunAllTests,.btnRerunFailedTests,.btnBrokenOnlyTests,.btnPrintFailuresTests,.btnToggleTestOutput,.btnToggleInstrumentationBasedReload, .btnToggleLiveReload, .btnForceRestart, .btnTestsResults, .btnTestsGuide").each(function(){
        $(this).removeClass("d-none");
    });
}

function hideRunningToolbar(){
    $(".btnRerunAllTests,.btnRerunFailedTests,.btnBrokenOnlyTests,.btnPrintFailuresTests,.btnToggleTestOutput,.btnToggleInstrumentationBasedReload, .btnToggleLiveReload, .btnForceRestart, .btnTestsResults, .btnTestsGuide").each(function(){
        $(this).addClass("d-none");
    });
}

function showLoading(){
    $(".testsToolbar").addClass("d-none");
    $(".testsLoading").removeClass("d-none");
}

function hideLoading(){
    $(".testsToolbar").removeClass("d-none");
    $(".testsLoading").addClass("d-none");
}

function setBrokenOnly(isBrokenOnly){
    if(isBrokenOnly){
        switchOnBrokenOnly();
    }else{
        switchOffBrokenOnly();
    }
}

function switchOnBrokenOnly(){
    $('.btnBrokenOnlyTests i').removeClass("text-secondary");
    $('.btnBrokenOnlyTests i').addClass("text-danger");
    
    $('.btnBrokenOnlyTests i').removeClass("fa-heart");
    $('.btnBrokenOnlyTests i').addClass("fa-heart-broken");
}

function switchOffBrokenOnly(){
    $('.btnBrokenOnlyTests i').addClass("text-secondary");
    $('.btnBrokenOnlyTests i').removeClass("text-danger");
    
    $('.btnBrokenOnlyTests i').addClass("fa-heart");
    $('.btnBrokenOnlyTests i').removeClass("fa-heart-broken");
}

function setTestOutput(isTestOutput){
    if(isTestOutput){
        switchOnTestOutput();
    }else{
        switchOffTestOutput();
    }
}

function switchOnTestOutput(){
    $('.btnToggleTestOutput i').removeClass("text-secondary");
    $('.btnToggleTestOutput i').addClass("text-success");
}

function switchOffTestOutput(){
    $('.btnToggleTestOutput i').addClass("text-secondary");
    $('.btnToggleTestOutput i').removeClass("text-success");
}

function setInstrumentationBasedReload(isInstrumentationBasedReload){
    if(isInstrumentationBasedReload){
        switchOnInstrumentation();
    }else{
        switchOffInstrumentation();
    }
}

function switchOnInstrumentation(){
    $('.btnToggleInstrumentationBasedReload i').removeClass("text-secondary");
    $('.btnToggleInstrumentationBasedReload i').addClass("text-success");
}

function switchOffInstrumentation(){
    $('.btnToggleInstrumentationBasedReload i').addClass("text-secondary");
    $('.btnToggleInstrumentationBasedReload i').removeClass("text-success");
}

function switchOnLiveReload(){
    $('.btnToggleLiveReload i').removeClass("text-secondary");
    $('.btnToggleLiveReload i').addClass("text-success");
}

function switchOffLiveReload(){
    $('.btnToggleLiveReload i').addClass("text-secondary");
    $('.btnToggleLiveReload i').removeClass("text-success");
}

function setLiveReload(isLiveReload) {
    if(isLiveReload) {
       switchOnLiveReload();
    }else{
       switchOffLiveReload();
    }
}


