package it.unicam.cs.pg.modelTess4JTraining;

import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe per la modifica e correzione delle bounding box in file .box associati a immagini
 * Permette operazioni di ridimensionamento, eliminazione e salvataggio delle modifiche
 */
public class BoxCleaner {
    private List<String> boxFileLines;
    private String boxFilePath;
    private String imagePath;
    private List<Rectangle> wordStrBoxes;
    private int selectedBoxIndex = -1;
    private ImageView imageView;
    private ResizeEdge selectedEdge = ResizeEdge.NONE;
    private Point2D dragStartPoint;

    private enum ResizeEdge {
        LEFT, RIGHT, TOP, BOTTOM, NONE
    }

    /**
     * Avvia l'interfaccia grafica per la modifica delle bounding box
     * @param imagePath percorso dell'immagine da elaborare
     * @param boxFilePath percorso del file .box da modificare
     */
    public void boxCleaner(String imagePath, String boxFilePath) {
        this.imagePath = imagePath;
        this.boxFilePath = boxFilePath;
        Stage viewerStage = new Stage();
        viewerStage.setTitle("Box Cleaner");

        boxFileLines = new ArrayList<>();
        wordStrBoxes = new ArrayList<>();

        imageView = new ImageView(loadImageWithBoxes());

        ScrollPane scrollPane = new ScrollPane(imageView);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        Button saveButton = new Button("Salva Modifiche");
        Button deleteButton = new Button("Elimina Box");
        saveButton.setOnAction(event -> saveBoxFile());
        deleteButton.setOnAction(event -> deleteSelectedBox());

        HBox controlPanel = new HBox(10);
        controlPanel.setPadding(new Insets(10));
        controlPanel.getChildren().addAll(saveButton, deleteButton);

        BorderPane root = new BorderPane();
        root.setTop(controlPanel);
        root.setCenter(scrollPane);

        setupMouseHandlers();
        Scene scene = new Scene(root, 800, 600);
        setupKeyboardHandler(scene);

        viewerStage.setScene(scene);
        viewerStage.show();
    }

    /**
     * Carica l'immagine e disegna le bounding box presenti nel file
     * @return Image con le box disegnate
     */
    private Image loadImageWithBoxes() {
        Mat image = Imgcodecs.imread(imagePath);
        if (image.empty()) {
            showAlert(Alert.AlertType.ERROR, "Errore", "Impossibile caricare l'immagine!");
            return null;
        }

        readBoxFile();
        drawBoxesOnImage(image);
        return convertMatToImage(image);
    }

