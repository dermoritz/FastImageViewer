package de.moritz.fastimageviewer.image.file;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import de.moritz.fastimageviewer.main.BufferState;
import javafx.scene.image.Image;

public class ImageBuffer {

    private int forward;
    private int backward;
    private volatile Map<Integer, Image> imageBuffer = new HashMap<>();
    private Integer maxIndex;
    private static final Logger LOG = LoggerFactory.getLogger(ImageBuffer.class);
    private Function<Integer, Image> loadImage;
    private CompletableFuture<Void> updateTask;
    private volatile EventBus eventBus;

    @Inject
    private ImageBuffer(@Assisted("backward") Integer backward, @Assisted("forward") Integer forward,
                        @Assisted("maxIndex") Integer maxIndex, @Assisted Function<Integer, Image> loadImage,
                        EventBus eventBus) {
        this.eventBus = eventBus;
        this.loadImage = checkNotNull(loadImage);
        checkArgument(backward >= 0 && forward >= 0 && maxIndex >= 0);
        this.backward = backward;
        this.forward = forward;
        this.maxIndex = maxIndex;
    }

    public void startUpdate(int index) {
        // cancel previous update task
        if (updateTask != null) {
            if (!updateTask.isDone()) {
                updateTask.cancel(true);
                LOG.debug("Canceled update");
            }
        }
        updateTask = CompletableFuture.runAsync(() -> updateBuffer(index));
    }

    public Image get(int index) {
        return imageBuffer.get(index);
    }

    private void updateBuffer(int index) {
        LOG.debug("updateing buffer for index " + index);

        int right = index + forward <= maxIndex ? index + forward : maxIndex;
        int left = index - backward >= 0 ? index - backward : 0;
        // remove images out of range
        Set<Integer> keys = new HashSet<>(imageBuffer.keySet());
        for (Integer i : keys) {
            if (i < left || i > right) {
                LOG.debug("Removing image " + i + " from buffer.");
                imageBuffer.remove(i);
                //updateBufferState(index);
            }
        }
        // add all that are not in buff but in range
        for (int i = left; i <= right; i++) {
            if (imageBuffer.get(i) == null) {
                imageBuffer.put(i, loadImage.apply(i));
                LOG.debug("image " + i + " loaded into buffer.");
                updateBufferState(index);
            }
        }
    }

    private void updateBufferState(int index) {
        // backward buffer
        int backBufferCount = 0;
        for (int i = index-1; i > index - backward; i--) {
            if (imageBuffer.get(i) != null) {
                backBufferCount++;
            }
        }
        int forwardBufferCount = 0;
        for (int i = index; i < index + forward; i++) {
            if (imageBuffer.get(i) != null) {
                forwardBufferCount++;
            }
        }
        eventBus.post(new BufferState((double) forwardBufferCount / forward,
                                      (double) backBufferCount / backward));
    }

    public interface Inst {
        /**
         *
         * @param backward
         *            count of images that kept in buffer after viewed
         * @param forward
         *            count of images buffer
         * @return configured {@link ImageBuffer} instance
         */
        ImageBuffer get(@Assisted("backward") Integer backward, @Assisted("forward") Integer forward,
                        @Assisted("maxIndex") Integer maxIndex, @Assisted Function<Integer, Image> loadImage);
    }

}
