package ru.golovkov.multimediadataprocessing;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import lombok.Data;

@Data
public class ImageWrapper {

    private int width;
    private int height;
    private PixelReader pixelReader;

    public ImageWrapper(Image image) {
        pixelReader = image.getPixelReader();
        height = (int) image.getHeight();
        width = (int) image.getWidth();
    }
}
