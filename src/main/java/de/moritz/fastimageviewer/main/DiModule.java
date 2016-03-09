package de.moritz.fastimageviewer.main;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.google.inject.AbstractModule;
import com.google.inject.BindingAnnotation;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public class DiModule extends AbstractModule {

    private final String[] args;

    public DiModule( String[] args ) {
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
