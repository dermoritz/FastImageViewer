package de.moritz.fastimageviewer.image.imageservice;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.common.base.Strings;
import com.google.common.io.CharStreams;
import com.google.common.net.MediaType;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import javafx.scene.image.Image;

public class ImageServiceApiImpl implements ImageServiceApi {
    private static final String INDEX_PATH = "/index";
    private static final String INDEX_FILTER_PATH = "/indexFilter";
    private static final String INFO = "/info";
    private static final String INDEX_INFO_PATH = "/indexInfo";
    private static final String INDEX_FILTER_INFO = "/indexFilterInfo";
    private HttpRequestFactory requestFactory;
    private GenericUrl baseUrl;
    private String user;
    private String pass;

    private Logger LOG = LoggerFactory.getLogger(ImageServiceApiImpl.class);

    @Inject
    private ImageServiceApiImpl(@Assisted String serviceUrl) {
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
    }

    @Override
    public Image getImage(ImageServiceImageId id) {
        return getImageFromResource(getPathFromId(id));
    }

    @Override
    public String getImageInfo(ImageServiceImageId id) {
        baseUrl.setRawPath(getInfoPathFromId(id));
        return readStringFromUrl(baseUrl);
    }

    @Override
    public int maxIndex() {
        baseUrl.setRawPath(INDEX_PATH);
        LOG.debug("Retieving max index from " + baseUrl);
        return readIntFromUrl(baseUrl);
    }

    @Override
    public int maxIndexForFilter(String filter) {
        baseUrl.setRawPath("/"+ cleatFirstSlash(filter) + INFO);
        return readIntFromUrl(baseUrl);
    }

    private Image getImageFromResource(String path) {
        baseUrl.setRawPath(path);
        Image image = null;
        try {
            LOG.debug("Loading image from " + baseUrl);
            HttpRequest request = requestFactory.buildGetRequest(baseUrl);
            setAuth(request);
            HttpResponse response = request.execute();
            if (MediaType.parse(response.getContentType()).is(MediaType.ANY_IMAGE_TYPE)) {
                image = new Image(response.getContent());
            }
        } catch (IOException e) {
            LOG.debug("Can't receive image: " + e.getMessage());
        }
        return image;
    }

    private String getPathFromId(ImageServiceImageId id) {
        String path;
        if (Strings.isNullOrEmpty(id.getFilter())) {
            path = INDEX_PATH + "/" + id.getIndex();
        } else {
            String filter = cleatFirstSlash(id.getFilter());
            path = INDEX_FILTER_PATH + "/" + filter + "/" + id.getIndex();
        }
        return path;
    }

    private String getInfoPathFromId(ImageServiceImageId id) {
        String path;
        if (Strings.isNullOrEmpty(id.getFilter())) {
            path = INDEX_INFO_PATH + "/" + id.getIndex();
        } else {
            String filter = cleatFirstSlash(id.getFilter());
            path = INDEX_FILTER_INFO + "/" + filter + "/" + id.getIndex();
        }
        return path;
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

    private String readStringFromUrl(GenericUrl url) {
        LOG.debug("Trying to retrieve int from: " + url);
        String string = null;
        try {
            HttpRequest request = requestFactory.buildGetRequest(url);
            setAuth(request);
            HttpResponse response = request.execute();
            string = CharStreams.toString(new InputStreamReader(response.getContent()));
        } catch (IOException e) {
            LOG.debug("Problem retrieving int: ", e);
        }
        return string;
    }

    private void setAuth(HttpRequest request) {
        if (user != null) {
            request.getHeaders().setBasicAuthentication(user, pass);
        }
    }

    private String cleatFirstSlash(String in){
        return in.startsWith("/") ? in.replaceFirst("/", "") : in;
    }

}
