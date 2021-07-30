var myself = $('script[src*=logstream]');

// Get the non application root path
var frameworkRootPath = myself.attr('data-frameworkRootPath');   
if (typeof frameworkRootPath === "undefined" ) {
    var pathname = window.location.pathname;
    var frameworkRootPath = pathname.substr(0, pathname.indexOf('/dev/'));
}
// Get the streaming path
var streamingPath = myself.attr('data-streamingPath');
if (typeof streamingPath === "undefined" ) {
   var streamingPath = "/dev/logstream";
}

var zoom = 0.90;
var linespace = 1.00;
var tabspace = 1;
var increment = 0.05;

var webSocket;
var tab = "&nbsp;";
var space = "&nbsp;";

var isRunning = true;
var logScrolling = true;

var filter = "";

var localstoragekey = "quarkus_logging_manager_state";

$('document').ready(function () {
    loadSettings();
    
    openSocket();
    // Make sure we stop the connection when the browser close
    window.onbeforeunload = function () {
        closeSocket();
    };
    
    logstreamStopStartButton.addEventListener("click", stopStartEvent);
    logstreamClearLogButton.addEventListener("click", clearScreenEvent);
    logstreamZoomOutButton.addEventListener("click", zoomOutEvent);
    logstreamZoomInButton.addEventListener("click", zoomInEvent);
    logstreamFollowLogButton.addEventListener("click", followLogEvent);
    logstreamFilterModalInputButton.addEventListener("click", applyFilter);
    
    addControlCListener();
    addEnterListener();
    addScrollListener();
    addLineSpaceListener();
    addTabSizeListener();
    
    $('[data-toggle="tooltip"]').tooltip();    

    logstreamFilterModalInput.addEventListener("keyup", function(event) {
        if (event.keyCode === 13) {
            event.preventDefault();
            logstreamFilterModalInputButton.click();
        }
    });
    
    $('#logstreamFilterModal').on('shown.bs.modal', function () {
        $('#logstreamFilterModalInput').trigger('focus');
    });
    
    // save settings on hide
    document.addEventListener('visibilitychange', function() {
        if (document.visibilityState == 'hidden') { 
            saveSettings();
        }
    });
    
});

function loadSettings(){
    if (localstoragekey in localStorage) {
        var state = JSON.parse(localStorage.getItem(localstoragekey));

        zoom = state.zoom;
        applyZoom();

        linespace = state.linespace;
        applyLineSpacing();

        tabspace = state.tabspace;
        applyTabSpacing();

        logScrolling = state.logScrolling;
        applyFollowLog();

        $("#logstreamFilterModalInput").val(state.filter);
        applyFilter();
        
        $('#logstreamColumnsModalLevelIconSwitch').prop('checked', state.levelIconSwitch);
        $('#logstreamColumnsModalSequenceNumberSwitch').prop('checked', state.sequenceNumberSwitch);
        $('#logstreamColumnsModalDateSwitch').prop('checked', state.dateSwitch);
        $('#logstreamColumnsModalTimeSwitch').prop('checked', state.timeSwitch);
        $('#logstreamColumnsModalLevelSwitch').prop('checked', state.levelSwitch);
        $('#logstreamColumnsModalSourceClassFullAbbreviatedSwitch').prop('checked', state.sourceClassFullAbbreviatedSwitch);
        $('#logstreamColumnsModalSourceClassFullSwitch').prop('checked', state.sourceClassFullSwitch);
        $('#logstreamColumnsModalSourceClassSwitch').prop('checked', state.sourceClassSwitch);
        $('#logstreamColumnsModalSourceMethodNameSwitch').prop('checked', state.sourceMethodNameSwitch);
        $('#logstreamColumnsModalThreadIdSwitch').prop('checked', state.threadIdSwitch);
        $('#logstreamColumnsModalThreadNameSwitch').prop('checked', state.threadNameSwitch);
        $('#logstreamColumnsModalMessageSwitch').prop('checked', state.messageSwitch);
        $('#logstreamColumnsModalHostNameSwitch').prop("checked", state.hostNameSwitch);
        $('#logstreamColumnsModalLoggerNameAbbreviatedSwitch').prop("checked", state.loggerNameAbbreviatedSwitch);
        $('#logstreamColumnsModalLoggerNameSwitch').prop("checked", state.loggerNameSwitch);
        $('#logstreamColumnsModalLoggerClassNameSwitch').prop("checked", state.loggerClassNameSwitch);
        $('#logstreamColumnsModalSourceFileNameSwitch').prop("checked", state.sourceFileNameSwitch);
        $('#logstreamColumnsModalSourceLineNumberSwitch').prop("checked", state.sourceLineNumberSwitch);
        $('#logstreamColumnsModalProcessIdSwitch').prop("checked", state.processIdSwitch);
        $('#logstreamColumnsModalProcessNameSwitch').prop("checked", state.processNameSwitch);
    }    
}

