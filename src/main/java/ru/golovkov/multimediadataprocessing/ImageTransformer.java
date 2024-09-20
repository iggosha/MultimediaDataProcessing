package ru.golovkov.multimediadataprocessing;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.util.Arrays;

public class ImageTransformer {

    public Image createNegativeImage(ImageWrapper imageWrapper) {
        WritableImage writableImage = new WritableImage(imageWrapper.getWidth(), imageWrapper.getHeight());
        PixelWriter pixelWriter = writableImage.getPixelWriter();
        PixelReader pixelReader = imageWrapper.getPixelReader();
        for (int y = 0; y < imageWrapper.getHeight(); y++) {
            for (int x = 0; x < imageWrapper.getWidth(); x++) {
                Color color = pixelReader.getColor(x, y);
                double red = 1.0 - color.getRed();
                double green = 1.0 - color.getGreen();
                double blue = 1.0 - color.getBlue();
                double opacity = color.getOpacity();
                pixelWriter.setColor(x, y, new Color(red, green, blue, opacity));
            }
        }
        return writableImage;
    }

    public Image createPowerLawTransformation(ImageWrapper imageWrapper, double gamma) {
        WritableImage writableImage = new WritableImage(imageWrapper.getWidth(), imageWrapper.getHeight());
        int c = 1;
        PixelWriter pixelWriter = writableImage.getPixelWriter();
        PixelReader pixelReader = imageWrapper.getPixelReader();
        for (int y = 0; y < imageWrapper.getHeight(); y++) {
            for (int x = 0; x < imageWrapper.getWidth(); x++) {
                Color color = pixelReader.getColor(x, y);
                double red = c * Math.pow(color.getRed(), gamma);
                double green = c * Math.pow(color.getGreen(), gamma);
                double blue = c * Math.pow(color.getBlue(), gamma);
                double opacity = color.getOpacity();
                pixelWriter.setColor(x, y, new Color(red, green, blue, opacity));
            }
        }
        return writableImage;
    }

    public Image createBrightnessRangeCut(ImageWrapper imageWrapper, int minBrightness, int maxBrightness) {
        WritableImage writableImage = new WritableImage(imageWrapper.getWidth(), imageWrapper.getHeight());
        PixelWriter pixelWriter = writableImage.getPixelWriter();
        PixelReader pixelReader = imageWrapper.getPixelReader();
        for (int y = 0; y < imageWrapper.getHeight(); y++) {
            for (int x = 0; x < imageWrapper.getWidth(); x++) {
                Color color = pixelReader.getColor(x, y);
                double red = color.getRed() * 255;
                double green = color.getGreen() * 255;
                double blue = color.getBlue() * 255;
                if (red < minBrightness || red > maxBrightness || green < minBrightness || green > maxBrightness || blue < minBrightness || blue > maxBrightness) {
                    red = green = blue = 0;
                } else {
                    red /= 255;
                    green /= 255;
                    blue /= 255;
                }
                double opacity = color.getOpacity();
                pixelWriter.setColor(x, y, new Color(red, green, blue, opacity));
            }
        }
        return writableImage;
    }

