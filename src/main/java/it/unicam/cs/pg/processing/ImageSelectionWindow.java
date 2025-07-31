package it.unicam.cs.pg.processing;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.scene.SnapshotParameters;
import org.opencv.core.Mat;
import java.io.File;
import java.util.function.Consumer;

/**
 * Finestra per la selezione di una regione rettangolare in un'immagine.
 * Permette all'utente di selezionare un'area specifica dell'immagine e
 * di esportare la regione selezionata.
 */
public class ImageSelectionWindow {
    private Rectangle selectionRect;
    private ImageView imageView;

    /**
     * Mostra la finestra di selezione dell'immagine.
     *
     * @param imageFile File immagine da elaborare
     * @param onSaveCallback Callback chiamato con la regione selezionata
     */
    public void show(File imageFile, Consumer<Mat> onSaveCallback) {
        Stage imageStage = new Stage();
        imageStage.setTitle("Seleziona Porzione");

        // Inizializzazione componenti grafici
        imageView = new ImageView();
        Image image = new Image(imageFile.toURI().toString());
        imageView.setImage(image);

        Pane pane = new Pane(imageView);
        pane.setMinSize(image.getWidth(), image.getHeight());
        pane.setPrefSize(image.getWidth(), image.getHeight());

        // Configurazione area scrollabile
        ScrollPane scrollPane = new ScrollPane(pane);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setFitToWidth(false);
        scrollPane.setFitToHeight(false);

        // Inizializzazione rettangolo di selezione
        selectionRect = new Rectangle();
        selectionRect.setFill(Color.TRANSPARENT);
        selectionRect.setStroke(Color.BLUE);
        selectionRect.setStrokeWidth(2);
        pane.getChildren().add(selectionRect);

        // Gestione eventi del mouse
        final boolean[] isDrawing = {false};
        final double[] startX = {0};
        final double[] startY = {0};

        pane.setOnMousePressed(event -> {
            isDrawing[0] = true;
            startX[0] = event.getX();
            startY[0] = event.getY();
            selectionRect.setX(startX[0]);
            selectionRect.setY(startY[0]);
            selectionRect.setWidth(0);
            selectionRect.setHeight(0);
        });

        pane.setOnMouseDragged(event -> {
            if (isDrawing[0]) {
                double currentX = event.getX();
                double currentY = event.getY();
                // Aggiorna dimensioni e posizione del rettangolo
                selectionRect.setWidth(Math.abs(currentX - startX[0]));
                selectionRect.setHeight(Math.abs(currentY - startY[0]));
                selectionRect.setX(Math.min(startX[0], currentX));
                selectionRect.setY(Math.min(startY[0], currentY));
            }
        });

        pane.setOnMouseReleased(event -> isDrawing[0] = false);

        // Pulsanti di controllo
        Button saveSelectionButton = new Button("Salva Selezione");
        saveSelectionButton.setPrefWidth(150);
        saveSelectionButton.setOnAction(event -> {
            Mat region = captureSelection();
            if (region != null && !region.empty()) {
                onSaveCallback.accept(region);
            }
        });

        Button closeButton = new Button("Chiudi");
        closeButton.setPrefWidth(150);
        closeButton.setOnAction(event -> imageStage.close());

        // Pannello comandi
        HBox controlPanel = new HBox(10, saveSelectionButton, closeButton);
        controlPanel.setPadding(new Insets(10));

        BorderPane root = new BorderPane();
        root.setTop(controlPanel);
        root.setCenter(scrollPane);

        Scene imageScene = new Scene(root, 800, 600);
        imageStage.setScene(imageScene);
        imageStage.show();
    }

    /**
     * Cattura la regione selezionata e la converte in formato Mat.
     *
     * @return Mat contenente la regione selezionata, null se nessuna selezione valida
     */
    private Mat captureSelection() {
        if (selectionRect.getWidth() > 0 && selectionRect.getHeight() > 0) {
            SnapshotParameters params = new SnapshotParameters();
            params.setViewport(new javafx.geometry.Rectangle2D(
                    selectionRect.getX(), selectionRect.getY(),
                    selectionRect.getWidth(), selectionRect.getHeight()
            ));
            // Acquisisci snapshot dell'area selezionata
            WritableImage writableImage = imageView.snapshot(params, null);
            return ImageUtils.convertFXImageToMat(writableImage);
        }
        return null;
    }
}