function saveSettings(){
    // Running state
    var state = {
        "zoom": zoom,
        "linespace": linespace,
        "tabspace": tabspace,
        "logScrolling": logScrolling,
        "filter": filter,
        "levelIconSwitch": $('#logstreamColumnsModalLevelIconSwitch').is(":checked"),
        "sequenceNumberSwitch": $('#logstreamColumnsModalSequenceNumberSwitch').is(":checked"),
        "dateSwitch": $('#logstreamColumnsModalDateSwitch').is(":checked"),
        "timeSwitch": $('#logstreamColumnsModalTimeSwitch').is(":checked"),
        "levelSwitch": $('#logstreamColumnsModalLevelSwitch').is(":checked"),
        "sourceClassFullAbbreviatedSwitch": $('#logstreamColumnsModalSourceClassFullAbbreviatedSwitch').is(":checked"),
        "sourceClassFullSwitch": $('#logstreamColumnsModalSourceClassFullSwitch').is(":checked"),
        "sourceClassSwitch": $('#logstreamColumnsModalSourceClassSwitch').is(":checked"),
        "sourceMethodNameSwitch": $('#logstreamColumnsModalSourceMethodNameSwitch').is(":checked"),
        "threadIdSwitch": $('#logstreamColumnsModalThreadIdSwitch').is(":checked"),
        "threadNameSwitch": $('#logstreamColumnsModalThreadNameSwitch').is(":checked"),
        "messageSwitch": $('#logstreamColumnsModalMessageSwitch').is(":checked"),
        "hostNameSwitch": $('#logstreamColumnsModalHostNameSwitch').is(":checked"),
        "loggerNameAbbreviatedSwitch": $('#logstreamColumnsModalLoggerNameAbbreviatedSwitch').is(":checked"),
        "loggerNameSwitch": $('#logstreamColumnsModalLoggerNameSwitch').is(":checked"),
        "loggerClassNameSwitch": $('#logstreamColumnsModalLoggerClassNameSwitch').is(":checked"),
        "sourceFileNameSwitch": $('#logstreamColumnsModalSourceFileNameSwitch').is(":checked"),
        "sourceLineNumberSwitch": $('#logstreamColumnsModalSourceLineNumberSwitch').is(":checked"),
        "processIdSwitch": $('#logstreamColumnsModalProcessIdSwitch').is(":checked"),
        "processNameSwitch": $('#logstreamColumnsModalProcessNameSwitch').is(":checked")
    };

    localStorage.setItem(localstoragekey, JSON.stringify(state));
}

function addControlCListener(){
    // Add listener to stop
    var ctrlDown = false,
            ctrlKey = 17,
            cmdKey = 91,
            cKey = 67;

    $(document).keydown(function (e) {
        if (e.keyCode === ctrlKey || e.keyCode === cmdKey)
            ctrlDown = true;
    }).keyup(function (e) {
        if (e.keyCode === ctrlKey || e.keyCode === cmdKey)
            ctrlDown = false;
    });

    $(document).keydown(function (e) {
        if (e.target.tagName === "BODY") {
            if (ctrlDown && (e.keyCode === cKey))stopLog();
        }
    });
}

function addScrollListener(){
    $(document).on('mousewheel DOMMouseScroll', function(event) {
        if (event.shiftKey) {
            if( event.originalEvent.detail > 0 || event.originalEvent.wheelDelta < 0 ) {
                zoomOutEvent();
            } else {
                zoomInEvent();
            }
            return false;
        }
    });
}

