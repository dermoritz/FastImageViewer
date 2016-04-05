package de.moritz.fastimageviewer.image.imageservice;

import com.google.common.base.Preconditions;

import javafx.scene.image.Image;

/**
 * pair of image and its {@link ImageServiceImageId}.
 * @author moritz
 *
 */
public class ImageWithId {

    private ImageServiceImageId id;
    private Image image;

    public ImageWithId(Image image, ImageServiceImageId id) {
        this.image = Preconditions.checkNotNull(image);
        this.id = Preconditions.checkNotNull(id);
    }

    /**
     * @return the id
     */
    public ImageServiceImageId getId() {
        return id;
    }

    /**
     * @return the image
     */
    public Image getImage() {
        return image;
    }

}
