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

    private final String[] args;
    private String startPath;

    public DiModule( String[] args ) {
        this.startPath = args == null ? null : args[0];
        this.args = args;
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

}
