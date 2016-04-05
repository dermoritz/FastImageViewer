package de.moritz.fastimageviewer.image.imageservice;

import javafx.scene.image.Image;

/**
 * Specifies the part of imageservice rest api used here.
 * @author moritz
 *
 */
public interface ImageServiceApi {

    /**
     *
     * @param id id of image
     * @return the image or null if no image is returned by service
     */
    Image getImage(ImageServiceImageId id);

    /**
     *
     * @param id id of image
     * @return the info for given image id or null if no info is returned by service
     */
    String getImageInfo(ImageServiceImageId id);

    /**
     * Indexes to be used range from 0 to this
     * @return max index to be used
     */
    int maxIndex();

    /**
     * Indexes to be used for given filter range from 0 to this.
     * @param filter
     * @return max index for this filter (means there are maxIndex+1 images matching the filter)
     */
    int maxIndexForFilter(String filter);
}
