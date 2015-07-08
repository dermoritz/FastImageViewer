package de.moritz.fastimageviewer.image;

import java.awt.RenderingHints;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javafx.scene.image.Image;

public class ImageProviderImpl implements ImageProvider {

    private Path imageFolder;
    private List<Path> imagePaths;
    private int currentIndex = 0;
    private Map<Integer, Image> imageBuffer = new HashMap<>();

    public ImageProviderImpl(String path) {
        this.imageFolder = Paths.get(path);
        getFiles();
    }

    private void getFiles() {
        List<Path> result = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(imageFolder, "*.{jpg,jpeg,png,gif,bmp}")) {
            for (Path entry : stream) {
                result.add(entry);
            }
        } catch (IOException ex) {
            // I/O error encounted during the iteration, the cause is an
            // IOException
            throw new IllegalStateException("Problem on parsing folder: ", ex);
        }
        imagePaths = result;
    }

    @Override
    public Image getImage(int index) {
        if (index < 0 || index > getMaxIndex()) {
            throw new IllegalArgumentException("Index must be between 0  and " + getMaxIndex());
        }
        // check buffer
        Image image = imageBuffer.get(index);
        if (image == null) {
            image = loadImage(imagePaths.get(index));
        }
        updateBuffer(index);
        return image;

    }

    private void updateBuffer(int index) {
        int bufferPrevRange = 1;
        int bufferNextRange = 2;

        int right = index + bufferNextRange <= getMaxIndex() ? index + bufferNextRange : getMaxIndex();
        int left = index - bufferPrevRange >= 0 ? index - bufferPrevRange : 0;
        // remove images out of range
        Set<Integer> keys = new HashSet<>(imageBuffer.keySet());
        for (Integer i : keys) {
            if (i < left || i > right) {
                imageBuffer.remove(i);
            }
        }
        // add all that are not in buff but in range
        for (int i = left; i <= right; i++) {
            if(imageBuffer.get(i)==null){
                imageBuffer.put(i, loadImage(imagePaths.get(i)));
            }
        }
    }

    private Image loadImage(Path path) {
        String imageUrl;
        try {
            imageUrl = path.toUri().toURL().toString();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Problem with file path: ", e);
        }
        return new Image(imageUrl);
    }

    @Override
    public int getMaxIndex() {
        return imagePaths.size() - 1;
    }

    @Override
    public boolean hasNext() {
        if (getMaxIndex() > 0 && currentIndex < getMaxIndex()) {
            return true;
        }
        return false;
    }

    @Override
    public Image next() {
        if (hasNext()) {
            currentIndex++;
        }
        return getImage(currentIndex);
    }

    @Override
    public Image prev() {
        if (currentIndex > 0) {
            currentIndex--;
        }
        return getImage(currentIndex);
    }

}
