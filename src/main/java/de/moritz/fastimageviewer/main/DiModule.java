package de.moritz.fastimageviewer.main;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import de.moritz.fastimageviewer.image.ImageProvider;
import de.moritz.fastimageviewer.image.ImageProviderImpl;
import de.moritz.fastimageviewer.image.ImageServiceImageProvider;
import javafx.scene.Parent;

public class DiModule extends AbstractModule {

    private String startPath;

    public DiModule( String startPath ) {
        this.startPath = startPath;
    }

    @Override
    protected void configure() {
        //
    }

    @Provides
    @Singleton
    @Inject
    public Parent getParent( MainLayout ml ) {
        return ml.getRoot();
    }

    @Provides
    @Singleton
    public ImageProvider getImageProvider() {
        if( startPath.toLowerCase().startsWith( "http" ) ) {
            return new ImageServiceImageProvider( startPath );
        } else {
            return new ImageProviderImpl( startPath );
        }
    }

}