    public Image createAverageFilter(ImageWrapper imageWrapper) {
        WritableImage writableImage = new WritableImage(imageWrapper.getWidth(), imageWrapper.getHeight());
        PixelWriter pixelWriter = writableImage.getPixelWriter();
        PixelReader pixelReader = imageWrapper.getPixelReader();
        for (int y = 1; y < imageWrapper.getHeight() - 1; y++) {
            for (int x = 1; x < imageWrapper.getWidth() - 1; x++) {
                double sumRed = 0;
                double sumGreen = 0;
                double sumBlue = 0;
                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        Color color = pixelReader.getColor(x + kx, y + ky);
                        sumRed += color.getRed();
                        sumGreen += color.getGreen();
                        sumBlue += color.getBlue();
                    }
                }
                double opacity = pixelReader.getColor(x, y).getOpacity();
                double red = sumRed / 9;
                double green = sumGreen / 9;
                double blue = sumBlue / 9;
                pixelWriter.setColor(x, y, new Color(red, green, blue, opacity));
            }
        }
        return writableImage;
    }

    public Image createMedianFilter(ImageWrapper imageWrapper) {
        WritableImage writableImage = new WritableImage(imageWrapper.getWidth(), imageWrapper.getHeight());
        PixelWriter pixelWriter = writableImage.getPixelWriter();
        PixelReader pixelReader = imageWrapper.getPixelReader();
        for (int y = 1; y < imageWrapper.getHeight() - 1; y++) {
            for (int x = 1; x < imageWrapper.getWidth() - 1; x++) {
                double[] redValues = new double[9];
                double[] greenValues = new double[9];
                double[] blueValues = new double[9];
                int index = 0;
                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        Color color = pixelReader.getColor(x + kx, y + ky);
                        redValues[index] = color.getRed();
                        greenValues[index] = color.getGreen();
                        blueValues[index] = color.getBlue();
                        index++;
                    }
                }
                Arrays.sort(redValues);
                Arrays.sort(greenValues);
                Arrays.sort(blueValues);
                double opacity = pixelReader.getColor(x, y).getOpacity();
                double red = redValues[4];
                double green = greenValues[4];
                double blue = blueValues[4];
                pixelWriter.setColor(x, y, new Color(red, green, blue, opacity));
            }
        }
        return writableImage;
    }

    public Image createRobertsGradient(ImageWrapper imageWrapper) {
        WritableImage writableImage = new WritableImage(imageWrapper.getWidth(), imageWrapper.getHeight());
        PixelWriter pixelWriter = writableImage.getPixelWriter();
        PixelReader pixelReader = imageWrapper.getPixelReader();
        for (int y = 0; y < imageWrapper.getHeight() - 1; y++) {
            for (int x = 0; x < imageWrapper.getWidth() - 1; x++) {
                Color color1 = pixelReader.getColor(x, y);
                Color color2 = pixelReader.getColor(x + 1, y);
                Color color3 = pixelReader.getColor(x, y + 1);
                Color color4 = pixelReader.getColor(x + 1, y + 1);

                double red1 = color1.getRed();
                double green1 = color1.getGreen();
                double blue1 = color1.getBlue();

                double red2 = color2.getRed();
                double green2 = color2.getGreen();
                double blue2 = color2.getBlue();

                double red3 = color3.getRed();
                double green3 = color3.getGreen();
                double blue3 = color3.getBlue();

                double red4 = color4.getRed();
                double green4 = color4.getGreen();
                double blue4 = color4.getBlue();

                double red = Math.sqrt((red1 - red4) * (red1 - red4) + (red2 - red3) * (red2 - red3));
                if (red > 1.0) red = 1;
                double green = Math.sqrt((green1 - green4) * (green1 - green4) + (green2 - green3) * (green2 - green3));
                if (green > 1.0) green = 1;
                double blue = Math.sqrt((blue1 - blue4) * (blue1 - blue4) + (blue2 - blue3) * (blue2 - blue3));
                if (blue > 1.0) blue = 1;

                double opacity = color1.getOpacity();
                pixelWriter.setColor(x, y, new Color(red, green, blue, opacity));
            }
        }
        return writableImage;
    }

    public Image createSobelGradient(ImageWrapper imageWrapper) {
        WritableImage writableImage = new WritableImage(imageWrapper.getWidth(), imageWrapper.getHeight());
        PixelWriter pixelWriter = writableImage.getPixelWriter();
        PixelReader pixelReader = imageWrapper.getPixelReader();
        for (int y = 1; y < imageWrapper.getHeight() - 1; y++) {
            for (int x = 1; x < imageWrapper.getWidth() - 1; x++) {
                double gradientX = 0;
                double gradientY = 0;
                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        Color color = pixelReader.getColor(x + kx, y + ky);
                        double red = color.getRed();
                        double green = color.getGreen();
                        double blue = color.getBlue();
                        gradientX += kx * (red + green + blue);
                        gradientY += ky * (red + green + blue);
                    }
                }
                double gradient = Math.sqrt(gradientX * gradientX + gradientY * gradientY);
                double red = Math.min(gradient, 1.0);
                double opacity = pixelReader.getColor(x, y).getOpacity();
                pixelWriter.setColor(x, y, new Color(red, red, red, opacity));
            }
        }
        return writableImage;
    }

    public Image createLaplacian(ImageWrapper imageWrapper) {
        WritableImage writableImage = new WritableImage(imageWrapper.getWidth(), imageWrapper.getHeight());
        PixelWriter pixelWriter = writableImage.getPixelWriter();
        PixelReader pixelReader = imageWrapper.getPixelReader();
        for (int y = 1; y < imageWrapper.getHeight() - 1; y++) {
            for (int x = 1; x < imageWrapper.getWidth() - 1; x++) {
                double sumRed = 0;
                double sumGreen = 0;
                double sumBlue = 0;
                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        Color color = pixelReader.getColor(x + kx, y + ky);
                        sumRed += color.getRed();
                        sumGreen += color.getGreen();
                        sumBlue += color.getBlue();
                    }
                }
                Color centerColor = pixelReader.getColor(x, y);
                double redCenter = centerColor.getRed();
                double greenCenter = centerColor.getGreen();
                double blueCenter = centerColor.getBlue();

                double red = Math.abs(redCenter - sumRed / 9);
                double green = Math.abs(greenCenter - sumGreen / 9);
                double blue = Math.abs(blueCenter - sumBlue / 9);

                double opacity = centerColor.getOpacity();
                pixelWriter.setColor(x, y, new Color(red, green, blue, opacity));
            }
        }
        return writableImage;
    }
}
