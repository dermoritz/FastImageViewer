package de.moritz.fastimageviewer.image.imageservice;

import javax.annotation.Nullable;

/**
 * Identifies an image to be retrieved from imageService
 * @author moritz
 *
 */
public class ImageServiceImageId {
    private int index;
    private String filter;

    public ImageServiceImageId(int index, @Nullable String filter){
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
