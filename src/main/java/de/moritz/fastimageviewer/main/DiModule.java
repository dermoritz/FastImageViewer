package de.moritz.fastimageviewer.main;

import com.google.inject.AbstractModule;
import com.google.inject.BindingAnnotation;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import de.moritz.fastimageviewer.image.ImageProvider;
import de.moritz.fastimageviewer.image.ImageProviderImpl;
import de.moritz.fastimageviewer.image.ImageServiceImageProvider;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

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
    @Args
    public String[] getArgs(){
        return args;
    }

    @BindingAnnotation
    @Target({ FIELD, PARAMETER, METHOD }) @Retention(RUNTIME)
    public @interface Args {}

}
