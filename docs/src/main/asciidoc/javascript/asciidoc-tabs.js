// code originally coming from:
// https://github.com/bmuschko/asciidocj-tabbed-code-extension
// adapted to work with jQuery

$(document).ready(function() {
    function addBlockSwitches() {
        $('.listingblock.primary, .sidebarblock.primary').each(function() {
            var primary = $(this);
            createSwitchItem(primary, createBlockSwitch(primary)).item.addClass("selected");
            if (primary.children('.title').length) {
                primary.children('.title').remove();
            } else {
                primary.children('.content').first().children('.title').remove();
            }
            getAllSyncClasses(primary).forEach(className => primary.removeClass(className));
        });

        $('.listingblock.secondary, .sidebarblock.secondary').each(function(idx, node) {
            var secondary = $(node);
            var primary = findPrimary(secondary);
            var switchItem = createSwitchItem(secondary, primary.children('.asciidoc-tabs-switch'));
            switchItem.content.addClass('asciidoc-tabs-hidden');
            findPrimary(secondary).append(switchItem.content);
            secondary.remove();
        });
    }

    function createBlockSwitch(primary) {
        var blockSwitch = $('<div class="asciidoc-tabs-switch"></div>');
        primary.prepend(blockSwitch);
        return blockSwitch;
    }

    function findPrimary(secondary) {
        return secondary.prev('.primary');
    }

    function getSyncClasses(element) {
        return element.attr('class').replaceAll(/\s+/g, ' ').split(' ').filter(className => className.startsWith('asciidoc-tabs-sync'));
    }

    function getTargetSyncClasses(element) {
        return element.attr('class').replaceAll(/\s+/g, ' ').split(' ').filter(className => className.startsWith('asciidoc-tabs-target-sync'));
    }

    function getAllSyncClasses(element) {
        return element.attr('class').replaceAll(/\s+/g, ' ').split(' ').filter(className => className.startsWith('asciidoc-tabs-sync') || className.startsWith('asciidoc-tabs-target-sync'));
    }

    function triggerSyncEvent(element) {
        var syncClasses = getSyncClasses(element);
        if (syncClasses.length > 0) {
            $('.asciidoc-tabs-switch--item.' + syncClasses[0] + ':not(.selected)').not(element).click();
            $('.asciidoc-tabs-switch--item.' + syncClasses[0].replace('asciidoc-tabs-sync', 'asciidoc-tabs-target-sync') + ':not(.selected)').not(element).click();
        }
        var targetSyncClasses = getTargetSyncClasses(element);
        for (const targetSyncClass of targetSyncClasses) {
            $('.asciidoc-tabs-switch--item.' + targetSyncClass + ':not(.selected)').not(element).click();
        }
    }

    function createSwitchItem(block, blockSwitch) {
        var blockName;
        if (block.children('.title').length) {
            blockName = block.children('.title').text();
        } else {
            blockName = block.children('.content').first().children('.title').text();
            block.children('.content').first().children('.title').remove();
        }
        var allSyncClasses = getAllSyncClasses(block);
        var content = block.children('.content').first().append(block.next('.colist'));
        var item = $('<div class="asciidoc-tabs-switch--item ' + allSyncClasses.join(' ') + '">' + blockName + '</div>');
        item.on('click', '', content, function(e) {
            $(this).addClass('selected');
            $(this).siblings().removeClass('selected');
            e.data.siblings('.content').addClass('asciidoc-tabs-hidden');
            e.data.removeClass('asciidoc-tabs-hidden');

            triggerSyncEvent($(this));
        });
        blockSwitch.append(item);
        return {'item': item, 'content': content};
    }

    addBlockSwitches();
});
