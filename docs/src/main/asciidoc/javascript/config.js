jQuery(function(){
/*
 * SEARCH
 */
var inputs = {};
var tables = document.querySelectorAll("table.configuration-reference");
var typingTimer;

if(tables){
    var idx = 0;
    for (var table of tables) {
        var caption = table.previousElementSibling;
        if (table.classList.contains('searchable')) { // activate search engine only when needed
          var input = document.createElement("input");
          input.setAttribute("type", "search");
          input.setAttribute("placeholder", "FILTER CONFIGURATION");
          input.id = "config-search-"+(idx++);
          caption.children.item(0).appendChild(input);
          input.addEventListener("keyup", initiateSearch);
          input.addEventListener("input", initiateSearch);
          var descriptions = table.querySelectorAll(".description");
          if(descriptions){
            var heights = new Array(descriptions.length);
            var h = 0;
            for (description of descriptions){
              heights[h++] = description.offsetHeight;
            }
            var shadowTable = table.cloneNode(true);
            var shadowDescriptions = shadowTable.querySelectorAll(".description");
            h = 0;
            for (shadowDescription of shadowDescriptions){
              makeCollapsible(shadowDescription, heights[h++]);
            }
            table.parentNode.replaceChild(shadowTable, table);
            table = shadowTable;
          }
          inputs[input.id] = {"table": table};
        }

        var rowIdx = 0;
        for (var row of table.querySelectorAll("table.configuration-reference > tbody > tr")) {
            var heads = row.querySelectorAll("table.configuration-reference > tbody > tr > th");
            if(!heads || heads.length == 0){
                // mark even rows
                if(++rowIdx % 2){
                    row.classList.add("odd");
                }else{
                    row.classList.remove("odd");
                }
            }else{
                // reset count at each section
                rowIdx = 0;
            }
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
            var iconDecoration = decoration.children.item(0);
            var collapsibleSpan = decoration.children.item(1);
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

/*
 * COLLAPSIBLE DESCRIPTION
 */
function makeCollapsible(descDiv, descHeightLong){
    if (descHeightLong > 25) {
        var td = getAncestor(descDiv, "td");
        var row = td.parentNode;
        var iconDecoration = document.createElement("i");
        descDiv.classList.add('description-collapsed');
        iconDecoration.classList.add('fa', 'fa-chevron-down');

        var descDecoration = document.createElement("div");
        descDecoration.classList.add('description-decoration');
        descDecoration.appendChild(iconDecoration);

        var collapsibleSpan = document.createElement("span");
        collapsibleSpan.appendChild(document.createTextNode("Show more"));
        descDecoration.appendChild(collapsibleSpan);

        var collapsibleHandler = makeCollapsibleHandler(descDiv, td, row,
                                                        collapsibleSpan,
                                                        iconDecoration);

        var parent = descDiv.parentNode;

        parent.appendChild(descDecoration);
        row.classList.add("row-collapsible", "row-collapsed");
        row.addEventListener("click", collapsibleHandler);
    }

};

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
