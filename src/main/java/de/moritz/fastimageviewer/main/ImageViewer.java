package de.moritz.fastimageviewer.main;

import com.google.inject.Inject;
import de.moritz.fastimageviewer.image.ImageProvider;
import javafx.beans.value.ObservableValue;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageViewer {

    private ImageView imageView;
    private ImageProvider ip;
    
    private static final Logger LOG = LoggerFactory.getLogger(ImageViewer.class);
    
    @Inject
    private ImageViewer(ImageProvider ip) {
        this.ip = ip;

        imageView = new ImageView();
        imageView.setFocusTraversable(true);
        imageView.requestFocus();
        
        //load first image if there is one
        if (ip.hasNext()) {
            Image image = ip.getImage();
            imageView.setImage(image);
            //fitImage();
        }
    }


    public void pageKey(KeyEvent event){
        if(event.getCode() == KeyCode.PAGE_UP){
            imageView.setImage(ip.prev());
            event.consume();
        } else if(event.getCode() == KeyCode.PAGE_DOWN){
            imageView.setImage(ip.next());
            event.consume();
        }
        if(event.isConsumed()){
            fitImage();
        }
    }
    
    public void dragOver(DragEvent event) {
        Dragboard db = event.getDragboard();
        if (db.hasFiles()) {
            event.acceptTransferModes(TransferMode.LINK);
        } else {
            event.consume();
        }
    }

    public void dropFile(DragEvent event) {
        Dragboard db = event.getDragboard();
        boolean success = false;
        if (db.hasFiles() && db.getFiles().size() > 0) {
            ip.setPath(db.getFiles().get(0).toString());
            Image image = ip.getImage();
            imageView.setImage(image);
            fitImage();
        }
        event.setDropCompleted(success);
        event.consume();
    }

    public ImageView getImageView(){
        return imageView;
    }

    public void fitImage() {
        imageView.autosize();
        imageView.setTranslateX(0);
        imageView.setTranslateY(0);
        imageView.setScaleX(1);
        imageView.setScaleY(1);
        double width = imageView.getParent().getLayoutX();
        double height = imageView.getParent().getLayoutY();
        imageView.setPreserveRatio(true);
        imageView.setFitHeight(height);
        imageView.setFitWidth(width);

    }

    public void handleScroll(ScrollEvent event) {
        double deltaY = event.getDeltaY();
        event.consume();
        if (deltaY < 0) {
            imageView.setImage(ip.next());
        } else {
            imageView.setImage(ip.prev());
        }
        fitImage();
    }

    public void handleResize(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
        fitImage();
    }

    public void handleMouseDown(MouseEvent event) {
        double x = event.getX();
        double y = event.getY();
        event.consume();
        zoom100(x, y);
    }

    private void zoom100(double x, double y) {
        double oldHeight = imageView.getBoundsInLocal().getHeight();
        double oldWidth = imageView.getBoundsInLocal().getWidth();

        boolean heightLarger = oldHeight > oldWidth;
        imageView.setFitHeight(-1);
        imageView.setFitWidth(-1);
        // calculate scale factor
        double scale = 1;
        if (heightLarger) {
            scale = imageView.getBoundsInLocal().getHeight() / oldHeight;
        } else {
            scale = imageView.getBoundsInLocal().getWidth() / oldWidth;
        }
        double centery = imageView.getParent().getLayoutY() / 2;
        double centerx = imageView.getParent().getLayoutX()/ 2;

        double xOffset = scale * (centerx - x);
        double yOffset = scale * (centery - y);
        imageView.setTranslateX(xOffset);
        imageView.setTranslateY(yOffset);

    }

}
