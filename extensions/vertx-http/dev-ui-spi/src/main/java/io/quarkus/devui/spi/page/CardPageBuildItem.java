package io.quarkus.devui.spi.page;

import java.util.Optional;

/**
 * Add a page (or section) to the Dev UI. This is typically the middle part of the screen.
 * This will also add links to this pages
 */
public final class CardPageBuildItem extends AbstractPageBuildItem {

    private Optional<Card> optionalCard = Optional.empty();

    public CardPageBuildItem() {
        super();
    }

    public void setCustomCard(String cardComponent) {
        if (cardComponent != null) {
            this.optionalCard = Optional.of(new Card(cardComponent));
        }
    }

    public Optional<Card> getOptionalCard() {
        return this.optionalCard;
    }
}
