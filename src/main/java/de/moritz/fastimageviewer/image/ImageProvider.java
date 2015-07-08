package de.moritz.fastimageviewer.image;

import java.util.Iterator;

import javafx.scene.image.Image;

public interface ImageProvider extends Iterator<Image> {
    /**
     * 
     * @return previous image
     */
    Image prev();
    
    /**
     * 
     * @param index between 0 and maxindex
     * @return image with given index
     */
    Image getImage(int index);
    
    /**
     * 
     * @return max index
     */
    int getMaxIndex();
}
