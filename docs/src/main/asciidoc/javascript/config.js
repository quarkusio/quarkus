jQuery(function(){
/*
 * SEARCH
 */
$('table.configuration-reference').each(function() {
    var table = $(this);
    var rows = table.find("tr");
    var caption = table.prev();
    // add the input element
    var input = $("<input/>").attr("type", "search").attr("placeholder", "filter configuration");
    caption.children("p").prepend(input);
    input.keyup(function (){
        search(table, rows, input.val());
    });
    input.on("input", function (){
        search(table, rows, input.val());
    });
});

function highlight(element, text){
    element.contents()
        .each(function() {
            if(this.nodeType === 3){
                var elementText = this.nodeValue;
                if(elementText == undefined)
                    return;
                var elementTextLC = elementText.toLowerCase(); 
                var index = elementTextLC.indexOf(text);
                if(index != -1){
                    var start = 0;
                    var newElements = [];
                    // we use the DOM here to avoid &lt; and such being parsed as elements by jQuery when replacing content
                    do{
                        // text before
                        newElements.push(document.createTextNode(elementText.substring(start, index)));
                        // highlighted text
                        start = index + text.length;
                        var hlText = document.createTextNode(elementText.substring(index, start));
                        var hl = document.createElement("span");
                        hl.appendChild(hlText);
                        hl.setAttribute("class", "configuration-highlight");
                        newElements.push(hl);
                    }while((index = elementTextLC.indexOf(text, start)) != -1);
                    // text after
                    if(start < elementText.length)
                        newElements.push(document.createTextNode(elementText.substring(start)));
                    // replace
                    $(this).replaceWith(newElements);
                }
            }else if(this.nodeType === 1){
                // recurse
                highlight($(this), text);
            }
        });
}

function search(table, rows, val){
    var search = val.trim().toLowerCase();
    var lastSearch = table.data("lastSearch");
    if(search == lastSearch)
        return;
    // clear highlights
    rows.find("span.configuration-highlight").replaceWith(function() { return document.createTextNode($(this).text()); });
    rows.each(function() {
        this.normalize();
        var row = $(this);
        if(search.length == 0){
            row.show();
        }else{
            if(row.children("th").length > 1
               || row.text().toLowerCase().indexOf(search) != -1){
                row.show();
                // expand if shown
                if(row.hasClass("row-collapsed"))
                    row.click();
                highlight(row, search);
            }else{
                row.hide();
            }
        }
    });
    // recollapse them all when searching is over
    if(search.length == 0)
        rows.filter(".row-collapsible").not(".row-collapsed").click();
    table.data("lastSearch", search);
}

/*
 * COLLAPSIBLE DESCRIPTION
 */
$('table .description').each(function() {
    var descDiv = $(this);
    var descHeightLong = descDiv.height();
    var descHeightShort = descDiv.addClass('description-collapsed').height();
    var td = descDiv.parents("td");
    var row = td.parent();

    if (descHeightLong - descHeightShort > 16) {
        var iconDecoration1 = $('<i/>').addClass('fa fa-chevron-down');
        var iconDecoration2 = $('<i/>').addClass('fa fa-chevron-down');
        var iconDecoration3 = $('<i/>').addClass('fa fa-chevron-down');
        var descDecoration = $('<div/>').addClass('description-decoration').append(iconDecoration1).append(iconDecoration2).append(iconDecoration3);            
        var iconExpand = $('<i/>').addClass('fa fa-chevron-down');
        var iconExpandAll = $('<i/>').addClass('fa fa-chevron-circle-down');
        var iconCollapse = $('<i/>').addClass('fa fa-chevron-up');
        var iconCollapseAll = $('<i/>').addClass('fa fa-chevron-circle-up');
        var collapsibleLink = $('<a/>').attr('href', '#').text('Expand').addClass('link-collapsible').prepend(iconExpand);
        var collapsibleAllLink = $('<a/>').attr('href', '#').text('(All)').addClass('link-collapsible').prepend(iconExpandAll);

        var collapsibleHandler = function(event) {
            var target = $(event.target);
            if( target.is('a') && !target.is('.link-collapsible') ) {
                return true;
            }
            if( target.is('i') && (target.is('.icon-link') || target.is('.icon-source-code')) ) {
                return true;
            }
            if( target.is('b') && target.is('.caret') ) {
                return true;
            }

            collapsibleAllLink.text('(All)');
            var isCollapsed = descDiv.hasClass('description-collapsed');
            if( isCollapsed ) {
                collapsibleLink.text('Collapse');
                collapsibleLink.prepend(iconCollapse);
                collapsibleAllLink.prepend(iconCollapseAll);
                iconDecoration1.removeClass('fa-chevron-down').addClass('fa-chevron-up');
                iconDecoration2.removeClass('fa-chevron-down').addClass('fa-chevron-up');
                iconDecoration3.removeClass('fa-chevron-down').addClass('fa-chevron-up');
                td.attr("colspan", 3);
                var typeCell = td.next().children("p");
                if(typeCell.length > 0){
                    typeCell.clone().addClass("remove-on-collapse").prepend("Type: ").appendTo(descDiv);
                }
                var defaultCell = td.next().next().children("p");
                if(defaultCell.length > 0){
                    defaultCell.clone().addClass("remove-on-collapse").prepend("Defaults to: ").appendTo(descDiv);
                }
            }
            else {
                collapsibleLink.text('Expand');
                collapsibleLink.prepend(iconExpand);
                collapsibleAllLink.prepend(iconExpandAll);
                iconDecoration1.removeClass('fa-chevron-up').addClass('fa-chevron-down');
                iconDecoration2.removeClass('fa-chevron-up').addClass('fa-chevron-down');
                iconDecoration3.removeClass('fa-chevron-up').addClass('fa-chevron-down');
                td.removeAttr("colspan");
                descDiv.find(".remove-on-collapse").remove();
            }
            descDiv.toggleClass('description-collapsed');
            row.toggleClass('row-collapsed');
            td.siblings().toggleClass("hidden");
            
            if( target.is('a') && target.is('.link-collapsible') ) {
                return false;
            } else {
                return true;
            }
        };
        var collapsibleAllHandler = function(event) {
            var target = $(event.target);

            var isCollapsed = descDiv.hasClass('description-collapsed');
            if( isCollapsed ) {
              $(".description-collapsed").parents("tr").click();
            }else{
              $(".description").not(".description-collapsed").parents("tr").click();
            }
            if( target.is('a') && target.is('.link-collapsible') ) {
                return false;
            } else {
                return true;
            }
        };

        collapsibleLink.click(collapsibleHandler);
        collapsibleAllLink.click(collapsibleAllHandler);

        descDiv.parent().prepend(collapsibleLink);
        descDiv.parent().prepend(collapsibleAllLink);

        descDiv.parent().append(descDecoration);
        row.addClass("row-collapsible row-collapsed");
        row.click(collapsibleHandler);
    }
    else {
        descDiv.removeClass('description-collapsed');
    }

});

});