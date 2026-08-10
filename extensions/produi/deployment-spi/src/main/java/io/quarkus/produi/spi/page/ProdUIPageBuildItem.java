package io.quarkus.produi.spi.page;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import io.quarkus.devui.spi.page.AbstractPageBuildItem;
import io.quarkus.devui.spi.page.Card;
import io.quarkus.devui.spi.page.CardAction;
import io.quarkus.devui.spi.page.CardText;

/**
 * Add a page to the Prod UI. Extensions produce this build item to contribute
 * pages and data to the production UI served on the management interface.
 * <p>
 * This mirrors {@code CardPageBuildItem} from Dev UI but is processed only
 * when {@code quarkus.prod-ui.enabled=true} in production mode.
 */
public final class ProdUIPageBuildItem extends AbstractPageBuildItem {

    private Optional<Card> optionalCard = Optional.empty();
    private List<CardAction> cardActions;
    private List<CardText> cardTexts;

    private String darkLogo;
    private String lightLogo;

    public ProdUIPageBuildItem() {
        super();
    }

    public ProdUIPageBuildItem(String customIdentifier) {
        super(customIdentifier);
    }

    public void setCustomCard(String cardComponent) {
        if (cardComponent != null) {
            this.optionalCard = Optional.of(new Card(cardComponent));
        }
    }

    public Optional<Card> getOptionalCard() {
        return this.optionalCard;
    }

    public void addAction(CardAction action) {
        if (cardActions == null)
            cardActions = new ArrayList<>();
        cardActions.add(action);
    }

    public List<CardAction> getCardActions() {
        return this.cardActions == null ? Collections.emptyList() : this.cardActions;
    }

    public boolean hasCardActions() {
        return this.cardActions != null && !this.cardActions.isEmpty();
    }

    public void addText(CardText text) {
        if (cardTexts == null)
            cardTexts = new ArrayList<>();
        cardTexts.add(text);
    }

    public List<CardText> getCardTexts() {
        return this.cardTexts == null ? Collections.emptyList() : this.cardTexts;
    }

    public boolean hasCardTexts() {
        return this.cardTexts != null && !this.cardTexts.isEmpty();
    }

    public void setLogo(String darkLogo, String lightLogo) {
        this.darkLogo = darkLogo;
        this.lightLogo = lightLogo;
    }

    public boolean hasLogo() {
        return this.darkLogo != null && this.lightLogo != null;
    }

    public String getDarkLogo() {
        return this.darkLogo;
    }

    public String getLightLogo() {
        return this.lightLogo;
    }
}
