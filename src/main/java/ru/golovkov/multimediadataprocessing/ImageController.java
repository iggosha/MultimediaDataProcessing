package ru.golovkov.multimediadataprocessing;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
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

    private Image originalImage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
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
            case "Негатив" -> transformedImage = createNewNegativeImage(originalImageWrapper);
            case "Степенное преобразование" ->
                    transformedImage = createPowerLawTransformation(originalImageWrapper, 1.0, 2.0);
            case "Вырезание диапазона яркостей" ->
                    transformedImage = createBrightnessRangeCut(originalImageWrapper, 50, 200);
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

    // Остальные методы остаются без изменений

    private Image createNewNegativeImage(ImageWrapper imageWrapper) {
        WritableImage writableImage = new WritableImage(imageWrapper.getWidth(), imageWrapper.getHeight());
        PixelWriter pixelWriter = writableImage.getPixelWriter();
        PixelReader pixelReader = imageWrapper.getPixelReader();
        for (int y = 0; y < imageWrapper.getHeight(); y++) {
            for (int x = 0; x < imageWrapper.getWidth(); x++) {
                Color color = pixelReader.getColor(x, y);
                double r = 1.0 - color.getRed();
                double g = 1.0 - color.getGreen();
                double b = 1.0 - color.getBlue();
                double a = color.getOpacity();
                pixelWriter.setColor(x, y, new Color(r, g, b, a));
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
                double r = c * Math.pow(color.getRed(), gamma);
                double g = c * Math.pow(color.getGreen(), gamma);
                double b = c * Math.pow(color.getBlue(), gamma);
                double a = color.getOpacity();
                pixelWriter.setColor(x, y, new Color(r, g, b, a));
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
                double r = color.getRed() * 255;
                double g = color.getGreen() * 255;
                double b = color.getBlue() * 255;
                if (r < minBrightness || r > maxBrightness || g < minBrightness || g > maxBrightness || b < minBrightness || b > maxBrightness) {
                    r = g = b = 0;
                } else {
                    r /= 255;
                    g /= 255;
                    b /= 255;
                }
                double a = color.getOpacity();
                pixelWriter.setColor(x, y, new Color(r, g, b, a));
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
                double sumR = 0;
                double sumG = 0;
                double sumB = 0;
                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        Color color = pixelReader.getColor(x + kx, y + ky);
                        sumR += color.getRed();
                        sumG += color.getGreen();
                        sumB += color.getBlue();
                    }
                }
                double a = pixelReader.getColor(x, y).getOpacity();
                double r = sumR / 9;
                double g = sumG / 9;
                double b = sumB / 9;
                pixelWriter.setColor(x, y, new Color(r, g, b, a));
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
                double[] rValues = new double[9];
                double[] gValues = new double[9];
                double[] bValues = new double[9];
                int index = 0;
                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        Color color = pixelReader.getColor(x + kx, y + ky);
                        rValues[index] = color.getRed();
                        gValues[index] = color.getGreen();
                        bValues[index] = color.getBlue();
                        index++;
                    }
                }
                Arrays.sort(rValues);
                Arrays.sort(gValues);
                Arrays.sort(bValues);
                double a = pixelReader.getColor(x, y).getOpacity();
                double r = rValues[4];
                double g = gValues[4];
                double b = bValues[4];
                pixelWriter.setColor(x, y, new Color(r, g, b, a));
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

                double r1 = color1.getRed();
                double g1 = color1.getGreen();
                double b1 = color1.getBlue();

                double r2 = color2.getRed();
                double g2 = color2.getGreen();
                double b2 = color2.getBlue();

                double r3 = color3.getRed();
                double g3 = color3.getGreen();
                double b3 = color3.getBlue();

                double r4 = color4.getRed();
                double g4 = color4.getGreen();
                double b4 = color4.getBlue();

                double r = Math.sqrt((r1 - r4) * (r1 - r4) + (r2 - r3) * (r2 - r3));
                double g = Math.sqrt((g1 - g4) * (g1 - g4) + (g2 - g3) * (g2 - g3));
                double b = Math.sqrt((b1 - b4) * (b1 - b4) + (b2 - b3) * (b2 - b3));

                double a = color1.getOpacity();
                pixelWriter.setColor(x, y, new Color(r, g, b, a));
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
                double gx = 0;
                double gy = 0;
                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        Color color = pixelReader.getColor(x + kx, y + ky);
                        double r = color.getRed();
                        double g = color.getGreen();
                        double b = color.getBlue();
                        gx += kx * (r + g + b);
                        gy += ky * (r + g + b);
                    }
                }
                double gradient = Math.sqrt(gx * gx + gy * gy);
                double r = Math.min(gradient, 1.0);
                double a = pixelReader.getColor(x, y).getOpacity();
                pixelWriter.setColor(x, y, new Color(r, r, r, a));
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
                double sumR = 0;
                double sumG = 0;
                double sumB = 0;
                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        Color color = pixelReader.getColor(x + kx, y + ky);
                        sumR += color.getRed();
                        sumG += color.getGreen();
                        sumB += color.getBlue();
                    }
                }
                Color colorCenter = pixelReader.getColor(x, y);
                double rCenter = colorCenter.getRed();
                double gCenter = colorCenter.getGreen();
                double bCenter = colorCenter.getBlue();

                double r = Math.abs(rCenter - sumR / 9);
                double g = Math.abs(gCenter - sumG / 9);
                double b = Math.abs(bCenter - sumB / 9);

                double a = colorCenter.getOpacity();
                pixelWriter.setColor(x, y, new Color(r, g, b, a));
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
                double r = color.getRed() * 255;
                double g = color.getGreen() * 255;
                double b = color.getBlue() * 255;
                int gray = (int) ((r + g + b) / 3);
                histogram[gray]++;
            }
        }
        return histogram;
    }
}