function addLineSpaceListener(){
    $(document).keydown(function (e) {
        if (e.target.tagName === "BODY") {
            if (e.shiftKey && e.keyCode === 38) {
                lineSpaceIncreaseEvent();
            }else if (e.shiftKey && e.keyCode === 40) {
                lineSpaceDecreaseEvent();
            }
        }
    });
}

function addTabSizeListener(){
    $(document).keydown(function (e) {
        if (e.target.tagName === "BODY") {
            if (e.shiftKey && e.keyCode === 39) {
                tabSpaceIncreaseEvent();
            }else if (e.shiftKey && e.keyCode === 37) {
                tabSpaceDecreaseEvent();
            }
        }
    });
}

function addEnterListener(){
    $(document).keydown(function (e) {
        if (e.target.tagName === "BODY") {
            if (e.keyCode === 13 && !$('#logstreamFilterModal').hasClass('show')){
                writeResponse("</br>");
                var element = document.getElementById("logstreamLogTerminal");
                element.scrollIntoView({block: "end"});
            } 
        }
    });
}

function stopStartEvent() {
    if (isRunning) {
        stopLog();
    } else {
        startLog();
    }
}

function stopLog() {
    webSocket.send("stop");
    writeResponse("<hr class='logstreamStopLogHr'/>");

    logstreamStopStartButton.innerHTML = "<i class='fas fa-play'></i>";
    $("#logstreamFollowLogButton").hide();
    isRunning = false;
}

function startLog() {
    webSocket.send("start");

    logstreamStopStartButton.innerHTML = "<i class='fas fa-stop'></i>";
    $("#logstreamFollowLogButton").show();
    isRunning = true;
}

function clearScreenEvent() {
    logstreamLogTerminalText.innerHTML = "";
}

function applyLineSpacing(){
    $('#logstreamLogTerminal').css("line-height", linespace);
}

function applyTabSpacing(){
    if(tabspace === null || isNaN(tabspace))tabspace = 1;
    tab = "";
    for (i = 0; i < tabspace; i++) {
        tab = tab + space;
    };
}

function lineSpaceDecreaseEvent() {
    linespace = parseFloat(linespace) - parseFloat(increment);
    linespace = parseFloat(linespace).toFixed(2);
    showInfoMessage("<i class='fas fa-text-height'></i>" + space  + linespace);
    applyLineSpacing();
}

function lineSpaceIncreaseEvent() {
    linespace = parseFloat(linespace) + parseFloat(increment);
    linespace = parseFloat(linespace).toFixed(2);
    showInfoMessage("<i class='fas fa-text-height'></i>" + space  + linespace);
    applyLineSpacing();
}

function tabSpaceDecreaseEvent() {
    if(tabspace>1){
        tabspace = tabspace - 1;
        showInfoMessage("<i class='fas fa-text-width'></i>" + space  + tabspace);
        applyTabSpacing();
    }
}

function tabSpaceIncreaseEvent() {
    tabspace = tabspace + 1;
    showInfoMessage("<i class='fas fa-text-width'></i>" + space  + tabspace);
    applyTabSpacing();
}

function applyZoom(){
    $('#logstreamLogTerminalText').css("font-size", zoom + "em");
}

function zoomOutEvent() {
    zoom = parseFloat(zoom) - parseFloat(increment);
    zoom = parseFloat(zoom).toFixed(2);
    showInfoMessage("<i class='fas fa-search-minus'></i>" + space  + zoom);
    applyZoom();
}

function zoomInEvent() {
    zoom = parseFloat(zoom) + parseFloat(increment);
    zoom = parseFloat(zoom).toFixed(2);
    showInfoMessage("<i class='fas fa-search-plus'></i>" + space  + zoom);
    applyZoom();
}

function showInfoMessage(msg){
    $('#logstreamInformationSection').empty().show().html(msg).delay(3000).fadeOut(300);
}

function followLogEvent() {
    logScrolling = !logScrolling;
    applyFollowLog();
}

