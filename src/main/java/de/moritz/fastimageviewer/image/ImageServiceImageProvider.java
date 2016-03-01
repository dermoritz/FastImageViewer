package de.moritz.fastimageviewer.image;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import javafx.scene.image.Image;

import org.apache.commons.logging.Log;
import org.apache.http.Header;
import org.apache.http.annotation.ThreadSafe;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.auth.BasicScheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by moritz on 25.02.2016.
 *
 * Uses my ImageService to get images.
 *
 */
public class ImageServiceImageProvider implements ImageProvider {

    private static final String NEXT = "/next";
    private static final String PREV = "/prev";
    private static final Logger LOG = LoggerFactory.getLogger( ImageServiceImageProvider.class );
    private final HttpRequestFactory requestFactory;
    private final GenericUrl baseUrl;
    private String user;
    private String pass;
    private volatile ConcurrentLinkedDeque<Image> buffer = new ConcurrentLinkedDeque<>();
    private static final int BUFFER_SIZE = 10;
    private String currentPath;
    private volatile CompletableFuture<Void> bufferTask;

    public ImageServiceImageProvider( String serviceUrl ) {
        LOG.debug( "ImageService provider started with base url " + serviceUrl );
        requestFactory = new NetHttpTransport().createRequestFactory();
        try {
            baseUrl = new GenericUrl( new URL( serviceUrl ) );
        } catch( MalformedURLException e ) {
            throw new IllegalArgumentException( "Problem parsing url: " + serviceUrl + " cause: ", e );
        }
        String userInfo = baseUrl.getUserInfo();
        if( userInfo != null ) {
            String[] userPass = userInfo.split( ":" );
            user = userPass[0];
            pass = userPass[1];
            LOG.debug( "Credentials detected: " + user + ":" + pass );
        }
        currentPath = NEXT;
        fillBufferAsync();
    }

    @Override
    public Image prev() {
        return getImageFromResource( PREV );
    }

    @Override
    public Image getImage( int index ) {
        return getImage();
    }

    @Override
    public Image getImage() {
        fillBufferAsync();
        while (buffer.size()<1){
            LOG.debug("Buffer is empty.. waiting");
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new IllegalStateException("Problem on sleeping!?: ", e);
            }
        }
        return buffer.poll();
    }

    private Image getImageFromResource( String path ) {
        baseUrl.setRawPath( path );
        try {
            LOG.debug( "Loading image from " + baseUrl );
            HttpRequest request = requestFactory.buildGetRequest( baseUrl );
            if( user != null ) {
                request.getHeaders().setBasicAuthentication( user, pass );
            }
            HttpResponse response = request.execute();
            return new Image( response.getContent() );
        } catch( IOException e ) {
            throw new IllegalStateException( "Problem on reading from " + baseUrl + ", cause: ", e );
        }
    }

    private void fillBufferAsync() {
        if( bufferTask == null || bufferTask.isDone() ) {
            bufferTask = CompletableFuture.runAsync( this::fillBuffer );
        }
    }

    private void fillBuffer() {
        LOG.debug("Filling buffer...");
        while( buffer.size() < BUFFER_SIZE ) {
            buffer.offerFirst( getImageFromResource( currentPath ) );
            LOG.debug("Added image to buffer, " + buffer.size() + " images buffered.");
        }
        LOG.debug("buffer full");
    }

    @Override
    public int getMaxIndex() {
        return 0;
    }

    @Override
    public void setPath( String path ) {
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
