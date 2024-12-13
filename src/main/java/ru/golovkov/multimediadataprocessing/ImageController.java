package ru.golovkov.multimediadataprocessing;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
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
    private CheckBox maskColorCheckBox;

    @FXML
    private CheckBox replaceOriginalCheckBox;

    private ImageTransformer imageTransformer;

    private Image originalImage;

    private ImageWrapper originalImageWrapper;

    private Image transformedImage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        makeAllSlidersInvisible();
        imageTransformer = new ImageTransformer();
        transformationComboBox.getItems().addAll(
                "Негатив",
                "Степенное преобразование",
                "Вырезание диапазона яркостей",
                "Вырезание диапазона яркостей 2",
                "Линейный сглаживающий (усредняющий) фильтр",
                "Медианный фильтр",
                "Градиент Робертса",
                "Градиент Собеля",
                "Лапласиан 90",
                "Лапласиан 45",
                "Эквализация гистограммы",
                "Пороговая обработка",
                "Метод Оцу",
                "Дилатация",
                "Эрозия",
                "Замыкание",
                "Размыкание",
                "Выделение границ",
                "Остов"
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
        originalImageWrapper = new ImageWrapper(originalImage);
    }

    @FXML
    private void handleApplyTransformation() {
        makeAllSlidersInvisible();
        if (originalImage == null) {
            return;
        }
        String selectedTransformation = transformationComboBox.getValue();
        if (selectedTransformation == null) {
            return;
        }
        applyTransformation(selectedTransformation);
        transformedImageView.setImage(transformedImage);
        displayHistogram(transformedImage, transformedHistogramChart);

        if (replaceOriginalCheckBox.isSelected()) {
            originalImage = transformedImage;
            originalImageView.setImage(originalImage);
            displayHistogram(originalImage, originalHistogramChart);
            originalImageWrapper = new ImageWrapper(originalImage);
        }
    }

    @FXML
    private void handleSaveImage() {
        if (transformedImage == null) {
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранить изображение");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PNG", "*.png"),
                new FileChooser.ExtensionFilter("JPG", "*.jpg"),
                new FileChooser.ExtensionFilter("JPEG", "*.jpeg"),
                new FileChooser.ExtensionFilter("GIF", "*.gif")
        );
        File selectedFile = fileChooser.showSaveDialog(new Stage());
        if (selectedFile != null) {
            try {
                BufferedImage bufferedImage = convertToBufferedImage(transformedImage);
                String formatName = getFormatName(selectedFile);
                ImageIO.write(bufferedImage, formatName, selectedFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String getFormatName(File file) {
        String fileName = file.getName();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex >= 0) {
            return fileName.substring(dotIndex + 1);
        }
        return "png"; // Default format
    }

    private BufferedImage convertToBufferedImage(Image image) {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        WritableImage writableImage = new WritableImage(width, height);
        PixelReader pixelReader = image.getPixelReader();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = pixelReader.getColor(x, y);
                writableImage.getPixelWriter().setColor(x, y, color);
            }
        }
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = writableImage.getPixelReader().getColor(x, y);
                int r = (int) (color.getRed() * 255);
                int g = (int) (color.getGreen() * 255);
                int b = (int) (color.getBlue() * 255);
                int a = (int) (color.getOpacity() * 255);
                int rgba = (a << 24) | (r << 16) | (g << 8) | b;
                bufferedImage.setRGB(x, y, rgba);
            }
        }
        return bufferedImage;
    }

    private void applyTransformation(String selectedTransformation) {
        int[][] structuringElement = {{1, 1, 1}, {1, 1, 1}, {1, 1, 1}};
        boolean isMaskColorBlack = maskColorCheckBox.isSelected();

        switch (selectedTransformation) {
            case "Негатив" -> transformedImage = imageTransformer.createNegativeImage(originalImageWrapper);
            case "Степенное преобразование" -> {
                makeControlsVisible(gammaSlider);
                transformedImage = imageTransformer.createPowerLawTransformation(originalImageWrapper, gammaSlider.getValue());
            }
            case "Вырезание диапазона яркостей" -> {
                makeControlsVisible(minBrightnessSlider, maxBrightnessSlider);
                transformedImage = imageTransformer.createBrightnessRangeCut(originalImageWrapper,
                        (int) minBrightnessSlider.getValue(), (int) maxBrightnessSlider.getValue());
            }
            case "Вырезание диапазона яркостей 2" -> {
                makeControlsVisible(minBrightnessSlider, maxBrightnessSlider);
                transformedImage = imageTransformer.createBrightnessRangeCut2(originalImageWrapper,
                        (int) minBrightnessSlider.getValue(), (int) maxBrightnessSlider.getValue());
            }
            case "Линейный сглаживающий (усредняющий) фильтр" ->
                    transformedImage = imageTransformer.createAverageFilter(originalImageWrapper);
            case "Медианный фильтр" -> transformedImage = imageTransformer.createMedianFilter(originalImageWrapper);
            case "Градиент Робертса" ->
                    transformedImage = imageTransformer.createRobertsGradient(originalImageWrapper);
            case "Градиент Собеля" -> transformedImage = imageTransformer.createSobelGradient(originalImageWrapper);
            case "Лапласиан 90" -> transformedImage = imageTransformer.createLaplacian90(originalImageWrapper);
            case "Лапласиан 45" -> transformedImage = imageTransformer.createLaplacian45(originalImageWrapper);
            case "Эквализация гистограммы" -> transformedImage = imageTransformer.createHistogramEqualization(originalImageWrapper);
            case "Пороговая обработка" -> {
                makeControlsVisible(minBrightnessSlider);
                transformedImage = imageTransformer.applyThreshold(originalImageWrapper, (int) minBrightnessSlider.getValue());
            }
            case "Метод Оцу" -> transformedImage = imageTransformer.applyOtsuThreshold(originalImageWrapper);
            case "Дилатация" -> {
                makeControlsVisible(maskColorCheckBox);
                transformedImage = imageTransformer.dilate(originalImageWrapper, structuringElement, isMaskColorBlack);
            }
            case "Эрозия" -> {
                makeControlsVisible(maskColorCheckBox);
                transformedImage = imageTransformer.erode(originalImageWrapper, structuringElement, isMaskColorBlack);
            }
            case "Замыкание" -> {
                makeControlsVisible(maskColorCheckBox);
                transformedImage = imageTransformer.close(originalImageWrapper, structuringElement, isMaskColorBlack);
            }
            case "Размыкание" -> {
                makeControlsVisible(maskColorCheckBox);
                transformedImage = imageTransformer.open(originalImageWrapper, structuringElement, isMaskColorBlack);
            }
            case "Выделение границ" -> {
                makeControlsVisible(maskColorCheckBox);
                transformedImage = imageTransformer.boundaryExtraction(originalImageWrapper, structuringElement, isMaskColorBlack);
            }
            case "Остов" -> {
                makeControlsVisible(maskColorCheckBox);
                transformedImage = imageTransformer.skeletonize(originalImageWrapper, isMaskColorBlack);
            }
            default -> throw new IllegalArgumentException("Ничего не выбрано");
        }
    }

    private void displayHistogram(Image image, BarChart<String, Number> histogramChart) {
        ImageWrapper imageWrapper = new ImageWrapper(image);
        int[] histogram = gatherHistogramData(imageWrapper);
        int[] extendedHistogram = new int[300];
        System.arraycopy(histogram, 0, extendedHistogram, 0, 256);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (int i = 0; i < extendedHistogram.length; i++) {
            series.getData().add(new XYChart.Data<>(String.valueOf(i), extendedHistogram[i]));
        }
        histogramChart.getData().clear();
        histogramChart.getData().add(series);
    }

    private int[] gatherHistogramData(ImageWrapper imageWrapper) {
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

    private void makeAllSlidersInvisible() {
        gammaSlider.setVisible(false);
        minBrightnessSlider.setVisible(false);
        maxBrightnessSlider.setVisible(false);
        maskColorCheckBox.setVisible(false);
    }

    private void makeControlsVisible(Control... controls) {
        for (Control control : controls) {
            control.setVisible(true);
        }
    }
}