function applyFollowLog(){
    if (logScrolling) {
        $("#logstreamFollowLogButtonIcon").addClass("text-success");
        $("#logstreamFollowLogButtonIcon").addClass("fa-spin");
        showInfoMessage("<i class='fas fa-check-circle'></i>" + space  + "Autoscroll ON");
    }else{
        $("#logstreamFollowLogButtonIcon").removeClass("text-success");
        $("#logstreamFollowLogButtonIcon").removeClass("fa-spin");
        showInfoMessage("<i class='fas fa-times-circle'></i>" + space  + "Autoscroll OFF");
    }
}

function scrollToTop() {
    logScrolling = false;
}

function scrollToBottom() {
    logScrolling = true;
}

function applyFilter(){
    filter = $("#logstreamFilterModalInput").val();
    if(filter===""){
        clearFilter();
    }else{
        logstreamCurrentFilter.innerHTML = "<span style='border-bottom: 1px dotted;'>" + filter + " <i class='fas fa-times-circle' onclick='clearFilter();'></i></span>";
        
        var currentlines = $("#logstreamLogTerminalText").html().split('<!-- logline -->');
        
        var filteredHtml = "";
        var i;
        for (i = 0; i < currentlines.length; i++) {
            var htmlline = currentlines[i];
            filteredHtml = filteredHtml + getLogLine(htmlline) + "<!-- logline -->";
        } 
        
        logstreamLogTerminalText.innerHTML = "";
        writeResponse(filteredHtml);
    }
    $('#logstreamFilterModal').modal('hide');
}

function getLogLine(htmlline){
    if(filter===""){
        return htmlline;
    }else{
        
        var textline = $(htmlline).text();
        if(textline.includes(filter)){
            return htmlline;
        }else{
            return htmlline.replace('<span>', '<span class="logstreamFilteredOut">');
        }
    }
}

function clearFilter(){
    filter = "";
    $("#logstreamFilterModalInput").val("");
    logstreamCurrentFilter.innerHTML = "";
    
    var currentlines = $("#logstreamLogTerminalText").html().split('<!-- logline -->');
        
    var filteredHtml = "";
    var i;
    for (i = 0; i < currentlines.length; i++) {
        var htmlline = currentlines[i].replace('<span class="logstreamFilteredOut">', '<span>');
        filteredHtml = filteredHtml + htmlline + "<!-- logline -->";
    } 

    logstreamLogTerminalText.innerHTML = "";
    writeResponse(filteredHtml);
}

function getLevelIcon(level) {
    if($('#logstreamColumnsModalLevelIconSwitch').is(":checked")){
        level = level.toUpperCase();
        if (level === "WARNING" || level === "WARN")
            return "<i class='levelicon text-warning fas fa-exclamation-circle'></i>" + tab;
        if (level === "SEVERE" || level === "ERROR")
            return "<i class='levelicon text-danger fas fa-radiation'></i>" + tab;
        if (level === "INFO")
            return "<i class='levelicon text-primary fas fa-info-circle'></i>" + tab;
        if (level === "DEBUG")
            return "<i class='levelicon text-secondary fas fa-bug'></i>" + tab;

        return "<i class='levelicon fas fa-circle'></i>" + tab;
    }
    return "";
}

function getHostName(hostName) {
    if($('#logstreamColumnsModalHostNameSwitch').is(":checked")){
        return hostName + tab;
    }
    return "";
}

function getSequenceNumber(sequenceNumber){
    if($('#logstreamColumnsModalSequenceNumberSwitch').is(":checked")){
        return "<span class='badge badge-info'>" + sequenceNumber + "</span>" + tab;   
    }
    return "";
}

function getDateString(timestamp){
    if($('#logstreamColumnsModalDateSwitch').is(":checked")){
      return timestamp.slice(0, 10) + space;
    }
    return "";
}

function getTimeString(timestamp){
    if($('#logstreamColumnsModalTimeSwitch').is(":checked")){
        return timestamp.slice(11, 23).replace(".", ",") + tab;
    }
    return "";
}

