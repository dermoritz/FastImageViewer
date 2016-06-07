package de.moritz.fastimageviewer.image.imageservice;

import javax.annotation.Nullable;

/**
 * Identifies an image to be retrieved from imageService. An image is identified either by absolute index number or an index number relative to an applied filter.
 * @author moritz
 *
 */
public class ImageServiceImageId {
    private int index;
    private String filter;

    public ImageServiceImageId( int index, @Nullable String filter ) {
        this.index = index;
        this.filter = filter;
    }

    /**
     * @return the index
     */
    public int getIndex() {
        return index;
    }

    /**
     * @return the filter
     */
    public String getFilter() {
        return filter;
    }

}
