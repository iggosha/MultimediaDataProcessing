package ru.golovkov.multimediadataprocessing;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
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
                "Линейный сглаживающий (усредняющий) фильтр",
                "Медианный фильтр",
                "Градиент Робертса",
                "Градиент Собеля",
                "Лапласиан 90",
                "Лапласиан 45"
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
    }

    private void applyTransformation(String selectedTransformation) {
        switch (selectedTransformation) {
            case "Негатив" -> transformedImage = imageTransformer.createNegativeImage(originalImageWrapper);
            case "Степенное преобразование" -> {
                makeSlidersVisible(gammaSlider);
                transformedImage = imageTransformer.createPowerLawTransformation(originalImageWrapper,gammaSlider.getValue());
            }
            case "Вырезание диапазона яркостей" -> {
                makeSlidersVisible(minBrightnessSlider, maxBrightnessSlider);
                transformedImage = imageTransformer.createBrightnessRangeCut(originalImageWrapper,
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
    }

    private void makeSlidersVisible(Slider... sliders) {
        for (Slider slider: sliders) {
            slider.setVisible(true);
        }
    }
}
