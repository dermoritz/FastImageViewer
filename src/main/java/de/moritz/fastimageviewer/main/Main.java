package de.moritz.fastimageviewer.main;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    private static Logger LOG = null;

    private static Injector i;

    public static void main(String[] args) {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "DEBUG");
        LOG = LoggerFactory.getLogger(Main.class);
        i = Guice.createInjector(new DiModule(args.length > 0 ? args : null));
        // installTrust();
        Application.launch(args);
    }

    @Override
    public void start(final Stage primaryStage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/layout.fxml"));
        MainController controller = i.getInstance(MainController.class);

        if (SystemTray.isSupported()) {
            setupTray( primaryStage );

        }

        fxmlLoader.setController(controller);
        Scene scene = new Scene(fxmlLoader.load());
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.show();
        controller.onReady();
    }

    private void setupTray( final Stage primaryStage ) throws AWTException {
        Platform.setImplicitExit(false);
        primaryStage.setOnCloseRequest((e) -> primaryStage.hide());

        ActionListener listenerShow = ( e) -> Platform.runLater(() -> primaryStage.show());
        ActionListener listenerClose = (e) -> System.exit(0);
        SystemTray tray = SystemTray.getSystemTray();

        PopupMenu popup = new PopupMenu();
        MenuItem openItem = new MenuItem( "open" );
        MenuItem closeItem = new MenuItem( "close" );
        openItem.addActionListener( listenerShow );
        closeItem.addActionListener( listenerClose );
        popup.add( openItem );
        popup.add( closeItem );
        Image image = Toolkit.getDefaultToolkit().getImage( ClassLoader.getSystemClassLoader().getResource( "Google Images-32.png" ) );
        TrayIcon icon = new TrayIcon( image, "images", popup );
        icon.addMouseListener( new MouseListener() {
            @Override
            public void mouseClicked( MouseEvent e ) {
                if(e.getClickCount()>=2){
                    if(primaryStage.isShowing()){
                        Platform.runLater( () ->primaryStage.hide() );
                    }
                    else {
                        Platform.runLater( () ->primaryStage.show() );
                    }
                }
            }

            @Override
            public void mousePressed( MouseEvent e ) {

            }

            @Override
            public void mouseReleased( MouseEvent e ) {

            }

            @Override
            public void mouseEntered( MouseEvent e ) {

            }

            @Override
            public void mouseExited( MouseEvent e ) {

            }
        } );
        tray.add( icon );
    }

    private static void installTrust() {
        X509TrustManager trustAll = new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

            }

            @Override
            public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, new TrustManager[] {trustAll}, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            LOG.warn("Problem installing cert manager - to allow untrusted ssl connections:", e);
        }
    }

}
