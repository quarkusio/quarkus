package io.quarkus.devui.spi.page;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * Add a page (or section) to the Dev UI. This is typically the middle part of the screen.
 * This will also add links to this pages
 */
public final class CardPageBuildItem extends AbstractPageBuildItem {

    private Optional<Card> optionalCard = Optional.empty();
    private List<LibraryLink> libraryVersions = null;

    private String darkLogo = null;
    private String lightLogo = null;

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

    public void addLibraryVersion(String groupId, String artifactId, String name, String url) {
        try {
            addLibraryVersion(groupId, artifactId, name, URI.create(url).toURL());
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void addLibraryVersion(String groupId, String artifactId, String name, URL url) {
        if (libraryVersions == null)
            libraryVersions = new LinkedList<>();
        libraryVersions.add(new LibraryLink(groupId, artifactId, name, url));
    }

    public List<LibraryLink> getLibraryVersions() {
        return this.libraryVersions;
    }

    public boolean hasLibraryVersions() {
        return this.libraryVersions != null && !this.libraryVersions.isEmpty();
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
