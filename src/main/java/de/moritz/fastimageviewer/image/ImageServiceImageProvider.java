package de.moritz.fastimageviewer.image;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;

import javafx.scene.image.Image;

/**
 * Created by moritz on 25.02.2016.
 *
 * Uses my ImageService to get images.
 *
 */
public class ImageServiceImageProvider implements ImageProvider {

    private static final String NEXT = "/next";
    private static final Logger LOG = LoggerFactory.getLogger(ImageServiceImageProvider.class);
    private final HttpRequestFactory requestFactory;
    private final GenericUrl baseUrl;
    private String user;
    private String pass;
    private volatile ConcurrentLinkedDeque<Image> buffer = new ConcurrentLinkedDeque<>();
    private static final int BUFFER_SIZE = 10;

    private volatile List<Image> historyBuffer = new ArrayList<>();
    private static final int HISTORY_BUFFER_SIZE = 5;
    /**
     * Current position in history
     */
    private int historyIndex = 0;

    private static final String PREV = "/prev/" + BUFFER_SIZE;
    private String currentPath;
    private volatile CompletableFuture<Void> bufferTask;

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
        if(--historyIndex >=0 && historyBuffer.size()>historyIndex){
            result = historyBuffer.get(historyIndex);
        } else if (historyBuffer.size()>0) {
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
        fillBufferAsync();
        while (buffer.size() < 1) {
            LOG.debug("Buffer is empty.. waiting");
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new IllegalStateException("Problem on sleeping!?: ", e);
            }
        }
        Image poll = buffer.poll();
        historyIndex = historyBuffer.size()-1;
        historyBuffer.add(poll);
        if(historyBuffer.size()>HISTORY_BUFFER_SIZE){
            LOG.debug("revoving first image from history buffer.");
            historyBuffer.remove(0);
        }
        return poll;
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
            return new Image(response.getContent());
        } catch (IOException e) {
            throw new IllegalStateException("Problem on reading from " + baseUrl + ", cause: ", e);
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
            buffer.offerFirst(getImageFromResource(currentPath));
            LOG.debug("Added image to buffer, " + buffer.size() + " images buffered.");
        }
        LOG.debug("buffer full");
    }

    @Override
    public int getMaxIndex() {
        return 0;
    }

    @Override
    public void setPath(String path) {
        currentPath = path;
    }

    @Override
    public boolean hasNext() {
        return true;
    }

    @Override
    public Image next() {
        return getImage();
    }
}
