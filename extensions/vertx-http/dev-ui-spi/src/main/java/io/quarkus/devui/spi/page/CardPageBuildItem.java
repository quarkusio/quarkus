package io.quarkus.devui.spi.page;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Add a page (or section) to the Dev UI. This is typically the middle part of the screen.
 * This will also add links to this pages
 */
public final class CardPageBuildItem extends AbstractPageBuildItem {

    private final List<PageBuilder> pageBuilders;
    private Optional<Card> optionalCard = Optional.empty();

    public CardPageBuildItem(String extensionName) {
        super(extensionName);
        this.pageBuilders = new ArrayList<>();
    }

    public void addPage(PageBuilder page) {
        this.pageBuilders.add(page);
    }

    public void setCustomCard(String cardComponent) {
        if (cardComponent != null) {
            this.optionalCard = Optional.of(new Card(cardComponent));
        }
    }

    public List<PageBuilder> getPages() {
        return this.pageBuilders;
    }

    public Optional<Card> getOptionalCard() {
        return this.optionalCard;
    }
}
