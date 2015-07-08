package de.moritz.fastimageviewer.main;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import de.moritz.fastimageviewer.image.ImageProvider;
import de.moritz.fastimageviewer.image.ImageProviderImpl;
import javafx.scene.Parent;

public class DiModule extends AbstractModule {

    @Override
    protected void configure() {
         //
    }
    
    @Provides
    @Singleton
    @Inject
    public Parent getParent(MainLayout ml){
        return ml.getRoot();
    }
    
    @Provides
    @Singleton
    public ImageProvider getImageProvider(){
        return new ImageProviderImpl("C:/Users/moritz/Downloads/1/Heidi18Years_2014-02-22_61_10000");
    }

}
