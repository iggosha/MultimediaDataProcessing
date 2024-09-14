package ru.golovkov.multimediadataprocessing;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
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

    private Image originalImage;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        transformationComboBox.getItems().addAll("Негатив", "Другое преобразование");
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

        Image transformedImage = null;
        ImageWrapper originalImageWrapper = new ImageWrapper(originalImage);

        switch (selectedTransformation) {
            case "Негатив" -> transformedImage = createNewNegativeImage(originalImageWrapper);

        }
        if (transformedImage != null) {
            transformedImageView.setImage(transformedImage);
        }
    }

    private Image createNewNegativeImage(ImageWrapper imageWrapper) {
        WritableImage writableImage = new WritableImage(imageWrapper.getWidth(), imageWrapper.getHeight());
        PixelWriter pixelWriter = writableImage.getPixelWriter();
        for (int y = 0; y < imageWrapper.getHeight(); y++) {
            for (int x = 0; x < imageWrapper.getHeight(); x++) {
                int argb = imageWrapper.getPixelReader().getArgb(x, y);
                int a = (argb >> 24) & 0xff;
                int r = 255 - ((argb >> 16) & 0xff);
                int g = 255 - ((argb >> 8) & 0xff);
                int b = 255 - (argb & 0xff);
                int newArgb = (a << 24) | (r << 16) | (g << 8) | b;
                pixelWriter.setArgb(x, y, newArgb);
            }
        }
        return writableImage;
    }
}
