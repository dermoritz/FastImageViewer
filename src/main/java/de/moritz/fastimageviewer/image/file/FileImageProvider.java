package de.moritz.fastimageviewer.image.file;

import static com.google.common.base.Preconditions.*;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import de.moritz.fastimageviewer.image.ImageProvider;
import javafx.scene.image.Image;

public class FileImageProvider implements ImageProvider {

    private static final int FORWARD_BUFFER_SIZE = 2;
    private static final int BACK_BUFFER_SIZE = 1;
    private static final PathMatcher IMAGE_FILE_PATTERN = FileSystems.getDefault().getPathMatcher( "glob:*.{jpg,jpeg,png,gif,bmp}" );
    private Path imageFolder;
    private List<Path> imagePaths = new ArrayList<>();
    private int currentIndex = 0;

    private static final Logger LOG = LoggerFactory.getLogger(FileImageProvider.class);
    private ImageBuffer imageBuffer;
    private ImageBuffer.Inst imageBufferFactory;

    @Inject
    private FileImageProvider(@Nullable @Assisted String path, ImageBuffer.Inst imageBufferFactory) {
        this.imageBufferFactory = imageBufferFactory;
        if (path != null) {
            setPath(path);
        }
    }

    private void getFiles() {
        List<Path> result = new ArrayList<>();
        try {
            Files.walk(imageFolder).forEach( (Path p) -> {
                if( IMAGE_FILE_PATTERN.matches( p.getFileName() )) {
                    result.add( p );
                }
            } );
        } catch( IOException e ) {
            throw new IllegalStateException("Problem on parsing folder: ", e);
        }
        imagePaths = result;
    }

    @Override
    public Image getImage(int index) {
        if (index < 0 || index > getMaxIndex()) {
            throw new IllegalArgumentException("Index must be between 0  and " + getMaxIndex());
        }
        imageBuffer.startUpdate(index);
        // check buffer
        Image image = imageBuffer.get(index);
        if (image == null) {
            LOG.debug("Image not in buffer, loading it. :-(");
            image = loadImage(index);
        } else {
            LOG.debug("Image found in buffer... :-)");
        }
        return image;
    }

    private Image loadImage(Integer index) {
        Path path = imagePaths.get(checkNotNull(index));
        String imageUrl;
        try {
            imageUrl = path.toUri().toURL().toString();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Problem with file path: ", e);
        }
        return new Image(imageUrl);
    }

    private int getMaxIndex() {
        return imagePaths.size() - 1;
    }

    @Override
    public boolean hasNext() {
        return getMaxIndex() > 0 && currentIndex < getMaxIndex();
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
            imagePaths.stream().filter(filePath -> filePath.equals(Paths.get(path))).forEach(filePath -> {
                currentIndex = imagePaths.indexOf(filePath);
                LOG.debug("Setting index to " + currentIndex + " for image " + file.getName());
            });
        }
        if (imagePaths.size() > 0) {
            imageBuffer = imageBufferFactory.get(BACK_BUFFER_SIZE, FORWARD_BUFFER_SIZE, getMaxIndex(), this::loadImage);
        }
    }

    @Override
    public Image getImage() {
        return getImage(currentIndex);
    }

    public interface Inst {
        FileImageProvider get(@Assisted String path);
    }

    @Override
    public String getInfoForLast() {
        return imagePaths.get( currentIndex ).toString() + " (" + (currentIndex + 1) +"/" + imagePaths.size() + ")";
    }

    @Override
    public void setSort(boolean sorted) {
        //noop
    }

}
