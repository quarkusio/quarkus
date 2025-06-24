package io.quarkus.devui.deployment.extension;

import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import io.quarkus.devui.spi.page.Card;
import io.quarkus.devui.spi.page.LibraryLink;
import io.quarkus.devui.spi.page.Page;

public class Extension {
    private String namespace;
    private String artifact;
    private String name;
    private String shortName;
    private String description;
    private URL guide;
    private List<String> keywords;
    private String status;
    private List<String> configFilter;
    private List<String> categories;
    private String unlisted;
    private String builtWith;
    private List<String> providesCapabilities;
    private List<String> extensionDependencies;
    private Codestart codestart;
    private final List<Page> cardPages = new ArrayList<>();
    private final List<Page> menuPages = new ArrayList<>();
    private final List<Page> footerPages = new ArrayList<>();
    private Card card = null; // Custom card
    private List<LibraryLink> libraryLinks = null;
    private String darkLogo = null;
    private String lightLogo = null;
    private String headlessComponent = null;

    public Extension() {

    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getArtifact() {
        return artifact;
    }

    public void setArtifact(String artifact) {
        this.artifact = artifact;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortName() {
        return shortName;
    }

    public boolean hasLibraryLinks() {
        return libraryLinks != null && !libraryLinks.isEmpty();
    }

    public List<LibraryLink> getLibraryLinks() {
        return libraryLinks;
    }

    public void addLibraryLink(LibraryLink libraryLink) {
        if (this.libraryLinks == null)
            this.libraryLinks = new LinkedList<>();
        this.libraryLinks.add(libraryLink);
    }

    public String getDarkLogo() {
        return this.darkLogo;
    }

    public String getLightLogo() {
        return this.lightLogo;
    }

    public void setLogo(String darkLogo, String lightLogo) {
        this.darkLogo = darkLogo;
        this.lightLogo = lightLogo;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public URL getGuide() {
        return guide;
    }

    public void setGuide(URL guide) {
        this.guide = guide;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getConfigFilter() {
        return configFilter;
    }

    public void setConfigFilter(List<String> configFilter) {
        this.configFilter = configFilter;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public String getUnlisted() {
        return unlisted;
    }

    public void setUnlisted(String unlisted) {
        this.unlisted = unlisted;
    }

    public String getBuiltWith() {
        return builtWith;
    }

    public void setBuiltWith(String builtWith) {
        this.builtWith = builtWith;
    }

    public List<String> getProvidesCapabilities() {
        return providesCapabilities;
    }

    public void setProvidesCapabilities(List<String> providesCapabilities) {
        this.providesCapabilities = providesCapabilities;
    }

    public List<String> getExtensionDependencies() {
        return extensionDependencies;
    }

    public void setExtensionDependencies(List<String> extensionDependencies) {
        this.extensionDependencies = extensionDependencies;
    }

    public Codestart getCodestart() {
        return codestart;
    }

    public void setCodestart(Codestart codestart) {
        this.codestart = codestart;
    }

    public void addCardPage(Page page) {
        this.cardPages.add(page);
    }

    public void addCardPages(List<Page> pages) {
        this.cardPages.addAll(pages);
    }

    public List<Page> getCardPages() {
        return cardPages;
    }

    public void addMenuPage(Page page) {
        this.menuPages.add(page);
    }

    public void addMenuPages(List<Page> pages) {
        this.menuPages.addAll(pages);
    }

    public List<Page> getMenuPages() {
        return menuPages;
    }

    public void addFooterPage(Page page) {
        this.footerPages.add(page);
    }

    public void addFooterPages(List<Page> pages) {
        this.footerPages.addAll(pages);
    }

    public List<Page> getFooterPages() {
        return footerPages;
    }

    public void setCard(Card card) {
        this.card = card;
    }

    public Card getCard() {
        return this.card;
    }

    public boolean hasCard() {
        return this.card != null;
    }

    public void setHeadlessComponent(String headlessComponent) {
        this.headlessComponent = headlessComponent;
    }

    public String getHeadlessComponent() {
        return this.headlessComponent;
    }

    public String getHeadlessComponentRef() {
        if (headlessComponent != null) {
            return DOT + SLASH + DOT + DOT + SLASH + this.namespace + SLASH + this.headlessComponent;
        }
        return null;
    }

    @Override
    public String toString() {
        return "Extension{" + "namespace=" + namespace + ", artifact=" + artifact + ", name=" + name + ", shortName="
                + shortName + ", description=" + description + ", guide=" + guide + ", keywords=" + keywords + ", status="
                + status + ", configFilter=" + configFilter + ", categories=" + categories + ", unlisted=" + unlisted
                + ", builtWith=" + builtWith + ", providesCapabilities=" + providesCapabilities + ", extensionDependencies="
                + extensionDependencies + ", codestart=" + codestart + '}';
    }

    private static final String SLASH = "/";
    private static final String DOT = ".";
}
