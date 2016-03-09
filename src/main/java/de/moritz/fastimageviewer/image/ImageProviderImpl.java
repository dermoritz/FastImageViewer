package de.moritz.fastimageviewer.image;

import java.io.File;
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
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.moritz.fastimageviewer.main.BufferState;
import de.moritz.fastimageviewer.main.BufferStateCallback;
import javafx.scene.image.Image;

public class ImageProviderImpl implements ImageProvider {

    private static final int FORWARD_BUFFER_SIZE = 2;
    private static final int BACK_BUFFER_SIZE = 1;
    private Path imageFolder;
    private List<Path> imagePaths = new ArrayList<>();
    private int currentIndex = 0;
    private volatile Map<Integer, Image> imageBuffer = new HashMap<>();

    private static final Logger LOG = LoggerFactory.getLogger(ImageProviderImpl.class);
    private CompletableFuture<Integer> updateBufferTask;
    private BufferStateCallback bufferStateCallback;
    private Consumer<String> infoConsumer;

    public ImageProviderImpl(String path) {
        if (path != null) {
            setPath(path);
        }
    }

    private void getFiles() {
        List<Path> result = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(imageFolder, "*.{jpg,jpeg,png,gif,bmp}")) {
            for (Path entry : stream) {
                result.add(entry);
            }
        } catch (IOException ex) {
            // I/O error encountered during the iteration, the cause is an
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
            LOG.debug("Image not in buffer, loading it. :-(");
            image = loadImage(imagePaths.get(index));
        } else {
            LOG.debug("Image found in buffer... :-)");
        }
        // cancel previous update task
        if (updateBufferTask != null) {
            if (!updateBufferTask.isDone()) {
                updateBufferTask.cancel(true);
                LOG.debug("Canceled update");
            }
        }
        // start new update task
        updateBufferTask = CompletableFuture.supplyAsync(() -> {
            updateBuffer(index);
            return 1;
        });
        if(infoConsumer!=null){
            infoConsumer.accept(String.valueOf(image.getWidth()) + "x" + String.valueOf(image.getHeight()));
        }
        return image;

    }

    private void updateBuffer(int index) {
        LOG.debug("updateing buffer for index " + index);

        int right = index + FORWARD_BUFFER_SIZE <= getMaxIndex() ? index + FORWARD_BUFFER_SIZE : getMaxIndex();
        int left = index - BACK_BUFFER_SIZE >= 0 ? index - BACK_BUFFER_SIZE : 0;
        // remove images out of range
        Set<Integer> keys = new HashSet<>(imageBuffer.keySet());
        for (Integer i : keys) {
            if (i < left || i > right) {
                LOG.debug("Removing image " + i + " from buffer.");
                imageBuffer.remove(i);
            }
        }
        // add all that are not in buff but in range
        for (int i = left; i <= right; i++) {
            if (imageBuffer.get(i) == null) {
                imageBuffer.put(i, loadImage(imagePaths.get(i)));
                LOG.debug("image " + i + " loaded into buffer.");
            }
        }
        callBackBufferState();
    }

    private void callBackBufferState() {
        if (bufferStateCallback != null) {
            // backwar buffer
            int backBufferCount = 0;
            for (int i = 0; i < BACK_BUFFER_SIZE; i++) {
                if (imageBuffer.get(i) != null) {
                    backBufferCount++;
                }
            }
            int forwardBufferCount = 0;
            for (int i = BACK_BUFFER_SIZE; i <= FORWARD_BUFFER_SIZE; i++) {
                if (imageBuffer.get(i) != null) {
                    forwardBufferCount++;
                }
            }
            bufferStateCallback.state(new BufferState((double) forwardBufferCount / FORWARD_BUFFER_SIZE,
                                                      (double) backBufferCount / BACK_BUFFER_SIZE));
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

    @Override
    public void setPath(String path) {
        File file = new File(path);
        if (file.isDirectory()) {
            this.imageFolder = Paths.get(path);
            getFiles();
            currentIndex = 0;
        }
        // browse folder of given image starting on given image
        else {
            this.imageFolder = Paths.get(file.getParent());
            getFiles();
            // searching given file an set index accordingly.
            for (Path filePath : imagePaths) {
                if (filePath.equals(Paths.get(path))) {
                    currentIndex = imagePaths.indexOf(filePath);
                    LOG.debug("Setting index to " + currentIndex + " for image " + file.getName());
                }
            }
        }
    }

    @Override
    public Image getImage() {
        return getImage(currentIndex);
    }

    @Override
    public void setBufferChangeCallback(BufferStateCallback state) {
        this.bufferStateCallback = state;
    }

    @Override
    public void setInfoCallBack(Consumer<String> infoConsumer) {
        this.infoConsumer = infoConsumer;
    }

}
