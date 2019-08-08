package io.quarkus.it.jaxb.object;

import java.util.Date;

/**
 * Describes the news objects proposed by erpds.
 */
public interface INews {

    /**
     * @return the unique {@link INews} identifier.
     */
    public Long getId();

    /**
     * @return the {@link INews} title.
     */
    public String getTitle();

    /**
     * @return the {@link INews} description.
     */
    public String getDescription();

    /**
     * @return the {@link INews} author name.
     */
    public String getAuthor();

    /**
     * @return the exact date when the {@link INews} has been requested.
     */
    public Date getRequestedDate();

    /**
     * @return the actual {@link INews} date.
     */
    public Date getDate();

    /**
     * @return the {@link INews} category.
     */
    public Category getCategory();

    /**
     * @return the {@link INews} url.
     */
    public String getUrl();

}