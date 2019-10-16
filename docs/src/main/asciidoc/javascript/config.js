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
        var input = document.createElement("input");
        input.setAttribute("type", "search");
        input.setAttribute("placeholder", "filter configuration");
        input.id = "config-search-"+(idx++);
        caption.children.item(0).appendChild(input);
        input.addEventListener("keyup", initiateSearch);
        input.addEventListener("input", initiateSearch);
        inputs[input.id] = {"table": table};
        var descriptions = table.querySelectorAll(".description");
        if(descriptions){
            for (description of descriptions){
                makeCollapsible(input, description);
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
    // return row.text().toLowerCase().indexOf(search) != -1;
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
        reinstallClickHandlers(input, inputs[input.id].shadowTable);
    }
    return inputs[input.id].shadowTable;
}

function reinstallClickHandlers(input, table){
    var descriptions = table.querySelectorAll(".description");
    if(descriptions){
        for (descDiv of descriptions){
            if(!descDiv.classList.contains("description-collapsed"))
                continue;
            var content = descDiv.parentNode;
            var td = getAncestor(descDiv, "td");
            var row = td.parentNode;
            var decoration = content.lastElementChild;
            var iconDecoration1 = decoration.children.item(0);
            var iconDecoration2 = decoration.children.item(1);
            var iconDecoration3 = decoration.children.item(2);
            var collapsibleLink = content.children.item(1);
            var collapsibleAllLink = content.children.item(0);
            var iconExpand = collapsibleLink.children.item(0);
            var iconCollapse = collapsibleLink.children.item(1);
            var iconExpandAll = collapsibleAllLink.children.item(0);
            var iconCollapseAll = collapsibleAllLink.children.item(1);
            var collapsibleHandler = makeCollapsibleHandler(input, descDiv, td, row, 
                                                            collapsibleLink, collapsibleAllLink,
                                                            iconCollapse, iconCollapseAll, iconExpand, iconExpandAll,
                                                            iconDecoration1, iconDecoration2, iconDecoration3);
            var collapsibleAllHandler = makeCollapsibleAllHandler(input, descDiv);
        
            collapsibleLink.addEventListener('click', collapsibleHandler);
            collapsibleAllLink.addEventListener('click', collapsibleAllHandler);
        
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
    for (var row of table.querySelectorAll("tr")) {
        if(!search){
            row.style.removeProperty("display");
            // recollapse when searching is over
            if(autoExpand 
                && row.classList.contains("row-collapsible")
                && !row.classList.contains("row-collapsed"))
                row.click();
        }else{
            var heads = row.querySelectorAll("th");
            if(heads && heads.length > 0){
                if(heads.length > 1){
                    // always show the top header, never highlight it
                    row.style.removeProperty("display");
                }else{
                    // keep the header rows for rows who matched, but start hidden
                    lastSectionHeader = row;
                    highlight(row, search);
                    row.style.display = "none";
                }
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
function makeCollapsible(input, descDiv){
    var descHeightLong = descDiv.offsetHeight;
    var td = getAncestor(descDiv, "td");
    var row = td.parentNode;

    // this causes a relayout and is expensive, so only do that if we're not sure it won't require collapsing
    var descHeightShort = descHeightLong;
    if(descHeightLong > 25){
        descDiv.classList.add('description-collapsed');
        descHeightShort = descDiv.offsetHeight;
    }

    if (descHeightLong - descHeightShort > 16) {
        var iconDecoration1 = document.createElement("i");
        iconDecoration1.classList.add('fa', 'fa-chevron-down');
        var iconDecoration2 = iconDecoration1.cloneNode();
        var iconDecoration3 = iconDecoration1.cloneNode();
        var descDecoration = document.createElement("div");
        descDecoration.classList.add('description-decoration');
        descDecoration.appendChild(iconDecoration1);
        descDecoration.appendChild(iconDecoration2);
        descDecoration.appendChild(iconDecoration3);

        var iconExpand = document.createElement("i");
        iconExpand.classList.add('fa', 'fa-chevron-down');
        var iconExpandAll = document.createElement("i");
        iconExpandAll.classList.add('fa', 'fa-chevron-circle-down');
        var iconCollapse = document.createElement("i");
        iconCollapse.classList.add('fa', 'fa-chevron-up');
        iconCollapse.style.display = "none";
        var iconCollapseAll = document.createElement("i");
        iconCollapseAll.classList.add('fa', 'fa-chevron-circle-up');
        iconCollapseAll.style.display = "none";

        var collapsibleLink = document.createElement("a");
        collapsibleLink.setAttribute("href", "#");
        collapsibleLink.appendChild(iconExpand);
        collapsibleLink.appendChild(iconCollapse);
        collapsibleLink.appendChild(document.createTextNode("Expand"));
        collapsibleLink.classList.add('link-collapsible');

        var collapsibleAllLink = document.createElement("a");
        collapsibleAllLink.setAttribute("href", "#");
        collapsibleAllLink.appendChild(iconExpandAll);
        collapsibleAllLink.appendChild(iconCollapseAll);
        collapsibleAllLink.appendChild(document.createTextNode("(All)"));
        collapsibleAllLink.classList.add('link-collapsible');
        
        var collapsibleHandler = makeCollapsibleHandler(input, descDiv, td, row, 
                                                        collapsibleLink, collapsibleAllLink,
                                                        iconCollapse, iconCollapseAll, iconExpand, iconExpandAll,
                                                        iconDecoration1, iconDecoration2, iconDecoration3);
        var collapsibleAllHandler = makeCollapsibleAllHandler(input, descDiv);

        collapsibleLink.addEventListener('click', collapsibleHandler);
        collapsibleAllLink.addEventListener('click', collapsibleAllHandler);

        var parent = descDiv.parentNode;
        parent.insertBefore(collapsibleLink, parent.firstChild);
        parent.insertBefore(collapsibleAllLink, parent.firstChild);

        parent.appendChild(descDecoration);
        row.classList.add("row-collapsible", "row-collapsed");
        row.addEventListener("click", collapsibleHandler);
    }
    else {
        descDiv.classList.remove('description-collapsed');
    }

};

function makeCollapsibleHandler(input, descDiv, td, row, 
    collapsibleLink, collapsibleAllLink,
    iconCollapse, iconCollapseAll, iconExpand, iconExpandAll,
    iconDecoration1, iconDecoration2, iconDecoration3) {
    
    return function(event) {
        var target = event.target;
        if( (target.localName == 'a' || getAncestor(target, "a"))
            && target != collapsibleLink ) {
            return;
        }

        var isCollapsed = descDiv.classList.contains('description-collapsed');
        if( isCollapsed ) {
            iconCollapse.style.removeProperty("display");
            iconExpand.style.display = "none";
            collapsibleLink.childNodes.item(1).nodeValue = 'Collapse';
            iconCollapseAll.style.removeProperty("display");
            iconExpandAll.style.display = "none";
            iconDecoration1.classList.replace('fa-chevron-down', 'fa-chevron-up');
            iconDecoration2.classList.replace('fa-chevron-down', 'fa-chevron-up');
            iconDecoration3.classList.replace('fa-chevron-down', 'fa-chevron-up');
            td.setAttribute("colspan", 3);
            var typeCell = td.nextElementSibling.firstElementChild;
            if(typeCell){
                var cell = typeCell.cloneNode(true);
                cell.classList.add("remove-on-collapse");
                var labelSpan = document.createElement("span");
                labelSpan.appendChild(document.createTextNode("Type: "));
                labelSpan.classList.add("description-label");
                cell.insertBefore(labelSpan, cell.firstChild);
                descDiv.appendChild(cell);
            }
            var defaultCell = td.nextElementSibling.nextElementSibling.firstElementChild;
            if(defaultCell){
                var cell = defaultCell.cloneNode(true);
                cell.classList.add("remove-on-collapse");
                var labelSpan = document.createElement("span");
                labelSpan.appendChild(document.createTextNode("Defaults to: "));
                labelSpan.classList.add("description-label");
                cell.insertBefore(labelSpan, cell.firstChild);
                descDiv.appendChild(cell);
            }
        }
        else {
            iconExpand.style.removeProperty("display");
            iconCollapse.style.display = "none";
            collapsibleLink.childNodes.item(1).nodeValue = 'Expand';
            iconExpandAll.style.removeProperty("display");
            iconCollapseAll.style.display = "none";
            iconDecoration1.classList.replace('fa-chevron-up', 'fa-chevron-down');
            iconDecoration2.classList.replace('fa-chevron-up', 'fa-chevron-down');
            iconDecoration3.classList.replace('fa-chevron-up', 'fa-chevron-down');
            td.removeAttribute("colspan");
            var toRemoveList = descDiv.querySelectorAll(".remove-on-collapse");
            if(toRemoveList){
                for(var toRemove of toRemoveList){
                    toRemove.parentNode.removeChild(toRemove);
                }
            }
        }
        descDiv.classList.toggle('description-collapsed');
        descDiv.classList.toggle('description-expanded');
        row.classList.toggle('row-collapsed');
        td.nextElementSibling.classList.toggle("hidden");
        td.nextElementSibling.nextElementSibling.classList.toggle("hidden");
        
        if( target.localName == 'a' && target.classList.contains('link-collapsible') ) {
            event.preventDefault();
            event.stopPropagation();
        }
    };
}


function makeCollapsibleAllHandler(input, descDiv) {
    return function(event){
        var target = event.target;

        var isCollapsed = descDiv.classList.contains('description-collapsed');
        
        var table = getShadowTable(input);
        collapseAll(table, isCollapsed);
        applySearch(table, inputs[input.id].lastSearch, false);
        swapShadowTable(input);
        
        event.preventDefault();
        event.stopPropagation();
    };
}

function collapseAll(table, isCollapsed){
    var target = isCollapsed ? ".description-collapsed" : ".description-expanded";
    var toClickList = table.querySelectorAll(target);
    if(toClickList){
        for(var toClick of toClickList){
            var tr = getAncestor(toClick, "tr");
            tr.click();
        }
    }
}

});