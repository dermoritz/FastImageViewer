package de.moritz.fastimageviewer.main;

import de.moritz.fastimageviewer.image.ImageProvider;
import de.moritz.fastimageviewer.image.ImageProviderImpl;
import de.moritz.fastimageviewer.image.ImageServiceImageProvider;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.api.client.repackaged.com.google.common.base.Strings;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Created by moritz on 05.03.2016.
 */
@Singleton
public class MainController implements Initializable {

    private final ImageViewer imageView;
    private ImageProvider ip;
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

    @FXML
    private volatile ProgressBar bufferBar;
    private String startPath;
    private String subPath;
    private boolean webserviceMode;

    @Inject
    public MainController(ImageViewer imageView, @DiModule.Args String[] args) {
        this.args = args;
        this.ip = getIp(args);
        this.imageView = imageView;
    }

    private void updateBuffer(BufferState state) {
        bufferBar.setProgress(state.getForward());
    }

    private ImageProvider getIp(String[] args) {
        startPath = args == null ? null : args[0];
        subPath = null;
        ImageProvider ip = null;
        if (startPath != null && startPath.toLowerCase().startsWith("http")) {
            webserviceMode = true;
            ip = new ImageServiceImageProvider(startPath);
            if (args.length > 1 && !Strings.isNullOrEmpty(args[1])) {
                subPath = args[1];
                ip.setPath(subPath);
            }
        } else {
            webserviceMode = false;
            ip = new ImageProviderImpl(startPath);
        }
        ip.setBufferChangeCallback(this::updateBuffer);
        ip.setInfoCallBack(this::setTitle);
        return ip;
    }

    private void registerEvents() {
        root.addEventFilter(KeyEvent.KEY_PRESSED, this::pageKey);
        root.setOnScroll(this::handleScroll);
        root.heightProperty().addListener(this::handleResize);
        root.widthProperty().addListener(this::handleResize);
        root.setOnDragOver(this::dragOver);
        root.setOnDragDropped(this::dropFile);
        imageArea.setOnMousePressed(imageView::handleMouseDown);
        imageArea.setOnMouseReleased((event) -> imageView.fitImage());
        goButton.setOnAction(this::handlePathChanged);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        imageArea.getChildren().add(imageView.getImageView());
        registerEvents();
        if (args.length > 0) {
            pathField.setText(args[0]);
        }
        if (args.length > 1) {
            filterField.setText(args[1].replaceFirst("/", ""));
        }
    }

    public void onReady() {
        if (ip != null && ip.hasNext()) {
            imageView.setImageAndFit(ip.next());
        }
    }

    private void handlePathChanged(ActionEvent event) {
        String newFilter = filterField.getText();
        if(!Strings.isNullOrEmpty(newFilter) && !newFilter.startsWith("/")){
            newFilter = "/"+newFilter;
        }
        if (!pathField.getText().equals(startPath)) {
            ip=getIp(new String[]{pathField.getText(),newFilter});
            imageView.setImageAndFit(ip.next());
        } else if(!newFilter.equals(subPath) && webserviceMode){
            ip.setPath(newFilter);
            imageView.setImageAndFit(ip.next());
        }
    }

    private void pageKey(KeyEvent event) {
        if (event.getCode() == KeyCode.PAGE_UP) {
            imageView.setImageAndFit(ip.prev());
            event.consume();
        } else if (event.getCode() == KeyCode.PAGE_DOWN) {
            imageView.setImageAndFit(ip.next());
            event.consume();
        }
    }

    private void setTitle(String title){
        ((Stage)root.getScene().getWindow()).setTitle(title);
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
            imageView.setImageAndFit(image);
        }
        event.setDropCompleted(success);
        event.consume();
    }

    private void handleScroll(ScrollEvent event) {
        double deltaY = event.getDeltaY();
        event.consume();
        if (deltaY < 0) {
            imageView.setImageAndFit(ip.next());
        } else {
            imageView.setImageAndFit(ip.prev());
        }
    }

    private void handleResize(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
        imageView.fitImage();
    }

}
