jQuery(function(){
/*
 * SEARCH
 */
var inputs = {};
var tables = document.querySelectorAll("table.configuration-reference");
var typingTimer;

if(tables){
    for (var table of tables) {
        var caption = table.previousElementSibling;
        if (table.classList.contains('searchable')) { // activate search engine only when needed
            var input = caption.firstElementChild.lastElementChild;
            input.addEventListener("keyup", initiateSearch);
            input.addEventListener("input", initiateSearch);
            if (input.attributes.disabled) input.attributes.removeNamedItem('disabled');
            inputs[input.id] = {"table": table};
        }

        const collapsibleRows = table.querySelectorAll('tr.row-collapsible');
        if (collapsibleRows) {
            for (let row of collapsibleRows) {
                const td = row.firstElementChild;
                const decoration = td.firstElementChild.lastElementChild.firstElementChild;
                const iconDecoration = decoration.children.item(0);
                const collapsibleSpan = decoration.children.item(1);
                const descDiv = td.firstElementChild.children.item(1);
                const collapsibleHandler = makeCollapsibleHandler(descDiv, td, row, collapsibleSpan, iconDecoration);
                row.addEventListener('click', collapsibleHandler);
            }
        }

        // render hidden rows asynchronously
        setTimeout(() => renderHiddenRows());
    }
}

function renderHiddenRows() {
    // some rows are initially hidden so that user can hit the ground running
    // we render them at this very moment, but when user can already use search function
    const hiddenRows = document.querySelectorAll('table.configuration-reference-all-rows.tableblock > tbody > tr.row-hidden');
    if (hiddenRows) {
        for (row of hiddenRows) {
            row.classList.remove('row-hidden');
        }
    }
}

function initiateSearch(event){
    // only start searching after the user stopped typing for 300ms, since we can't abort
    // running tasks, we don't want to search three times for "foo" (one letter at a time)
    if(typingTimer)
        clearTimeout(typingTimer);
    typingTimer = setTimeout(() => search(event.target), 300)
}

function highlight(element, text){
    var iter = document.createNodeIterator(element, NodeFilter.SHOW_TEXT, null);

    while (n = iter.nextNode()){
        var parent = n.parentNode;
        var elementText = n.nodeValue;
        if(elementText == undefined)
            continue;
        var elementTextLC = elementText.toLowerCase();
        var index = elementTextLC.indexOf(text);
        if(index != -1
           && acceptTextForSearch(n)){
            var start = 0;
            var fragment = document.createDocumentFragment()
            // we use the DOM here to avoid &lt; and such being parsed as elements by jQuery when replacing content
            do{
                // text before
                fragment.appendChild(document.createTextNode(elementText.substring(start, index)));
                // highlighted text
                start = index + text.length;
                var hlText = document.createTextNode(elementText.substring(index, start));
                var hl = document.createElement("span");
                hl.appendChild(hlText);
                hl.setAttribute("class", "configuration-highlight");
                fragment.appendChild(hl);
            }while((index = elementTextLC.indexOf(text, start)) != -1);
            // text after
            n.nodeValue = elementText.substring(start);
            // replace
            parent.insertBefore(fragment, n);
        }
    }
    iter.detach();
}

function clearHighlights(table){
    for (var span of table.querySelectorAll("span.configuration-highlight")) {
        var parent = span.parentNode;
        var prev = span.previousSibling;
        var next = span.nextSibling;
        var target;
        if(prev && prev.nodeType == Node.TEXT_NODE){
            target = prev;
        }
        var text = span.childNodes.item(0).nodeValue;
        if(next && next.nodeType == Node.TEXT_NODE){
            text += next.nodeValue;
            parent.removeChild(next);
        }
        if(target){
            target.nodeValue += text;
        }else{
            target = document.createTextNode(text);
            parent.insertBefore(target, span);
        }
        parent.removeChild(span);
    }
}

function findText(row, search){
    var iter = document.createNodeIterator(row, NodeFilter.SHOW_TEXT, null);

    while (n = iter.nextNode()){
        var elementText = n.nodeValue;
        if(elementText == undefined)
            continue;
        if(elementText.toLowerCase().indexOf(search) != -1
            // check that it's not decoration
            && acceptTextForSearch(n)){
            iter.detach();
            return true;
        }
    }
    iter.detach();
    return false;
}

function acceptTextForSearch(n){
  var classes = n.parentNode.classList;
  return !classes.contains("link-collapsible")
    && !classes.contains("description-label");
}

function getShadowTable(input){
    if(!inputs[input.id].shadowTable){
        inputs[input.id].shadowTable = inputs[input.id].table.cloneNode(true);
        reinstallClickHandlers(inputs[input.id].shadowTable);
    }
    return inputs[input.id].shadowTable;
}

function reinstallClickHandlers(table){
    var descriptions = table.querySelectorAll(".description");
    if(descriptions){
        for (descDiv of descriptions){
            if(!descDiv.classList.contains("description-collapsed"))
                continue;
            var content = descDiv.parentNode;
            var td = getAncestor(descDiv, "td");
            var row = td.parentNode;
            var decoration = content.lastElementChild;
            var iconDecoration = decoration.firstElementChild.children.item(0);
            var collapsibleSpan = decoration.firstElementChild.children.item(1);
            var collapsibleHandler = makeCollapsibleHandler(descDiv, td, row,
                collapsibleSpan,
                iconDecoration);

            row.addEventListener("click", collapsibleHandler);
        }
    }
}

function swapShadowTable(input){
    var currentTable = inputs[input.id].table;
    var shadowTable = inputs[input.id].shadowTable;

    // makes sure hidden rows are always displayed when search term is defined
    if (shadowTable.classList.contains('configuration-reference-all-rows')) {
        shadowTable.classList.remove('configuration-reference-all-rows');
    }

    currentTable.parentNode.replaceChild(shadowTable, currentTable);
    inputs[input.id].table = shadowTable;
    inputs[input.id].shadowTable = currentTable;
}

function search(input){
    var search = input.value.trim().toLowerCase();
    var lastSearch = inputs[input.id].lastSearch;
    if(search == lastSearch)
        return;
    // work on shadow table
    var table = getShadowTable(input);

    applySearch(table, search, true);

    inputs[input.id].lastSearch = search;
    // swap tables
    swapShadowTable(input);
}

function applySearch(table, search, autoExpand){
    // clear highlights
    clearHighlights(table);
    var lastSectionHeader = null;
    var idx = 0;
    for (var row of table.querySelectorAll("table.configuration-reference > tbody > tr")) {
        var heads = row.querySelectorAll("table.configuration-reference > tbody > tr > th");
        if(!heads || heads.length == 0){
            // mark even rows
            if(++idx % 2){
                row.classList.add("odd");
            }else{
                row.classList.remove("odd");
            }
        }else{
            // reset count at each section
            idx = 0;
        }
        if(!search){
            row.style.removeProperty("display");
            // recollapse when searching is over
            if(autoExpand
                && row.classList.contains("row-collapsible")
                && !row.classList.contains("row-collapsed"))
                row.click();
        }else{
            if(heads && heads.length > 0){
                // keep the column header with no highlight, but start hidden
                lastSectionHeader = row;
                row.style.display = "none";
            }else if(findText(row, search)){
                row.style.removeProperty("display");
                // expand if shown
                if(autoExpand && row.classList.contains("row-collapsed"))
                    row.click();
                highlight(row, search);
                if(lastSectionHeader){
                    lastSectionHeader.style.removeProperty("display");
                    // avoid showing it more than once
                    lastSectionHeader = null;
                }
            }else{
                row.style.display = "none";
            }
        }
    }
}

function getAncestor(element, name){
    for ( ; element && element !== document; element = element.parentNode ) {
        if ( element.localName == name )
            return element;
    }
    return null;
}

function makeCollapsibleHandler(descDiv, td, row,
                                collapsibleSpan,
                                iconDecoration) {

    return function(event) {
        var target = event.target;
        if( (target.localName == 'a' || getAncestor(target, "a"))) {
            return;
        }

        // don't collapse if the target is button with attribute "do-not-collapse"
        if( (target.localName == 'button' && target.hasAttribute("do-not-collapse"))) {
            return;
        }

        var isCollapsed = descDiv.classList.contains('description-collapsed');
        if( isCollapsed ) {
            collapsibleSpan.childNodes.item(0).nodeValue = 'Show less';
            iconDecoration.classList.replace('fa-chevron-down', 'fa-chevron-up');
        }
        else {
            collapsibleSpan.childNodes.item(0).nodeValue = 'Show more';
            iconDecoration.classList.replace('fa-chevron-up', 'fa-chevron-down');
        }
        descDiv.classList.toggle('description-collapsed');
        descDiv.classList.toggle('description-expanded');
        row.classList.toggle('row-collapsed');
    };
}

});