function getLevelText(level) {
    if($('#logstreamColumnsModalLevelSwitch').is(":checked")){
        level = level.toUpperCase();
        if (level === "WARNING" || level === "WARN")
            return "<span class='text-warning'>WARN" + space + "</span>" + tab;
        if (level === "SEVERE" || level === "ERROR")
            return "<span class='text-danger'>ERROR</span>" + tab;
        if (level === "INFO")
            return "<span class='text-primary'>INFO" + space + "</span>" + tab;
        if (level === "DEBUG")
            return "<span class='text-secondary'>DEBUG</span>" + tab;

        return level + tab;
    }
    return "";
}

function getLoggerNameAbbreviated(loggerNameAbbreviated){
    if($('#logstreamColumnsModalLoggerNameAbbreviatedSwitch').is(":checked")){
        return "<span class='text-primary'>[" + loggerNameAbbreviated + "]</span>" + tab;
    }
    return "";
}

function getLoggerName(loggerName){
    if($('#logstreamColumnsModalLoggerNameSwitch').is(":checked")){
        return "<span class='text-primary'>[" + loggerName + "]</span>" + tab;
    }
    return "";
}

function getLoggerClassName(loggerClassName){
    if($('#logstreamColumnsModalLoggerClassNameSwitch').is(":checked")){
        return "<span class='text-info'>[" + loggerClassName + "]</span>" + tab;
    }
    return "";
}

function getClassFullAbbreviatedName(sourceClassNameFull, lineNumber, sourceClassNameFullShort) {
    if($('#logstreamColumnsModalSourceClassFullAbbreviatedSwitch').is(":checked")){
        if (isClickableClassName(sourceClassNameFull)) {
            return makeClickableClassNameLink(sourceClassNameFull, lineNumber, sourceClassNameFullShort);
        }
        return "<span class='text-secondary'>[" + sourceClassNameFullShort + "]</span>" + tab;
    }
    return "";
}

function getFullClassName(sourceClassNameFull, lineNumber) {
    if($('#logstreamColumnsModalSourceClassFullSwitch').is(":checked")){
        if (isClickableClassName(sourceClassNameFull)) {
            return makeClickableClassNameLink(sourceClassNameFull, lineNumber, sourceClassNameFull);
        }
        return "<span class='text-secondary'>[" + sourceClassNameFull + "]</span>" + tab;
    }
    return "";
}

function getClassName(sourceClassNameFull, lineNumber, className) {
    if($('#logstreamColumnsModalSourceClassSwitch').is(":checked")){
        if (isClickableClassName(sourceClassNameFull)) {
            return makeClickableClassNameLink(sourceClassNameFull, lineNumber, className);
        }
        return "<span class='text-secondary'>[" + className + "]</span>" + tab;
    }
    return "";
}

function isClickableClassName(className){
    if (className !== undefined && appClassLang(className) && ideKnown()) {
        return true;
    }
    return false;
}

function makeClickableClassNameLink(className, lineNumber, display){
    return "<a class='text-secondary clickable-app-class' onclick='openInIDE(\"" + className + "\",\"" + lineNumber + "\");'>[" + display + "]</a>" + tab;
}

function getMethodName(methodName) {
    if($('#logstreamColumnsModalSourceMethodNameSwitch').is(":checked")){
        return methodName + tab;
    }
    return "";
}

function getFileName(fileName){
    if($('#logstreamColumnsModalSourceFileNameSwitch').is(":checked")){
        return "<span class='text-monospace'>" + fileName + "</span>" + space;
    }
    return "";
}

function getLineNumber(lineNumber){
    if($('#logstreamColumnsModalSourceLineNumberSwitch').is(":checked")){
        return "<span class='text-monospace'>(line: " + lineNumber + ")</span>" + tab;
    }
    return "";
}

function getProcessId(processName, processId) {
    if($('#logstreamColumnsModalProcessIdSwitch').is(":checked")){
        return "<span class='text-info' data-toggle='tooltip' data-placement='top' title='Process Name: " + processName + "'>(" + processId + ")</span>" + tab;
    }
    return "";
}

function getProcessName(processName, processId) {
    if($('#logstreamColumnsModalProcessNameSwitch').is(":checked")){
        return "<span class='text-info' data-toggle='tooltip' data-placement='top' title='Process Id: " + processId + "'>(" + processName + ")</span>" + tab;
    }
    return "";
}

