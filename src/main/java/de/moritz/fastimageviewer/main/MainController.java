package de.moritz.fastimageviewer.main;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Screen;
import javafx.stage.Window;

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

    @FXML
    private SplitPane root;

    @FXML
    private ScrollPane imageArea;


    @Inject
    public MainController(ImageViewer imageView) {
        this.imageView = imageView;

    }

    private void registerEvents() {
        root.setOnKeyPressed(imageView::pageKey);
        root.setOnScroll(imageView::handleScroll);
        root.heightProperty().addListener(imageView::handleResize);
        root.widthProperty().addListener(imageView::handleResize);
        root.setOnMousePressed(imageView::handleMouseDown);
        root.setOnMouseReleased((event) -> imageView.fitImage());
        root.setOnDragOver(imageView::dragOver);
        root.setOnDragDropped(imageView::dropFile);
        imageArea.addEventFilter(ScrollEvent.SCROLL, (event) -> {event.consume();});
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        imageArea.setContent(imageView.getImageView());
        registerEvents();
    }

    public void onReady(){
        imageView.fitImage();
    }


}
