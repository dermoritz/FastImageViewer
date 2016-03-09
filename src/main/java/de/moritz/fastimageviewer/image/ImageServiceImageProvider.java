package de.moritz.fastimageviewer.image;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpMediaType;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.common.base.Stopwatch;
import com.google.common.net.MediaType;

import de.moritz.fastimageviewer.main.BufferState;
import de.moritz.fastimageviewer.main.BufferStateCallback;
import javafx.scene.image.Image;

/**
 * Created by moritz on 25.02.2016.
 *
 * Uses my ImageService to get images.
 *
 */
public class ImageServiceImageProvider implements ImageProvider {

    private static final String INFO = "/info";
    private static final String NEXT = "/next";
    private static final Logger LOG = LoggerFactory.getLogger(ImageServiceImageProvider.class);
    private static final int BUFFER_SIZE = 10;
    private static final int HISTORY_BUFFER_SIZE = 5;
    private static final int LOAD_TIME_OUT_SECONDS = 10;

    private final HttpRequestFactory requestFactory;
    private final GenericUrl baseUrl;
    private String user;
    private String pass;

    private volatile ConcurrentLinkedDeque<Image> buffer = new ConcurrentLinkedDeque<>();
    private volatile List<Image> historyBuffer = new ArrayList<>();
    private volatile boolean noImageFound = false;

    /**
     * Current position in history
     */
    private int historyIndex = 0;

    private static final String PREV = "/prev/" + BUFFER_SIZE;
    private String currentPath;
    private volatile CompletableFuture<Void> bufferTask;
    private BufferStateCallback bufferStateCallback;
    private Consumer<String> infoConsumer;

    public ImageServiceImageProvider(String serviceUrl) {
        LOG.debug("ImageService provider started with base url " + serviceUrl);
        requestFactory = new NetHttpTransport().createRequestFactory();
        try {
            baseUrl = new GenericUrl(new URL(serviceUrl));
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Problem parsing url: " + serviceUrl + " cause: ", e);
        }
        String userInfo = baseUrl.getUserInfo();
        if (userInfo != null) {
            String[] userPass = userInfo.split(":");
            user = userPass[0];
            pass = userPass[1];
            LOG.debug("Credentials detected: " + user + ":" + pass);
        }
        currentPath = NEXT;
        fillBufferAsync();
    }

    @Override
    public Image prev() {
        Image result = null;
        if (--historyIndex >= 0) {
            result = historyBuffer.get(historyIndex);
        } else if (historyBuffer.size() > 0) {
            LOG.debug("reached end of history.");
            result = historyBuffer.get(0);
        }
        return result;
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
        Image poll = buffer.poll();
        addToHistory(poll);
        if (infoConsumer != null && poll != null) {
            infoConsumer.accept(getTextFromResource(INFO));
        }
        return poll;
    }

    private void addToHistory(Image image) {
        historyBuffer.add(image);
        if (historyBuffer.size() > HISTORY_BUFFER_SIZE) {
            LOG.debug("revoving first image from history buffer.");
            historyBuffer.remove(0);
        }
        historyIndex = historyBuffer.size() - 1;
        callBackBufferState();
    }

    private Image getImageFromResource(String path) {
        baseUrl.setRawPath(path);
        try {
            LOG.debug("Loading image from " + baseUrl);
            HttpRequest request = requestFactory.buildGetRequest(baseUrl);
            if (user != null) {
                request.getHeaders().setBasicAuthentication(user, pass);
            }
            HttpResponse response = request.execute();
            if (MediaType.parse(response.getContentType()).is(MediaType.ANY_IMAGE_TYPE)) {
                return new Image(response.getContent());
            }
            return null;
        } catch (IOException e) {
            LOG.debug("Can't receive image: " + e.getMessage());
            return null;
        }
    }

    private String getTextFromResource(String path) {
        baseUrl.setRawPath(path);
        Scanner s = null;
        try {
            LOG.debug("Loading image from " + baseUrl);
            HttpRequest request = requestFactory.buildGetRequest(baseUrl);
            if (user != null) {
                request.getHeaders().setBasicAuthentication(user, pass);
            }
            HttpResponse response = request.execute();
            if (MediaType.parse(response.getContentType()).is(MediaType.ANY_TEXT_TYPE)) {
                s = new Scanner(response.getContent());
                s.useDelimiter("\\A");
                String out = s.hasNext() ? s.next() : "";
                s.close();
                return out;
            }
            return null;
        } catch (IOException e) {
            LOG.debug("Can't receive image: " + e.getMessage());
            return null;
        } finally {
            if (s != null) {
                s.close();
            }
        }
    }

    private void fillBufferAsync() {
        if (bufferTask == null || bufferTask.isDone()) {
            bufferTask = CompletableFuture.runAsync(this::fillBuffer);
        }
    }

    private void fillBuffer() {
        LOG.debug("Filling buffer...");
        while (buffer.size() < BUFFER_SIZE) {
            Image image = getImageFromResource(currentPath);
            if (image != null) {
                buffer.offerFirst(image);
                LOG.debug("Added image to buffer, " + buffer.size() + " images buffered.");
                callBackBufferState();
            } else {
                noImageFound = true;
                LOG.debug("no image found, change url...");
                return;
            }
        }
        LOG.debug("buffer full");
    }

    private void callBackBufferState() {
        if (bufferStateCallback != null) {
            bufferStateCallback.state(new BufferState((double) buffer.size() / BUFFER_SIZE,
                                                      (double) historyBuffer.size() / HISTORY_BUFFER_SIZE));
        }
    }

    @Override
    public int getMaxIndex() {
        return 0;
    }

    @Override
    public void setPath(String path) {
        currentPath = path;
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

    @Override
    public void setBufferChangeCallback(BufferStateCallback state) {
        this.bufferStateCallback = state;
    }

    @Override
    public void setInfoCallBack(Consumer<String> infoConsumer) {
        this.infoConsumer = infoConsumer;
    }
}
