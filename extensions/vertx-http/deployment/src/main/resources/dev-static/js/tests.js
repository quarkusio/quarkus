var testsMyself = $('script[src*=tests]');

// Get the non application root path
var testsFrameworkRootPath = myself.attr('data-frameworkRootPath');
if (typeof frameworkRootPath === "undefined" ) {
    var pathname = window.location.pathname;
    var frameworkRootPath = pathname.substr(0, pathname.indexOf('/dev/'));
}
// Get the streaming path
var testsStreamingPath = myself.attr('data-streamingPath');
if (typeof testsStreamingPath === "undefined" ) {
   var testsStreamingPath = "/dev/test";
}

var zoom = 0.90;
var panelHeight;
var linespace = 1.00;
var tabspace = 1;
var increment = 0.05;

var testsWebSocket;
var tab = "&nbsp;";
var space = "&nbsp;";

var isRunning = true;
var logScrolling = true;

var filter = "";

$('document').ready(function () {
    
    testOpenSocket();
    // Make sure we stop the connection when the browser close
    window.onbeforeunload = function () {
        testCloseSocket();
    };

    $("#quarkus-test-result-button-pause").click(function() {
        var new_uri =window.location.protocol + "//" + window.location.host + frameworkRootPath + "/dev/io.quarkus.quarkus-vertx-http/tests/toggle";
           $.post( new_uri );
       });

    $("#quarkus-test-result-button-run-all").click(function() {
        var new_uri =window.location.protocol + "//" + window.location.host + frameworkRootPath + "/dev/io.quarkus.quarkus-vertx-http/tests/runall";
           $.post( new_uri );
       });
});

function testOpenSocket() {
    // Ensures only one connection is open at a time
    if (testsWebSocket !== undefined && testsWebSocket.readyState !== WebSocket.CLOSED) {
        return;
    }
    // Create a new instance of the websocket
    var new_uri;
    if (window.location.protocol === "https:") {
        new_uri = "wss:";
    } else {
        new_uri = "ws:";
    }

    new_uri += "//" + window.location.host + frameworkRootPath + testsStreamingPath;
    testsWebSocket = new WebSocket(new_uri);

    testsWebSocket.onmessage = function (event) {
        var json = JSON.parse(event.data);
        if (json.running == false) {
            $("#quarkus-test-result-button").removeClass("btn-success");
            $("#quarkus-test-result-button").removeClass("btn-danger");
            $("#quarkus-test-result-button").addClass("btn-warning");
            $("#quarkus-test-result-button-caption").text("Tests not running");
            $("#quarkus-test-result-button-pause").text("Start Tests");
        } else if(json.failed == 0){
            $("#quarkus-test-result-button").removeClass("btn-warning");
            $("#quarkus-test-result-button").removeClass("btn-danger");
            $("#quarkus-test-result-button").addClass("btn-success");
            $("#quarkus-test-result-button-caption").text("All tests passed");
            $("#quarkus-test-result-button-pause").text("Pause Tests");
        } else {
            $("#quarkus-test-result-button").removeClass("btn-success");
            $("#quarkus-test-result-button").removeClass("btn-warning");
            $("#quarkus-test-result-button").addClass("btn-danger");
            $("#quarkus-test-result-button-caption").text(json.failed + " tests failed");
            $("#quarkus-test-result-button-pause").text("Pause Tests");
        }
        if (json.inProgress) {
            $("#quarkus-test-result-button-loading").css("display", "inline-flex");
        } else {
            $("#quarkus-test-result-button-loading").css("display", "none");
        }
        $("#quarkus-test-result-button").parent().css("display", "inline-flex");
         console.log(json);
    };

    testsWebSocket.onclose = function () {

    };

}

function testCloseSocket() {
    testsWebSocket.close();
}
