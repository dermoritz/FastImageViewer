package de.moritz.fastimageviewer.main;

import de.moritz.fastimageviewer.image.ImageProvider;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.SplitPane;
import javafx.scene.image.Image;
import javafx.scene.input.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Created by moritz on 05.03.2016.
 */
@Singleton
public class MainController implements Initializable {


    private final ImageViewer imageView;
    private final ImageProvider ip;

    @FXML
    private SplitPane root;

    @FXML
    private StackPane imageArea;




    @Inject
    public MainController(ImageViewer imageView, ImageProvider ip) {
        this.ip = ip;
        this.imageView = imageView;

    }

    private void registerEvents() {
        root.setOnKeyPressed(this::pageKey);
        root.setOnScroll(this::handleScroll);
        root.heightProperty().addListener(this::handleResize);
        root.widthProperty().addListener(this::handleResize);
        root.setOnDragOver(this::dragOver);
        root.setOnDragDropped(this::dropFile);
        imageArea.setOnMousePressed(imageView::handleMouseDown);
        imageArea.setOnMouseReleased((event) -> imageView.fitImage());
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        imageArea.getChildren().add(imageView.getImageView());
        registerEvents();
    }

    public void onReady(){
        if(ip.hasNext()){
            imageView.setImageAndFit(ip.next());
        }
    }

    public void pageKey(KeyEvent event){
        if(event.getCode() == KeyCode.PAGE_UP){
            imageView.setImageAndFit(ip.prev());
            event.consume();
        } else if(event.getCode() == KeyCode.PAGE_DOWN){
            imageView.setImageAndFit(ip.next());
            event.consume();
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
            imageView.setImageAndFit(image);
        }
        event.setDropCompleted(success);
        event.consume();
    }

    public void handleScroll(ScrollEvent event) {
        double deltaY = event.getDeltaY();
        event.consume();
        if (deltaY < 0) {
            imageView.setImageAndFit(ip.next());
        } else {
            imageView.setImageAndFit(ip.prev());
        }
    }

    public void handleResize(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
        imageView.fitImage();
    }

}
