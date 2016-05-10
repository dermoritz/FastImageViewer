package de.moritz.fastimageviewer.main;

import com.google.inject.Inject;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;

public class ImageViewer extends ImageView {

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
        this.setFitHeight( height );
        this.setFitWidth( width );
    }

    public void handleMouseDown( MouseEvent event ) {
        double x = event.getX();
        double y = event.getY();
        event.consume();
        zoom100( x, y );
    }

    private void zoom100( double x, double y ) {
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
