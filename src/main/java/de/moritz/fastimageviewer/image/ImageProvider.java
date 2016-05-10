package de.moritz.fastimageviewer.image;

import javafx.scene.image.Image;

import java.util.Iterator;

public interface ImageProvider extends Iterator<Image> {
	/**
	 *
	 * @return previous image
	 */
	Image prev();

	/**
	 *
	 * @param index
	 *            between 0 and maxindex
	 * @return image with given index
	 */
	Image getImage(int index);

	/**
	 *
	 * @return image given by user or first for current folder and sorting
	 */
	Image getImage();

	/**
	 * Sets source path for images.
	 *
	 * @param path
	 *            folder or path to file, in case of file all files in folder
	 *            will be available
	 */
	void setPath(String path);

	/**
	 *
	 * @return info for last image delivered.
	 */
	String getInfoForLast();

	/**
	 * If set to true images will by put out sorted (most likely name). False will return images in random order.
	 * The default is specified by implementing class.
	 *
	 * @param sorted true:sorted, false: random image out put
	 */
	void setSort(boolean sorted);
}
