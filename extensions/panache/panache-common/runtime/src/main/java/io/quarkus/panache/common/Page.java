package io.quarkus.panache.common;

/**
 * <p>
 * Utility class to represent paging information. Page instances are immutable.
 * </p>
 *
 * <p>
 * Usage:
 * </p>
 * 
 * <code><pre>
 * Page page = Page.ofSize(25);
 * Page secondPage = page.next();
 * </pre></code>
 * 
 * @author Stéphane Épardaud
 */
public class Page {

    /**
     * The current page index (0-based).
     */
    public final int index;

    /**
     * The current page size;
     */
    public final int size;

    /**
     * Builds a page of the given size.
     * 
     * @param size the page size
     * @throws IllegalArgumentException if the page size is less than or equal to 0
     * @see #ofSize(int)
     */
    public Page(int size) {
        this(0, size);
    }

    /**
     * Builds a page of the given index and size.
     * 
     * @param index the page index (0-based)
     * @param size the page size
     * @throws IllegalArgumentException if the page index is less than 0
     * @throws IllegalArgumentException if the page size is less than or equal to 0
     * @see #of(int, int)
     */
    public Page(int index, int size) {
        if (index < 0)
            throw new IllegalArgumentException("Page index must be >= 0 : " + index);
        if (size <= 0)
            throw new IllegalArgumentException("Page size must be > 0 : " + size);
        this.index = index;
        this.size = size;
    }

    /**
     * Builds a page of the given index and size.
     * 
     * @param index the page index (0-based)
     * @param size the page size
     * @throws IllegalArgumentException if the page index is less than 0
     * @throws IllegalArgumentException if the page size is less than or equal to 0
     */
    public static Page of(int index, int size) {
        return new Page(index, size);
    }

    /**
     * Builds a page of the given size.
     * 
     * @param size the page size
     * @throws IllegalArgumentException if the page size is less than or equal to 0
     */
    public static Page ofSize(int size) {
        return new Page(size);
    }

    /**
     * Returns a new page with the next page index and the same size.
     * 
     * @return a new page with the next page index and the same size.
     * @see #previous()
     */
    public Page next() {
        return new Page(index + 1, size);
    }

    /**
     * Returns a new page with the previous page index and the same size, or this page if it is the first page.
     * 
     * @return a new page with the next page index and the same size, or this page if it is the first page.
     * @see #next()
     */
    public Page previous() {
        return index > 0 ? new Page(index - 1, size) : this;
    }

    /**
     * Returns a new page with the first page index (0) and the same size, or this page if it is the first page.
     * 
     * @return a new page with the first page index (0) and the same size, or this page if it is the first page.
     */
    public Page first() {
        return index > 0 ? new Page(0, size) : this;
    }

    /**
     * Returns a new page at the given page index and the same size, or this page if the page index is the same.
     * 
     * @param newIndex the new page index
     * @return a new page at the given page index and the same size, or this page if the page index is the same.
     */
    public Page index(int newIndex) {
        return newIndex != index ? new Page(newIndex, size) : this;
    }
}
