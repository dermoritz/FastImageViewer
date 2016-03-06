package de.moritz.fastimageviewer.main;

import de.moritz.fastimageviewer.image.ImageProvider;
import de.moritz.fastimageviewer.image.ImageProviderImpl;
import de.moritz.fastimageviewer.image.ImageServiceImageProvider;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.*;
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
    private final String[] args;

    @FXML
    private SplitPane root;

    @FXML
    private StackPane imageArea;

    @FXML
    private Button goButton;

    @FXML
    private TextField pathField;

    @FXML
    private TextField filterField;


    @Inject
    public MainController(ImageViewer imageView, @DiModule.Args String[] args) {
        this.args = args;
        this.ip = getIp(args);
        this.imageView = imageView;

    }

    private ImageProvider getIp(String[] args) {
        String startPath = args == null ? null : args[0];
        if( startPath != null && startPath.toLowerCase().startsWith( "http" ) ) {
            ImageServiceImageProvider imageService = new ImageServiceImageProvider(startPath);
            if(args.length>1){
                imageService.setPath(args[1]);
            }
            return imageService;
        } else {
            return new ImageProviderImpl( startPath );
        }
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
        if(args.length>0){
            pathField.setText(args[0]);
        }
        if(args.length>1){
            filterField.setText(args[1]);
        }
    }

    public void onReady(){
        if(ip != null && ip.hasNext()){
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
