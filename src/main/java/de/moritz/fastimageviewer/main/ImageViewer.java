package de.moritz.fastimageviewer.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

public class ImageViewer extends ImageView {

    private static final Logger LOG = LoggerFactory.getLogger( ImageViewer.class );

    @Inject
    private ImageViewer() {

        this.setFocusTraversable( true );
        this.requestFocus();
        this.setPreserveRatio( true );
    }

    public void setImageAndFit( Image image ) {
        if( image != null ) {
            this.setImage( image );
            fitImage();
        }
    }

    public ImageView getImageView() {
        return this;
    }

    private double getParentHeight() {
        return ( (Pane) this.getParent() ).heightProperty().getValue();
    }

    private double getParentWidth() {
        return ( (Pane) this.getParent() ).widthProperty().getValue();
    }

    public void fitImage() {
        this.autosize();
        this.setTranslateX( 0 );
        this.setTranslateY( 0 );
        this.setScaleX( 1 );
        this.setScaleY( 1 );
        double width = getParentWidth();
        double height = getParentHeight();
        this.setPreserveRatio( true );
        // if no image or image is larger than container
        if( getImage() == null || this.getImage().getHeight() > height || this.getImage().getWidth() > width ) {
            this.setFitHeight( height );
            this.setFitWidth( width );
        } else {
            this.setFitHeight( -1 );
            this.setFitWidth( -1 );
        }

    }

    /**
     * moves image horizontally by given amount
     * @param x x movement in pixels
     */
    public void moveImageX( double x ) {
            this.setTranslateX( this.getTranslateX() - x );
    }

    /**
     * moves image vertically by given amount
     * @param y y movement in pixels
     */
    public void moveImageY( double y ) {
            this.setTranslateY( this.getTranslateY() - y );
    }

    public void zoom100( double x, double y ) {
        double oldHeight = this.getBoundsInLocal().getHeight();
        double oldWidth = this.getBoundsInLocal().getWidth();

        boolean heightLarger = oldHeight > oldWidth;
        this.setFitHeight( -1 );
        this.setFitWidth( -1 );
        // calculate scale factor
        double scale = 1;
        if( heightLarger ) {
            scale = this.getBoundsInLocal().getHeight() / oldHeight;
        } else {
            scale = this.getBoundsInLocal().getWidth() / oldWidth;
        }
        double centery = getParentHeight() / 2;
        double centerx = getParentWidth() / 2;

        double xOffset = scale * ( centerx - x );
        double yOffset = scale * ( centery - y );
        this.setTranslateX( xOffset );
        this.setTranslateY( yOffset );

    }

}
