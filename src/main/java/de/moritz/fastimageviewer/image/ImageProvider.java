package de.moritz.fastimageviewer.image;

import java.util.Iterator;
import java.util.function.Consumer;

import de.moritz.fastimageviewer.main.BufferStateCallback;
import javafx.scene.image.Image;

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
	 *
	 * @return max index
	 */
	int getMaxIndex();

	/**
	 * Sets source path for images.
	 *
	 * @param path
	 *            folder or path to file, in case of file all files in folder
	 *            will be available
	 */
	void setPath(String path);

	/**
	 * Sets call back to report buffer state.
	 * @param state callback
	 */
	void setBufferChangeCallback(BufferStateCallback state);


	void setInfoCallBack(Consumer<String> infoConsumer);
}
