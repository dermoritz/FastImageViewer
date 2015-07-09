package de.moritz.fastimageviewer.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import de.moritz.fastimageviewer.image.ImageProvider;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;

public class MainLayout {

    private ImageView imageView;
    private StackPane root;
    private ImageProvider ip;
    
    private static final Logger LOG = LoggerFactory.getLogger(MainLayout.class);
    
    @Inject
    private MainLayout(ImageProvider ip) {
        this.ip = ip;

        imageView = new ImageView();
        imageView.setFocusTraversable(true);
        imageView.requestFocus();
        
        root = new StackPane();
       
        root.getChildren().add(imageView);
        
        //root.requestFocus();
        registerEvents();
        
        //load first image if there is one
        if (ip.hasNext()) {
            Image image = ip.getImage();
            imageView.setImage(image);
            fitImage();
        }
    }

    private void registerEvents() {
        root.setOnKeyPressed((event) -> {
            pageKey(event);
        });
        OnScroll onScroll = new OnScroll();
        root.setOnScroll(onScroll);
        OnResize onResize = new OnResize();
        root.heightProperty().addListener(onResize);
        root.widthProperty().addListener(onResize);
        OnMouseDown onMouseDown = new OnMouseDown();
        root.setOnMousePressed(onMouseDown);
        root.setOnMouseReleased((event) -> fitImage());
        root.setOnDragOver((event) -> dragOver(event));
        root.setOnDragDropped((event) -> dropFile(event));

        
    }
    
    private void pageKey(KeyEvent event){
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
    
    private void dragOver(DragEvent event) {
        Dragboard db = event.getDragboard();
        if (db.hasFiles()) {
            event.acceptTransferModes(TransferMode.LINK);
        } else {
            event.consume();
        }
    }

    private void dropFile(DragEvent event) {
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

    public Parent getRoot() {
        return root;
    }

    private void fitImage() {
        imageView.autosize();
        imageView.setTranslateX(0);
        imageView.setTranslateY(0);
        imageView.setScaleX(1);
        imageView.setScaleY(1);
        double width = root.getWidth();
        double height = root.getHeight();
        imageView.setPreserveRatio(true);
        imageView.setFitHeight(height);
        imageView.setFitWidth(width);

    }

    private class OnScroll implements EventHandler<ScrollEvent> {

        public OnScroll() {
        }

        @Override
        public void handle(ScrollEvent event) {
            double deltaY = event.getDeltaY();
            if (deltaY < 0) {
                imageView.setImage(ip.next());
            } else {
                imageView.setImage(ip.prev());
            }
            fitImage();
        }

    }

    private class OnResize implements ChangeListener<Number> {

        @Override
        public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
            fitImage();
        }

    }

    private class OnMouseDown implements EventHandler<MouseEvent> {

        @Override
        public void handle(MouseEvent event) {
            double x = event.getX();
            double y = event.getY();
            zoom100(x, y);
        }

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

        double centery = root.getLayoutBounds().getHeight() / 2;
        double centerx = root.getLayoutBounds().getWidth() / 2;

        double xOffset = scale * (centerx - x);
        double yOffset = scale * (centery - y);
        imageView.setTranslateX(xOffset);
        imageView.setTranslateY(yOffset);

    }

}
