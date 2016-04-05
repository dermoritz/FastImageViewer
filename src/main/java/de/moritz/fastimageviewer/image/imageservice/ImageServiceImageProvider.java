package de.moritz.fastimageviewer.image.imageservice;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;
import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import de.moritz.fastimageviewer.image.ImageProvider;
import de.moritz.fastimageviewer.main.BufferState;
import javafx.scene.image.Image;

/**
 * Created by moritz on 25.02.2016.
 *
 * Uses my ImageService to get images.
 *
 */
public class ImageServiceImageProvider implements ImageProvider {

    private static final Logger LOG = LoggerFactory.getLogger(ImageServiceImageProvider.class);
    private static final int BUFFER_SIZE = 10;
    private static final int HISTORY_BUFFER_SIZE = 5;
    private static final int LOAD_TIME_OUT_SECONDS = 10;
    private Random random = new SecureRandom();

    private volatile ConcurrentLinkedDeque<ImageWithId> buffer = new ConcurrentLinkedDeque<>();
    private volatile List<ImageWithId> historyBuffer = new ArrayList<>();
    private volatile ImageServiceImageId currentImage;
    private volatile boolean noImageFound = false;

    /**
     * Current position in history
     */
    private int historyIndex = 0;

    private String filterPath;
    private volatile CompletableFuture<Void> bufferTask;
    private EventBus eventBus;
    private int maxIndex;

    private ImageServiceApi imageService;

    @Inject
    private ImageServiceImageProvider(@Assisted String serviceUrl, EventBus eventBus, ImageServiceApiFactory imageService) {
        this.eventBus = eventBus;
        this.imageService = imageService.get(serviceUrl);
        LOG.debug("ImageService provider started with base url " + serviceUrl);
        maxIndex = getMaxIndex();
        fillBufferAsync();
    }

    @Override
    public Image prev() {
        ImageWithId result = null;
        if (--historyIndex >= 0) {
            result = historyBuffer.get(historyIndex);
        } else if (historyBuffer.size() > 0) {
            LOG.debug("reached end of history.");
            result = historyBuffer.get(0);
        }
        if(result!=null){
            currentImage = result.getId();
            return result.getImage();
        }else {
            return null;
        }
    }

    @Override
    public Image getImage(int index) {
        return getImage();
    }

    @Override
    public Image getImage() {
        Stopwatch createStarted = Stopwatch.createStarted();
        noImageFound = false;
        fillBufferAsync();
        while (buffer.size() < 1 && !noImageFound) {
            if (createStarted.elapsed(TimeUnit.SECONDS) <= LOAD_TIME_OUT_SECONDS) {
                LOG.debug("Buffer is empty.. waiting");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new IllegalStateException("Problem on sleeping!?: ", e);
                }
            } else {
                LOG.debug("Timeout reached for try again...");
                return null;
            }
        }
        ImageWithId poll = buffer.poll();
        addToHistory(poll);
        currentImage = poll.getId();
        return poll.getImage();
    }

    private void addToHistory(ImageWithId image) {
        historyBuffer.add(image);
        if (historyBuffer.size() > HISTORY_BUFFER_SIZE) {
            LOG.debug("revoving first image from history buffer.");
            historyBuffer.remove(0);
        }
        historyIndex = historyBuffer.size() - 1;
        postBufferState();
    }

    private void fillBufferAsync() {
        if (bufferTask == null || bufferTask.isDone()) {
            bufferTask = CompletableFuture.runAsync(this::fillBuffer);
        }
    }

    private void fillBuffer() {
        LOG.debug("Filling buffer...");
        while (buffer.size() < BUFFER_SIZE) {
            ImageServiceImageId id = new ImageServiceImageId(random.nextInt(maxIndex+1), filterPath);
            Image image = imageService.getImage(id);
            if (image != null) {
                buffer.offerFirst(new ImageWithId(image, id));
                LOG.debug("Added image to buffer, " + buffer.size() + " images buffered.");
                postBufferState();
            } else {
                noImageFound = true;
                LOG.debug("no image found, change url...");
                return;
            }
        }
        LOG.debug("buffer full");
    }

    @Override
    public int getMaxIndex() {
        return imageService.maxIndex();
    }

    @Override
    public void setPath(String path) {
        if (path.isEmpty()) {
            filterPath = null;
            maxIndex = getMaxIndex();
        } else {
            filterPath = path;
            maxIndex = imageService.maxIndexForFilter(path);
        }
        LOG.debug("path set to " + path);
        buffer.clear();
        fillBufferAsync();
    }

    @Override
    public boolean hasNext() {
        return true;
    }

    @Override
    public Image next() {
        return getImage();
    }

    private void postBufferState() {
        eventBus.post(new BufferState((double) buffer.size() / BUFFER_SIZE,
                                      (double) historyBuffer.size() / HISTORY_BUFFER_SIZE));
    }

    public interface Inst {
        ImageServiceImageProvider get(@Assisted String serviceUrl);
    }

    @Override
    public String getInfoForLast() {
        return imageService.getImageInfo(currentImage);
    }

}
