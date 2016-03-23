package de.moritz.fastimageviewer.image;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.common.base.Stopwatch;
import com.google.common.eventbus.EventBus;
import com.google.common.io.CharStreams;
import com.google.common.net.MediaType;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import de.moritz.fastimageviewer.main.BufferState;
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

    private static final String INDEX_PATH = "/index";
    private static final String INDEX_FILTER_PATH = "/indexFilter";
    private static final String INDEX_INFO_PATH = "/indexInfo";

    private static final Logger LOG = LoggerFactory.getLogger(ImageServiceImageProvider.class);
    private static final int BUFFER_SIZE = 10;
    private static final int HISTORY_BUFFER_SIZE = 5;
    private static final int LOAD_TIME_OUT_SECONDS = 10;
    private Random random = new SecureRandom();
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
    private String filterPath;
    private volatile CompletableFuture<Void> bufferTask;
    private EventBus eventBus;
    private int maxIndex;

    @Inject
    private ImageServiceImageProvider(@Assisted String serviceUrl, EventBus eventBus) {
        this.eventBus = eventBus;
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
        maxIndex = getMaxIndex();
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
        return poll;
    }

    private void addToHistory(Image image) {
        historyBuffer.add(image);
        if (historyBuffer.size() > HISTORY_BUFFER_SIZE) {
            LOG.debug("revoving first image from history buffer.");
            historyBuffer.remove(0);
        }
        historyIndex = historyBuffer.size() - 1;
        postBufferState();
    }

    private Image getImageFromResource(String path) {
        baseUrl.setRawPath(path);
        try {
            LOG.debug("Loading image from " + baseUrl);
            HttpRequest request = requestFactory.buildGetRequest(baseUrl);
            setAuth(request);
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

    private void setAuth(HttpRequest request) {
        if (user != null) {
            request.getHeaders().setBasicAuthentication(user, pass);
        }
    }

    private void fillBufferAsync() {
        if (bufferTask == null || bufferTask.isDone()) {
            bufferTask = CompletableFuture.runAsync(this::fillBuffer);
        }
    }

    private void fillBuffer() {
        String path = INDEX_PATH;
        if (filterPath != null) {
            path = filterPath;
        }
        LOG.debug("Filling buffer...");
        while (buffer.size() < BUFFER_SIZE) {
            Image image = getImageFromResource(path + "/" + random.nextInt(maxIndex + 1));
            if (image != null) {
                buffer.offerFirst(image);
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
        baseUrl.setRawPath(INDEX_PATH);
        LOG.debug("Retieving max index from " + baseUrl);
        return readIntFromUrl(baseUrl);
    }

    private int readIntFromUrl(GenericUrl url) {
        int result = 0;
        LOG.debug("Trying to retrieve int from: " + url);
        try {
            HttpRequest request = requestFactory.buildGetRequest(url);
            setAuth(request);
            HttpResponse response = request.execute();
            String string = CharStreams.toString(new InputStreamReader(response.getContent()));
            try {
                result = Integer.parseInt(string);
                LOG.debug("int retieved: " + result);
            } catch (NumberFormatException e) {
                LOG.debug("Problem parsing string \"" + string + "\" as int.");
            }
        } catch (IOException e) {
            LOG.debug("Problem retrieving int: ", e);
        }
        return result;
    }

    @Override
    public void setPath(String path) {
        if (path.isEmpty()) {
            filterPath = null;
            maxIndex = getMaxIndex();
        } else {
            filterPath = path;
            maxIndex = updateMaxIndexForFilter(path);
        }
        LOG.debug("path set to " + path);
        buffer.clear();
        fillBufferAsync();
    }

    private int updateMaxIndexForFilter(String path) {
        baseUrl.setRawPath(path + INFO);
        return readIntFromUrl(baseUrl);
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

}
