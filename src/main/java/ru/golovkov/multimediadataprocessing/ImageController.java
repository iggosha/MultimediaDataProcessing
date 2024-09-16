package ru.golovkov.multimediadataprocessing;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Slider;
import javafx.scene.image.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;

public class ImageController implements Initializable {

    @FXML
    private ImageView originalImageView;

    @FXML
    private ImageView transformedImageView;

    @FXML
    private ComboBox<String> transformationComboBox;

    @FXML
    private BarChart<String, Number> originalHistogramChart;

    @FXML
    private BarChart<String, Number> transformedHistogramChart;

    @FXML
    private Slider minBrightnessSlider;

    @FXML
    private Slider maxBrightnessSlider;

    @FXML
    private Slider gammaSlider;

    @FXML
    private Slider scaleSlider;

    private Image originalImage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        gammaSlider.setVisible(false);
        minBrightnessSlider.setVisible(false);
        maxBrightnessSlider.setVisible(false);
        scaleSlider.setVisible(false);
        transformationComboBox.getItems().addAll(
                "Негатив",
                "Степенное преобразование",
                "Вырезание диапазона яркостей",
                "Линейный сглаживающий (усредняющий) фильтр",
                "Медианный фильтр",
                "Градиент Робертса",
                "Градиент Собеля",
                "Лапласиан"
        );
    }

    @FXML
    private void handleOpenImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите изображение");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Изображения", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        File selectedFile = fileChooser.showOpenDialog(new Stage());
        if (selectedFile != null) {
            originalImage = new Image(selectedFile.toURI().toString());
            originalImageView.setImage(originalImage);
            displayHistogram(originalImage, originalHistogramChart);
        }
    }

    @FXML
    private void handleApplyTransformation() {
        gammaSlider.setVisible(false);
        minBrightnessSlider.setVisible(false);
        maxBrightnessSlider.setVisible(false);
        scaleSlider.setVisible(false);
        if (originalImage == null) {
            return;
        }

        String selectedTransformation = transformationComboBox.getValue();
        if (selectedTransformation == null) {
            return;
        }

        Image transformedImage;

        ImageWrapper originalImageWrapper = new ImageWrapper(originalImage);

        switch (selectedTransformation) {
            case "Негатив" -> transformedImage = createNegativeImage(originalImageWrapper);
            case "Степенное преобразование" -> {
                gammaSlider.setVisible(true);
                scaleSlider.setVisible(true);
                transformedImage = createPowerLawTransformation(originalImageWrapper, scaleSlider.getValue(), gammaSlider.getValue());
            }
            case "Вырезание диапазона яркостей" -> {
                minBrightnessSlider.setVisible(true);
                maxBrightnessSlider.setVisible(true);
                transformedImage = createBrightnessRangeCut(originalImageWrapper, (int) minBrightnessSlider.getValue(), (int) maxBrightnessSlider.getValue());
            }
            case "Линейный сглаживающий (усредняющий) фильтр" ->
                    transformedImage = createAverageFilter(originalImageWrapper);
            case "Медианный фильтр" -> transformedImage = createMedianFilter(originalImageWrapper);
            case "Градиент Робертса" -> transformedImage = createRobertsGradient(originalImageWrapper);
            case "Градиент Собеля" -> transformedImage = createSobelGradient(originalImageWrapper);
            case "Лапласиан" -> transformedImage = createLaplacian(originalImageWrapper);
            default -> throw new IllegalArgumentException("Ничего не выбрано");
        }

        transformedImageView.setImage(transformedImage);
        displayHistogram(transformedImage, transformedHistogramChart);
    }

    private void displayHistogram(Image image, BarChart<String, Number> histogramChart) {
        ImageWrapper imageWrapper = new ImageWrapper(image);
        int[] histogram = createHistogram(imageWrapper);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Гистограмма");

        for (int i = 0; i < histogram.length; i++) {
            series.getData().add(new XYChart.Data<>(String.valueOf(i), histogram[i]));
        }

        histogramChart.getData().clear();
        histogramChart.getData().add(series);
    }

    private Image createNegativeImage(ImageWrapper imageWrapper) {
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

    private Image createPowerLawTransformation(ImageWrapper imageWrapper, double c, double gamma) {
        WritableImage writableImage = new WritableImage(imageWrapper.getWidth(), imageWrapper.getHeight());
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

    private Image createBrightnessRangeCut(ImageWrapper imageWrapper, int minBrightness, int maxBrightness) {
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

    private Image createAverageFilter(ImageWrapper imageWrapper) {
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

    private Image createMedianFilter(ImageWrapper imageWrapper) {
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

    private Image createRobertsGradient(ImageWrapper imageWrapper) {
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
                double green = Math.sqrt((green1 - green4) * (green1 - green4) + (green2 - green3) * (green2 - green3));
                double blue = Math.sqrt((blue1 - blue4) * (blue1 - blue4) + (blue2 - blue3) * (blue2 - blue3));

                double opacity = color1.getOpacity();
                pixelWriter.setColor(x, y, new Color(red, green, blue, opacity));
            }
        }
        return writableImage;
    }

    private Image createSobelGradient(ImageWrapper imageWrapper) {
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

    private Image createLaplacian(ImageWrapper imageWrapper) {
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

    private int[] createHistogram(ImageWrapper imageWrapper) {
        int[] histogram = new int[256];
        PixelReader pixelReader = imageWrapper.getPixelReader();
        for (int y = 0; y < imageWrapper.getHeight(); y++) {
            for (int x = 0; x < imageWrapper.getWidth(); x++) {
                Color color = pixelReader.getColor(x, y);
                double red = color.getRed() * 255;
                double green = color.getGreen() * 255;
                double blue = color.getBlue() * 255;
                int gray = (int) ((red + green + blue) / 3);
                histogram[gray]++;
            }
        }
        return histogram;
    }
}
