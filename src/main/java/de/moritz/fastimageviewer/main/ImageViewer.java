package de.moritz.fastimageviewer.main;

import com.google.inject.Inject;
import javafx.scene.Cursor;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageViewer extends ImageView {

    private volatile Boolean zoomedIn = false;

    private static final Logger LOG = LoggerFactory.getLogger(ImageViewer.class);
    private Double mouseStartX;
    private Double mouseStartY;

    @Inject
    private ImageViewer() {

        this.setFocusTraversable(true);
        this.requestFocus();
        this.setPreserveRatio(true);
    }

    public void setImageAndFit(Image image) {
        if (image != null) {
            this.setImage(image);
            fitImage();
        }
    }

    public ImageView getImageView() {
        return this;
    }

    private double getParentHeight() {
        return ((Pane) this.getParent()).heightProperty().getValue();
    }

    private double getParentWidth() {
        return ((Pane) this.getParent()).widthProperty().getValue();
    }

    public void fitImage() {
        this.autosize();
        this.setTranslateX(0);
        this.setTranslateY(0);
        this.setScaleX(1);
        this.setScaleY(1);
        double width = getParentWidth();
        double height = getParentHeight();
        this.setPreserveRatio(true);
        //if no image or image is larger than container
        if (getImage() == null || this.getImage().getHeight() > height || this.getImage().getWidth() > width) {
            this.setFitHeight(height);
            this.setFitWidth(width);
        } else {
            this.setFitHeight(-1);
            this.setFitWidth(-1);
        }

    }

    public void handleMouseDown(MouseEvent event) {
        LOG.debug("handle mouse down");
        getScene().setCursor(Cursor.NONE);
        double x = event.getX();
        double y = event.getY();
        event.consume();
        zoomedIn = true;
        zoom100(x, y);
        mouseStartX = null;
        mouseStartY = null;
    }

    public void handleMouseRelease(MouseEvent event) {
        getScene().setCursor(Cursor.DEFAULT);
        zoomedIn = false;
        fitImage();
    }

    public void dragOnMouseMove(MouseEvent event) {
        if (zoomedIn) {
            if (mouseStartX != null && mouseStartY != null) {
                double x = this.getTranslateX() - (mouseStartX - event.getX());
                double y = this.getTranslateY() - (mouseStartY - event.getY());
                this.setTranslateX(x);
                this.setTranslateY(y);
            }
            mouseStartX = event.getX();
            mouseStartY = event.getY();
        }
    }

    private void zoom100(double x, double y) {
        double oldHeight = this.getBoundsInLocal().getHeight();
        double oldWidth = this.getBoundsInLocal().getWidth();

        boolean heightLarger = oldHeight > oldWidth;
        this.setFitHeight(-1);
        this.setFitWidth(-1);
        // calculate scale factor
        double scale = 1;
        if (heightLarger) {
            scale = this.getBoundsInLocal().getHeight() / oldHeight;
        } else {
            scale = this.getBoundsInLocal().getWidth() / oldWidth;
        }
        double centery = getParentHeight() / 2;
        double centerx = getParentWidth() / 2;

        double xOffset = scale * (centerx - x);
        double yOffset = scale * (centery - y);
        this.setTranslateX(xOffset);
        this.setTranslateY(yOffset);

    }

}