function getThreadId(threadName, threadId) {
    if($('#logstreamColumnsModalThreadIdSwitch').is(":checked")){
        return "<span class='text-success' data-toggle='tooltip' data-placement='top' title='Thread Name: " + threadName + "'>(" + threadId + ")</span>" + tab;
    }
    return "";
}

function getThreadName(threadName, threadId) {
    if($('#logstreamColumnsModalThreadNameSwitch').is(":checked")){
        return "<span class='text-success' data-toggle='tooltip' data-placement='top' title='Thread Id: " + threadId + "'>(" + threadName + ")</span>" + tab;
    }
    return "";
}

function getLogMessage(message){
    if($('#logstreamColumnsModalMessageSwitch').is(":checked")){
        // Make links clickable
        if(message.includes("http://")){
            message = makeLink(message, "http://");
        }
        if(message.includes("https://")){
            message = makeLink(message, "https://");
        }
        // Make sure multi line is supported
        if(message.includes('\n')){
            var htmlifiedLines = [];
            var lines = message.split('\n');
            for (var i = 0; i < lines.length; i++) {
                var line = lines[i];
                line = line.replace(/ /g, '\u00a0');
                if(i === lines.length-1){
                    htmlifiedLines.push(line);
                }else{
                    htmlifiedLines.push(line + '<br/>');
                }
            }
            message = htmlifiedLines.join('');
        }   
        return message;
    }
    return "";
}

function makeLink(message, protocol){
    var url = message.substring(message.indexOf(protocol));
    if(url.includes(" ")){
        url = url.substr(0,url.indexOf(' '));
    }
    var link = "<a href='" + url + "' class='text-primary' target='_blank'>" + url + "</a>";

    return message.replace(url, link);    
}

function enhanceStacktrace(stacktrace) {
    var enhanceStacktrace = [];
    var lines = stacktrace.split('\n');
    for (var i = 0; i < lines.length; i++) {
        var line = lines[i].trim();
        if (line) {
            var startWithAt = line.startsWith("at ");
            if (!startWithAt) {
                var parts = line.split(":");
                line = "<b>" + parts[0] + ":</b>" + parts[1];
            } else {
                if(!line.includes(".zig")){
                    var parts = line.split(" ");
                    // Make it clickable
                    var classMethodFileNumber = parts[1];
                    var classMethodFileNumberSplit = classMethodFileNumber.split("(");
                    var classMethod = classMethodFileNumberSplit[0];
                    var fileNumber = classMethodFileNumberSplit[1];
                    givenClassName = classMethod.substring(0, classMethod.lastIndexOf('.'));
                    if(isClickableClassName(givenClassName)){
                        lineNumber = fileNumber.substring(fileNumber.lastIndexOf(':') + 1, fileNumber.lastIndexOf(')'));
                        line = "<a class='text-wrap text-danger clickable-app-class' onclick='openInIDE(\"" + givenClassName + "\",\"" + lineNumber + "\");'><b>" + line + "</b></a>";
                    }
                }
                line = space + space + space + space + space + space + line;
            }
        }
        enhanceStacktrace.push(line + '<br/>');
    }
    var newStacktrace = enhanceStacktrace.join('');
    return "<span class=\"text-wrap text-danger\">" + newStacktrace + "</span>";
}

function writeResponse(text) {
    var logfile = $('#logstreamLogTerminalText');
    logfile.append(text);
    if (logScrolling) {
        var element = document.getElementById("logstreamLogTerminal");
        element.scrollIntoView({block: "end"});
    }
    // TODO: Trim the top if it gets too big ?
}