    /**
     * Legge e parserizza il file .box estraendo le coordinate delle bounding box
     */
    private void readBoxFile() {
        try (BufferedReader br = new BufferedReader(new FileReader(boxFilePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                boxFileLines.add(line);
                if (line.startsWith("WordStr")) {
                    String[] parts = line.split(" ");
                    if (parts.length >= 5) {
                        int x1 = Integer.parseInt(parts[1]);
                        int y1 = Integer.parseInt(parts[2]);
                        int x2 = Integer.parseInt(parts[3]);
                        int y2 = Integer.parseInt(parts[4]);

                        if (y1 != y2) {
                            wordStrBoxes.add(new Rectangle(x1, y1, x2 - x1, y2 - y1));
                        }
                    }
                }
            }
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Errore", "Lettura file .box fallita: " + e.getMessage());
        }
    }

    /**
     * Configura gli handler per gli eventi del mouse
     */
    private void setupMouseHandlers() {
        imageView.setOnMouseMoved(event -> {
            if (selectedBoxIndex == -1) return;

            Rectangle box = wordStrBoxes.get(selectedBoxIndex);
            Point2D mousePoint = new Point2D(event.getX(), event.getY());

            selectedEdge = detectEdgeNearMouse(box, mousePoint);
            updateCursor(selectedEdge);
        });

        imageView.setOnMousePressed(event -> {
            if (selectedEdge == ResizeEdge.NONE) {
                selectBox(event.getX(), event.getY());
            }

            if (selectedBoxIndex != -1 && selectedEdge != ResizeEdge.NONE) {
                dragStartPoint = new Point2D(event.getX(), event.getY());
            }
        });

        imageView.setOnMouseDragged(event -> {
            if (dragStartPoint == null || selectedBoxIndex == -1) return;

            Rectangle box = wordStrBoxes.get(selectedBoxIndex);
            double deltaX = event.getX() - dragStartPoint.getX();
            double deltaY = event.getY() - dragStartPoint.getY();

            switch (selectedEdge) {
                case LEFT:
                    box.x += (int) deltaX;
                    box.width -= (int) deltaX;
                    break;
                case RIGHT:
                    box.width += (int) deltaX;
                    break;
                case TOP:
                    box.height -= (int) deltaY;
                    break;
                case BOTTOM:
                    box.y -= (int) deltaY;
                    box.height += (int) deltaY;
                    break;
                default:
                    return;
            }

            box.width = Math.max(1, box.width);
            box.height = Math.max(1, box.height);

            dragStartPoint = new Point2D(event.getX(), event.getY());
            updateBoxFileLine(selectedBoxIndex);
            redrawImage();
        });

        imageView.setOnMouseReleased(event -> {
            dragStartPoint = null;
            selectedEdge = ResizeEdge.NONE;
            imageView.setCursor(Cursor.DEFAULT);
        });
    }

    /**
     * Rileva il bordo più vicino al cursore per il ridimensionamento
     * @param box rettangolo da controllare
     * @param mousePoint posizione del mouse
     * @return bordo rilevato
     */
    private ResizeEdge detectEdgeNearMouse(Rectangle box, Point2D mousePoint) {
        final int edgeThreshold = 5;
        int imageHeight = (int) imageView.getImage().getHeight();

        int correctedY1 = imageHeight - box.y - box.height;
        int correctedY2 = imageHeight - box.y;

        boolean nearLeft = Math.abs(mousePoint.getX() - box.x) < edgeThreshold;
        boolean nearRight = Math.abs(mousePoint.getX() - (box.x + box.width)) < edgeThreshold;
        boolean nearTop = Math.abs(mousePoint.getY() - correctedY1) < edgeThreshold;
        boolean nearBottom = Math.abs(mousePoint.getY() - correctedY2) < edgeThreshold;

        if (nearLeft) return ResizeEdge.LEFT;
        if (nearRight) return ResizeEdge.RIGHT;
        if (nearTop) return ResizeEdge.TOP;
        if (nearBottom) return ResizeEdge.BOTTOM;
        return ResizeEdge.NONE;
    }

    /**
     * Aggiorna il cursore in base al bordo selezionato
     * @param edge bordo attivo
     */
    private void updateCursor(ResizeEdge edge) {
        switch (edge) {
            case LEFT:
            case RIGHT:
                imageView.setCursor(Cursor.H_RESIZE);
                break;
            case TOP:
            case BOTTOM:
                imageView.setCursor(Cursor.V_RESIZE);
                break;
            default:
                imageView.setCursor(Cursor.DEFAULT);
        }
    }

    /**
     * Seleziona una box in base alla posizione del mouse
     * @param mouseX coordinata X del mouse
     * @param mouseY coordinata Y del mouse
     */
    private void selectBox(double mouseX, double mouseY) {
        int imageHeight = (int) imageView.getImage().getHeight();

        for (int i = 0; i < wordStrBoxes.size(); i++) {
            Rectangle box = wordStrBoxes.get(i);
            int correctedY1 = imageHeight - box.y - box.height;
            int correctedY2 = imageHeight - box.y;

            if (mouseX >= box.x && mouseX <= box.x + box.width &&
                    mouseY >= correctedY1 && mouseY <= correctedY2) {
                selectedBoxIndex = i;
                redrawImage();
                return;
            }
        }
        selectedBoxIndex = -1;
        redrawImage();
    }

    /**
     * Configura gli shortcut da tastiera
     * @param scene scena principale
     */
    private void setupKeyboardHandler(Scene scene) {
        scene.setOnKeyPressed(event -> {
            if (selectedBoxIndex == -1) return;

            if (event.getCode() == javafx.scene.input.KeyCode.DELETE) {
                deleteSelectedBox();
            }
        });
    }

    /**
     * Elimina la box selezionata
     */
    private void deleteSelectedBox() {
        if (selectedBoxIndex == -1) return;

        int lineIndex = selectedBoxIndex * 2;
        if (lineIndex < boxFileLines.size() - 1) {
            boxFileLines.remove(lineIndex);
            boxFileLines.remove(lineIndex);
            wordStrBoxes.remove(selectedBoxIndex);
            selectedBoxIndex = -1;
            redrawImage();
        }
    }

    /**
     * Ridisegna l'immagine con le box aggiornate
     */
    private void redrawImage() {
        Mat image = Imgcodecs.imread(imagePath);
        if (image.empty()) return;

        drawBoxesOnImage(image);
        imageView.setImage(convertMatToImage(image));
    }

    /**
     * Disegna tutte le box sull'immagine
     * @param image matrice OpenCV su cui disegnare
     */
    private void drawBoxesOnImage(Mat image) {
        for (int i = 0; i < wordStrBoxes.size(); i++) {
            Rectangle box = wordStrBoxes.get(i);
            Scalar color = (i == selectedBoxIndex) ? new Scalar(0, 0, 255) : new Scalar(0, 255, 0);

            Imgproc.rectangle(image,
                    new Point(box.x, image.rows() - box.y - box.height),
                    new Point(box.x + box.width, image.rows() - box.y),
                    color, 2);
        }
    }

    /**
     * Aggiorna le coordinate nel file .box dopo una modifica
     * @param boxIndex indice della box modificata
     */
    private void updateBoxFileLine(int boxIndex) {
        int coordinateLineIndex = boxIndex * 2;

        if (coordinateLineIndex >= boxFileLines.size()) return;

        String originalCoordinateLine = boxFileLines.get(coordinateLineIndex);
        String[] parts = originalCoordinateLine.split(" ", 6);
        String text = (parts.length >= 6) ? parts[5] : "";

        Rectangle box = wordStrBoxes.get(boxIndex);

        String newCoordinateLine = String.format("WordStr %d %d %d %d %s",
                box.x,
                box.y,
                box.x + box.width,
                box.y + box.height,
                text.trim()
        );

        boxFileLines.set(coordinateLineIndex, newCoordinateLine);
    }

    /**
     * Converte una matrice OpenCV in un'immagine JavaFX
     * @param mat matrice da convertire
     * @return Image convertita
     */
    private Image convertMatToImage(Mat mat) {
        MatOfByte mob = new MatOfByte();
        Imgcodecs.imencode(".png", mat, mob);
        byte[] byteArray = mob.toArray();
        return new Image(new ByteArrayInputStream(byteArray));
    }

    /**
     * Salva le modifiche nel file .box
     */
    private void saveBoxFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(boxFilePath))) {
            for (String line : boxFileLines) {
                writer.write(line);
                writer.newLine();
            }
            showAlert(Alert.AlertType.INFORMATION, "Successo", "File salvato correttamente!");
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Errore", "Salvataggio fallito: " + e.getMessage());
        }
    }

    /**
     * Mostra un alert all'utente
     * @param type tipo di alert
     * @param title titolo della finestra
     * @param message messaggio da visualizzare
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}