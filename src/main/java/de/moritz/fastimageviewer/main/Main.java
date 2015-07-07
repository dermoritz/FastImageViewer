package de.moritz.fastimageviewer.main;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class Main extends Application {

    private List<Path> imageList;
    private ImageView imageView;
    private StackPane root;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        
        Screen screen = Screen.getPrimary();
        Rectangle2D bounds = screen.getVisualBounds();
        primaryStage.setX(0);
        primaryStage.setY(0);
        primaryStage.setWidth(bounds.getWidth());
        primaryStage.setHeight(bounds.getHeight());
        
        String imgPath = "C:/Users/moritz/Downloads/1/Heidi18Years_2014-02-22_61_10000";
        File file = new File(imgPath);
        imageList = getFiles(file.toPath());

        if (imageList != null && imageList.size() > 0) {
            final int index = 0;

            Image image = getImage(index);
            imageView = new ImageView();
            imageView.setImage(image);

            root = new StackPane();

            root.getChildren().add(imageView);
            primaryStage.setScene(new Scene(root));
            fitImage();

            primaryStage.show();
            OnScroll onScroll = new OnScroll(index);
            root.setOnScroll(onScroll);
            OnResize onResize = new OnResize();
            root.heightProperty().addListener(onResize);
            root.widthProperty().addListener(onResize);
            OnMouseDown onMouseDown = new OnMouseDown();
            root.setOnMousePressed(onMouseDown);
            root.setOnMouseReleased((event)->fitImage());

        }

    }

    private Image getImage(int index) {
        String imageUrl;
        try {
            imageUrl = imageList.get(index).toUri().toURL().toString();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Problem with file path: ", e);
        }
        return new Image(imageUrl);

    }

    private List<Path> getFiles(Path folder) throws IOException {
        List<Path> result = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder, "*.{jpg,jpeg,png,gif,bmp}")) {
            for (Path entry : stream) {
                result.add(entry);
            }
        } catch (DirectoryIteratorException ex) {
            // I/O error encounted during the iteration, the cause is an
            // IOException
            throw ex.getCause();
        }
        return result;
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

        private int index;

        public OnScroll(int startIndex) {
            this.index = startIndex;

        }

        @Override
        public void handle(ScrollEvent event) {
            double deltaY = event.getDeltaY();
            int maxIndex = imageList.size() - 1;
            if (deltaY < 0) {
                if (index == 0) {
                    index = maxIndex;
                } else {
                    index--;
                }
            } else {
                if (index == maxIndex) {
                    index = 0;
                } else {
                    index++;
                }
            }
            imageView.setImage(getImage(index));
            fitImage();
        }

    }

    private class OnResize implements ChangeListener<Number> {

        @Override
        public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
            fitImage();
        }

    }
    
    private class OnMouseDown implements EventHandler<MouseEvent>{

        @Override
        public void handle(MouseEvent event) {
            double x = event.getX();
            double y = event.getY();
            zoom100(x, y);
        }
        
    }
    
    private void zoom100(double x, double y){
        double imageHeight = imageView.getImage().getHeight();
        double centery = imageView.getViewport().getHeight()/2;
        double centerx = imageView.getViewport().getWidth()/2;
        imageView.setTranslateX(centerx-x);
        imageView.setTranslateY(centery-y);
        imageView.setFitHeight(-1);
        imageView.setFitWidth(-1);
    }
    
    
}
