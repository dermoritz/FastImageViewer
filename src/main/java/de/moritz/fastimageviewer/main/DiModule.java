package de.moritz.fastimageviewer.main;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.google.common.eventbus.EventBus;
import com.google.inject.AbstractModule;
import com.google.inject.BindingAnnotation;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

import de.moritz.fastimageviewer.image.FileImageProvider;
import de.moritz.fastimageviewer.image.ImageBuffer;
import de.moritz.fastimageviewer.image.ImageProvider;
import de.moritz.fastimageviewer.image.ImageServiceImageProvider;

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
        bind(EventBus.class).toInstance(eventBus);
        bindListener(Matchers.any(), new TypeListener() {

            @Override
            public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
                encounter.register(new InjectionListener<I>() {

                    @Override
                    public void afterInjection(Object injectee) {
                        eventBus.register(injectee);

                    }
                });

            }
        });
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
