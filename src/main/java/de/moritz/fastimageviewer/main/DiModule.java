package de.moritz.fastimageviewer.main;

import com.google.common.eventbus.EventBus;
import com.google.inject.*;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import de.moritz.fastimageviewer.image.ImageProvider;
import de.moritz.fastimageviewer.image.file.FileImageProvider;
import de.moritz.fastimageviewer.image.file.ImageBuffer;
import de.moritz.fastimageviewer.image.imageservice.ImageServiceApi;
import de.moritz.fastimageviewer.image.imageservice.ImageServiceApiFactory;
import de.moritz.fastimageviewer.image.imageservice.ImageServiceApiImpl;
import de.moritz.fastimageviewer.image.imageservice.ImageServiceImageProvider;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

public class DiModule extends AbstractModule {

    private final String[] args;

    private volatile EventBus eventBus = new EventBus();

    public DiModule(String[] args) {
        this.args = args == null ? new String[] {} : args;
    }

    @Override
    protected void configure() {
        install(new FactoryModuleBuilder().implement(ImageBuffer.class, ImageBuffer.class)
                                          .build(ImageBuffer.Inst.class));
        install(new FactoryModuleBuilder().implement(ImageProvider.class, ImageServiceImageProvider.class)
                                          .build(ImageServiceImageProvider.Inst.class));
        install(new FactoryModuleBuilder().implement(ImageProvider.class, FileImageProvider.class)
                                          .build(FileImageProvider.Inst.class));
        install(new FactoryModuleBuilder().implement(ImageServiceApi.class, ImageServiceApiImpl.class)
                                          .build(ImageServiceApiFactory.class));
        bind(EventBus.class).toInstance(eventBus);

    }

    @Provides
    @Singleton
    @Args
    public String[] getArgs() {
        return args;
    }

    @BindingAnnotation
    @Target({FIELD, PARAMETER, METHOD})
    @Retention(RUNTIME)
    public @interface Args {
    }

}
