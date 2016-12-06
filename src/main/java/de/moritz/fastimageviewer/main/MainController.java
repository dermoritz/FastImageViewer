package de.moritz.fastimageviewer.main;

import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;

/**
 * Created by moritz on 05.03.2016.
 */
@Singleton
public class MainController {

    private final ImageViewer imageView;
    private ImageProvider ip;
    private final String[] args;
    private static final float MOVEMENT_PIXEL = 50f;
    private static final Logger LOG = LoggerFactory.getLogger( MainController.class );

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
    private boolean zoomedIn = false;
    private Double mouseStartX;
    private Double mouseStartY;

    @Inject
    public MainController( ImageViewer imageView, @Args String[] args, FileImageProvider.Inst fileImageProviderFactory,
                           ImageServiceImageProvider.Inst serviceImageProviderFactory ) {
        this.args = args;
        this.fileImageProviderFactory = fileImageProviderFactory;
        this.serviceImageProviderFactory = serviceImageProviderFactory;
        this.ip = getIp( args );
        this.imageView = imageView;
    }

    @Subscribe
    public void updateBuffer( BufferState state ) {
        bufferBar.setProgress( state.getForward() );
    }

    @Subscribe
    public void setImageWaitedFor( Image image ) {
        imageView.setImageAndFit( image );
    }

    private ImageProvider getIp( String[] args ) {
        startPath = args == null || args.length < 1 ? null : args[0];
        subPath = null;
        ImageProvider ip;
        if( startPath != null && startPath.toLowerCase().startsWith( "http" ) ) {
            webserviceMode = true;
            ip = serviceImageProviderFactory.get( startPath );
            if( args.length > 1 && !Strings.isNullOrEmpty( args[1] ) ) {
                subPath = args[1];
                ip.setPath( subPath );
            }
        } else {
            webserviceMode = false;
            ip = fileImageProviderFactory.get( startPath );
        }
        return ip;
    }

    private void registerEvents() {
        root.addEventFilter( KeyEvent.KEY_PRESSED, this::keys );
        root.setOnScroll( this::handleScroll );
        root.heightProperty().addListener( (observable)->handleResize() );
        root.widthProperty().addListener( (observable)->handleResize() );
        root.setOnDragOver( this::dragOver );
        root.setOnDragDropped( this::dropFile );
        imageArea.setOnMousePressed( this::handleMouseDown );
        imageArea.setOnMouseReleased( this::handleMouseRelease );
        imageArea.setOnMouseDragged( this::dragOnMouseMove );
        goButton.setOnAction( this::handlePathChanged );
        infoButton.setOnAction( this::onInfoButton );
        sortCheckBox.selectedProperty().addListener( this::sortChanged );

    }

    @FXML
    public void initialize() {
        imageArea.getChildren().add( imageView.getImageView() );
        registerEvents();
        if( args.length > 0 ) {
            pathField.setText( args[0] );
        }
        if( args.length > 1 ) {
            filterField.setText( args[1].replaceFirst( "/", "" ) );
        }
        //unfocus pathField
        Platform.runLater( () -> root.requestFocus() );
    }

    private void handleMouseDown( MouseEvent event ) {
        if( event.getSource().equals( imageArea ) ) {
            imageView.requestFocus();
            LOG.debug( "handle mouse down" );
            root.getScene().setCursor( Cursor.NONE );
            double x = event.getX();
            double y = event.getY();
            event.consume();
            zoomedIn = true;
            imageView.zoom100( x, y );
            mouseStartX = null;
            mouseStartY = null;
        }
    }

    public void handleMouseRelease( MouseEvent event ) {
        root.getScene().setCursor( Cursor.DEFAULT );
        zoomedIn = false;
        imageView.fitImage();
    }

    public void dragOnMouseMove( MouseEvent event ) {
        if( zoomedIn ) {
            if( mouseStartX != null && mouseStartY != null ) {
                double x = mouseStartX - event.getX();
                double y = mouseStartY - event.getY();
                imageView.moveImageX( x );
                imageView.moveImageY( y );
            }
            mouseStartX = event.getX();
            mouseStartY = event.getY();
        }
    }

    public void onReady() {
        if( ip != null && ip.hasNext() ) {
            imageView.setImageAndFit( ip.next() );
        }
    }

    private void onInfoButton( ActionEvent event ) {
        try {
            Executors.callable(() -> infoField.setText( ip.getInfoForLast() )).call();
        } catch( Exception e ) {
            LOG.error("Problem on getting info: " + e);
        }
    }

    private void handlePathChanged( ActionEvent event ) {
        String newFilter = filterField.getText();
        if( !Strings.isNullOrEmpty( newFilter ) && !newFilter.startsWith( "/" ) ) {
            newFilter = "/" + newFilter;
        }
        if( !pathField.getText().equals( startPath ) ) {
            ip = getIp( new String[]{ pathField.getText(), newFilter } );
            imageView.setImageAndFit( ip.next() );
        } else if( !newFilter.equals( subPath ) && webserviceMode ) {
            ip.setPath( newFilter );
            imageView.setImageAndFit( ip.next() );
        }
    }

    private void keys( KeyEvent event ) {
        KeyCode code = event.getCode();
        switch( code ) {
            case W:
                if(zoomedIn){
                    imageView.moveImageY( MOVEMENT_PIXEL );
                } else if(!filterField.isFocused() && !pathField.isFocused()){
                    imageView.setImageAndFit( ip.prev() );
                }

                break;
            case S:
                if(zoomedIn){
                    imageView.moveImageY( -MOVEMENT_PIXEL );
                } else if(!filterField.isFocused() && !pathField.isFocused()){
                    imageView.setImageAndFit( ip.next() );
                }
                break;
            case A:
                if(zoomedIn){
                    imageView.moveImageX( MOVEMENT_PIXEL );
                }
                break;
            case D:
                if(zoomedIn){
                    imageView.moveImageX( -MOVEMENT_PIXEL );
                }
                break;
            case PAGE_UP:
                imageView.setImageAndFit( ip.prev() );
                event.consume();
                break;
            case PAGE_DOWN:
                imageView.setImageAndFit( ip.next() );
                event.consume();
                break;
            case ESCAPE:
                root.getScene().getWindow().hide();
                break;
            case I:
            case Q:
                onInfoButton( null );
        }
    }

    private void dragOver( DragEvent event ) {
        Dragboard db = event.getDragboard();
        if( db.hasFiles() ) {
            event.acceptTransferModes( TransferMode.LINK );
        } else {
            event.consume();
        }
    }

    private void dropFile( DragEvent event ) {
        Dragboard db = event.getDragboard();
        boolean success = false;
        if( db.hasFiles() && db.getFiles().size() > 0 ) {
            ip.setPath( db.getFiles().get( 0 ).toString() );
            Image image = ip.getImage();
            imageView.setImageAndFit( image );
            success = true;
        }
        event.setDropCompleted( success );
        event.consume();
    }

    private void handleScroll( ScrollEvent event ) {
        double deltaY = event.getDeltaY();
        event.consume();
        if( deltaY < 0 ) {
            imageView.setImageAndFit( ip.next() );
        } else {
            imageView.setImageAndFit( ip.prev() );
        }
    }

    private void handleResize( ) {
        //using run later to be sure the call is done after resize is finished
        Platform.runLater( imageView::fitImage);
    }

    private void sortChanged( ObservableValue<? extends Boolean> selected, Boolean oldV, Boolean newV ) {
        ip.setSort( newV );
    }

}
