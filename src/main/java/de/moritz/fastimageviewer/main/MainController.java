package de.moritz.fastimageviewer.main;

import com.google.common.base.Strings;
import com.google.common.eventbus.Subscribe;
import de.moritz.fastimageviewer.image.ImageProvider;
import de.moritz.fastimageviewer.image.file.FileImageProvider;
import de.moritz.fastimageviewer.image.file.FileImageProvider.Inst;
import de.moritz.fastimageviewer.image.imageservice.ImageServiceImageProvider;
import de.moritz.fastimageviewer.main.DiModule.Args;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.*;
import javafx.scene.layout.AnchorPane;
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
    private ImageProvider ip;
    private final String[] args;

    @FXML
    private AnchorPane root;

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

    @FXML
    private Button infoButton;

    @FXML
    private TextField infoField;

    @FXML
    private CheckBox sortCheckBox;


    private String startPath;
    private String subPath;
    private boolean webserviceMode;
    private Inst fileImageProviderFactory;
    private de.moritz.fastimageviewer.image.imageservice.ImageServiceImageProvider.Inst serviceImageProviderFactory;

    @Inject
    public MainController(ImageViewer imageView, @Args String[] args, FileImageProvider.Inst fileImageProviderFactory,
                          ImageServiceImageProvider.Inst serviceImageProviderFactory) {
        this.args = args;
        this.fileImageProviderFactory = fileImageProviderFactory;
        this.serviceImageProviderFactory = serviceImageProviderFactory;
        this.ip = getIp(args);
        this.imageView = imageView;
    }

    @Subscribe
    public void updateBuffer(BufferState state) {
        bufferBar.setProgress(state.getForward());
    }

    @Subscribe
    public void setImageWaitedFor(Image image) {
        imageView.setImageAndFit(image);
    }

    private ImageProvider getIp(String[] args) {
        startPath = args == null || args.length < 1 ? null : args[0];
        subPath = null;
        ImageProvider ip;
        if (startPath != null && startPath.toLowerCase().startsWith("http")) {
            webserviceMode = true;
            ip = serviceImageProviderFactory.get(startPath);
            if (args.length > 1 && !Strings.isNullOrEmpty(args[1])) {
                subPath = args[1];
                ip.setPath(subPath);
            }
        } else {
            webserviceMode = false;
            ip = fileImageProviderFactory.get(startPath);
        }
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
        imageArea.setOnMouseReleased(imageView::handleMouseRelease);
        imageArea.setOnMouseDragged(imageView::dragOnMouseMove);
        goButton.setOnAction(this::handlePathChanged);
        infoButton.setOnAction(this::onInfoButton);
        sortCheckBox.selectedProperty().addListener(this::sortChanged);
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

    private void onInfoButton(ActionEvent event){
        infoField.setText(ip.getInfoForLast());
    }

    private void handlePathChanged(ActionEvent event) {
        String newFilter = filterField.getText();
        if (!Strings.isNullOrEmpty(newFilter) && !newFilter.startsWith("/")) {
            newFilter = "/" + newFilter;
        }
        if (!pathField.getText().equals(startPath)) {
            ip = getIp(new String[] {pathField.getText(), newFilter});
            imageView.setImageAndFit(ip.next());
        } else if (!newFilter.equals(subPath) && webserviceMode) {
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
        } else if (event.getCode() == KeyCode.ESCAPE){
            Platform.exit();
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
            imageView.setImageAndFit(image);
            success = true;
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

    private void sortChanged(ObservableValue<? extends Boolean> selected, Boolean oldV, Boolean newV){
        ip.setSort(newV);
    }


}