function openSocket() {
    // Ensures only one connection is open at a time
    if (webSocket !== undefined && webSocket.readyState !== WebSocket.CLOSED) {
        writeResponse("Already connected...");
        return;
    }
    // Create a new instance of the websocket
    var new_uri;
    if (window.location.protocol === "https:") {
        new_uri = "wss:";
    } else {
        new_uri = "ws:";
    }
    
    new_uri += "//" + window.location.host + frameworkRootPath + streamingPath;
    webSocket = new WebSocket(new_uri);

    webSocket.onmessage = function (event) {
        var json = JSON.parse(event.data);
        
        if(json.type === "logLine"){
            messageLog(json);
        }else if(json.type === "init"){
            populateLoggerLevelModal(json.loggers,json.levels);
        }
    };

    webSocket.onclose = function () {
        saveSettings();
        if (isRunning) {
            stopLog();
        }
        writeResponse("Connection closed<br/>");
    };

    function messageLog(json) {
        
        var timestamp = new Date(json.timestamp);
        var level = json.level;
        var isoDateTime = new Date(timestamp.getTime() - (timestamp.getTimezoneOffset() * 60000)).toISOString();
        
        var htmlLine = "<span>" 
                + getLevelIcon(level)
                + getSequenceNumber(json.sequenceNumber)
                + getHostName(json.hostName)
                + getDateString(isoDateTime)
                + getTimeString(isoDateTime)
                + getLevelText(level)
                + getLoggerNameAbbreviated(json.loggerNameShort)
                + getLoggerName(json.loggerName)
                + getLoggerClassName(json.loggerClassName)        
                + getClassFullAbbreviatedName(json.sourceClassNameFull, json.sourceLineNumber,json.sourceClassNameFullShort)
                + getFullClassName(json.sourceClassNameFull,json.sourceLineNumber)
                + getClassName(json.sourceClassNameFull,json.sourceLineNumber,json.sourceClassName)
                + getMethodName(json.sourceMethodName)
                + getFileName(json.sourceFileName)
                + getLineNumber(json.sourceLineNumber)
                + getProcessId(json.processName, json.processId)
                + getProcessName(json.processName, json.ProcessId)
                + getThreadId(json.threadName, json.threadId)
                + getThreadName(json.threadName, json.threadId)
                + getLogMessage(json.formattedMessage) + "<br/>";
                
        if (json.stacktrace) {
            for (var i in json.stacktrace) {
                var stacktrace = enhanceStacktrace(json.stacktrace[i]);
                htmlLine = htmlLine + stacktrace;
            }
        }
        
        htmlLine = htmlLine + "</span><!-- logline -->";
        
        if(filter!=""){
            writeResponse(getLogLine(htmlLine));
        }else{
            writeResponse(htmlLine);
        }   
    }
}

function closeSocket() {
    webSocket.close();
}

function populateLoggerLevelModal(loggerNamesArray, levelNamesArray){
    var tbodyLevels = $('#logstreamLogLevelsModalTableBody');
    
    // Populate the dropdown
    for (var i = 0; i < loggerNamesArray.length; i++) {
        var row = "<tr><td id='" + createLevelRowId(loggerNamesArray[i].name) + "' class=" + getTextClass(loggerNamesArray[i].effectiveLevel) + ">" + loggerNamesArray[i].name + "</td><td>" + createDropdown(loggerNamesArray[i].name, loggerNamesArray[i].effectiveLevel,levelNamesArray) + "</td></tr>";
        tbodyLevels.append(row);
    }
    
    $('select').on('change', function() {
        changeLogLevel(this.value, $(this).find('option:selected').text());
    });
    
    populated = true;
}

function createLevelRowId(logger){
    var name = logger + "_row";
    return name.replaceAll(".", "_");
}

function getTextClass(level){
    level = level.toUpperCase();
    if (level === "WARNING" || level === "WARN")
        return "text-warning";
    if (level === "SEVERE" || level === "ERROR")
        return "text-danger";
    if (level === "INFO")
        return "text-primary";
    if (level === "DEBUG")
        return "text-secondary";

    return "";
}

function createDropdown(name, level, levelNamesArray){
    
    var dd = "<select class='custom-select custom-select-sm'>";
    // Populate the dropdown
    for (var i = 0; i < levelNamesArray.length; i++) {
        var selected = "";
        if(level === levelNamesArray[i]){
            selected = "selected";
        }
        dd = dd + "<option " + selected + " value='" + name + "'>" + levelNamesArray[i] +"</option>";
    }
    dd = dd + "</select>";
    
    return dd;
}

function changeLogLevel(val,text){
    webSocket.send("update|" + val + "|" + text);
    // Also change the style of the row
    var id = createLevelRowId(val);
    $('#' + id).removeClass();
    $('#' + id).addClass(getTextClass(text));    
}