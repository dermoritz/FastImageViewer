package de.moritz.fastimageviewer.image.imageservice;

import com.google.inject.assistedinject.Assisted;

public interface ImageServiceApiFactory {
    ImageServiceApi get(@Assisted String serviceUrl);
}