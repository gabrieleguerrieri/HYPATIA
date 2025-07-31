package it.unicam.cs.pg.gui;
import it.unicam.cs.pg.processing.ImageUtils;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.BorderPane;
import javafx.scene.image.WritableImage;
import javafx.scene.control.ScrollPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

/**
 * Interfaccia grafica per il preprocessing e l'elaborazione di immagini
 * utilizzando modelli DL4J per la rimozione dello sfondo e la super risoluzione.
 */
public class PreprocessingDl4jGui extends Application {

    private static final String PROJECT_ROOT = System.getProperty("user.dir");
    private static final String OUTPUT_BG_DIR = PROJECT_ROOT + "\\output_images\\background-remove";
    private static final String OUTPUT_DENOISE_DIR = PROJECT_ROOT + "\\output_images\\denoising";
    private static final String OUTPUT_SUPER_RES_DIR = PROJECT_ROOT + "\\output_images\\super-resolution";
    private static final String OUTPUT_NO_FILTER_DIR = PROJECT_ROOT + "\\output_images\\filtered";

    private boolean isDenoisingModel = false;

    private TextArea areaLog;
    private ComboBox<String> processingTypeComboBox;
    private FileChooser fileChooser;
    private MultiLayerNetwork modello;
    private MultiLayerNetwork modelloSuperRis;
    private Mat originalImage;

    /**
     * Avvia l'applicazione GUI.
     *
     * @param primaryStage finestra principale JavaFX
     */
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Preprocessing e Elaborazione Immagini con DL4J e OpenCV");

        fileChooser = new FileChooser();
        File projectRootDir = new File(PROJECT_ROOT);
        if (projectRootDir.exists() && projectRootDir.isDirectory()) {
            fileChooser.setInitialDirectory(projectRootDir);
        }

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10));
        grid.setVgap(8);
        grid.setHgap(10);

        // === BLOCCO 1: Carica immagine ===
        HBox hBoxCaricaImg = new HBox(10);
        Button btnCaricaImmagine = new Button("Carica Immagine");
        btnCaricaImmagine.setPrefWidth(280);
        btnCaricaImmagine.setOnAction(e -> caricaImmagine(primaryStage));
        Label lblCaricaImg = new Label("Seleziona un file immagine da elaborare.");
        lblCaricaImg.setWrapText(true);
        lblCaricaImg.setMaxWidth(280);
        hBoxCaricaImg.getChildren().addAll(btnCaricaImmagine, lblCaricaImg);
        grid.add(hBoxCaricaImg, 0, 0, 2, 1);

        // === BLOCCO 2: Rimozione sfondo/denoising ===
        HBox hBoxCaricaModello = new HBox(10);
        hBoxCaricaModello.setPadding(new Insets(20, 0, 0, 0));
        Button btnCaricaModelloRimozione = new Button("Carica Modello Rimozione Sfondo / Denoising");
        btnCaricaModelloRimozione.setPrefWidth(280);
        btnCaricaModelloRimozione.setOnAction(e -> caricaModello(primaryStage));
        Label lblCaricaModello = new Label("Carica il modello per rimuovere sfondo o rumore.");
        lblCaricaModello.setWrapText(true);
        lblCaricaModello.setMaxWidth(280);
        hBoxCaricaModello.getChildren().addAll(btnCaricaModelloRimozione, lblCaricaModello);
        grid.add(hBoxCaricaModello, 0, 1, 2, 1);

        HBox hBoxApplicaRimozione = new HBox(10);
        Button btnApplicaRimozioneSfondo = new Button("Applica Rimozione Sfondo / Denoising");
        btnApplicaRimozioneSfondo.setPrefWidth(280);
        btnApplicaRimozioneSfondo.setOnAction(e -> applicaRimozioneSfondoODenoisingConPatch());
        Label lblApplicaRimozione = new Label("Applica la rimozione dello sfondo o del rumore usando patch.");
        lblApplicaRimozione.setWrapText(true);
        lblApplicaRimozione.setMaxWidth(280);
        hBoxApplicaRimozione.getChildren().addAll(btnApplicaRimozioneSfondo, lblApplicaRimozione);
        grid.add(hBoxApplicaRimozione, 0, 2, 2, 1);

        // === BLOCCO 3: Super Risoluzione ===
        HBox hBoxCaricaSR = new HBox(10);
        hBoxCaricaSR.setPadding(new Insets(20, 0, 0, 0));
        Button btnCaricaModelloSuperRis = new Button("Carica Modello Super Risoluzione");
        btnCaricaModelloSuperRis.setPrefWidth(280);
        btnCaricaModelloSuperRis.setOnAction(e -> caricaModelloSuperRisoluzione(primaryStage));
        Label lblCaricaSR = new Label("Carica il modello per aumentare la risoluzione.");
        lblCaricaSR.setWrapText(true);
        lblCaricaSR.setMaxWidth(280);
        hBoxCaricaSR.getChildren().addAll(btnCaricaModelloSuperRis, lblCaricaSR);
        grid.add(hBoxCaricaSR, 0, 3, 2, 1);

        HBox hBoxApplicaSR = new HBox(10);
        Button btnApplicaSuperRis = new Button("Applica Super Risoluzione");
        btnApplicaSuperRis.setPrefWidth(280);
        btnApplicaSuperRis.setOnAction(e -> applicaSuperRisoluzioneConPatch());
        Label lblApplicaSR = new Label("Applica la super risoluzione all'immagine usando patch.");
        lblApplicaSR.setWrapText(true);
        lblApplicaSR.setMaxWidth(280);
        hBoxApplicaSR.getChildren().addAll(btnApplicaSuperRis, lblApplicaSR);
        grid.add(hBoxApplicaSR, 0, 4, 2, 1);

        // === BLOCCO 4: Filtri ===
        HBox hBoxFiltroCombo = new HBox(10);
        hBoxFiltroCombo.setPadding(new Insets(20, 0, 0, 0));
        ComboBox<String> filtroComboBox = new ComboBox<>();
        filtroComboBox.getItems().addAll("Filtro Gaussiano", "Filtro Mediano", "Edge Detection", "Adaptive Thresholding");
        filtroComboBox.getSelectionModel().selectFirst();
        filtroComboBox.setPrefWidth(280);
        Label lblFiltro = new Label("Scegli un filtro OpenCV da applicare.");
        lblFiltro.setWrapText(true);
        lblFiltro.setMaxWidth(280);
        hBoxFiltroCombo.getChildren().addAll(filtroComboBox, lblFiltro);
        grid.add(hBoxFiltroCombo, 0, 5, 2, 1);

        HBox hBoxApplicaFiltro = new HBox(10);
        Button btnApplicaFiltro = new Button("Applica Filtro Selezionato");
        btnApplicaFiltro.setPrefWidth(280);
        btnApplicaFiltro.setOnAction(e -> applicaFiltro(filtroComboBox.getValue()));
        Label lblApplicaFiltro = new Label("Applica il filtro selezionato sull'immagine.");
        lblApplicaFiltro.setWrapText(true);
        lblApplicaFiltro.setMaxWidth(280);
        hBoxApplicaFiltro.getChildren().addAll(btnApplicaFiltro, lblApplicaFiltro);
        grid.add(hBoxApplicaFiltro, 0, 6, 2, 1);

        // === LOG e OUTPUT ===
        areaLog = new TextArea();
        areaLog.setEditable(false);
        areaLog.setWrapText(true);
        areaLog.setPrefHeight(150);
        grid.add(areaLog, 0, 7, 2, 1);

        HBox hBoxClear = new HBox(10);
        Button btnClearOutput = new Button("Cancella Output Immagini");
        btnClearOutput.setPrefWidth(280);
        btnClearOutput.setOnAction(e -> clearOutputImages());
        Label lblClear = new Label("Elimina le immagini generate precedentemente.");
        lblClear.setWrapText(true);
        lblClear.setMaxWidth(280);
        hBoxClear.getChildren().addAll(btnClearOutput, lblClear);
        grid.add(hBoxClear, 0, 8, 2, 1);

        Scene scene = new Scene(grid, 600, 600);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    /**
     * Applica la super risoluzione all'immagine caricata suddividendola in patch,
     * utilizzando il modello DL4J di super risoluzione.
     */
    private void applicaSuperRisoluzioneConPatch() {
        if (modelloSuperRis == null) {
            areaLog.appendText("Caricare prima un modello di super risoluzione.\n");
            return;
        }
        if (originalImage == null || originalImage.empty()) {
            areaLog.appendText("Caricare prima un'immagine.\n");
            return;
        }

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                long startTime = System.nanoTime();  // Inizio misurazione

                int patchSize = 100;
                int upscale = 4;

                int width = originalImage.cols();
                int height = originalImage.rows();
                int finalWidth = width * upscale;
                int finalHeight = height * upscale;

                Mat finalImage = new Mat(finalHeight, finalWidth, originalImage.type());
                ImagePreProcessingScaler scaler = new ImagePreProcessingScaler(-1, 1);

                for (int y = 0; y + patchSize <= height; y += patchSize) {
                    for (int x = 0; x + patchSize <= width; x += patchSize) {
                        Mat patch = new Mat(originalImage, new org.opencv.core.Rect(x, y, patchSize, patchSize));
                        INDArray input = convertMatToINDArray(patch);
                        scaler.transform(input);
                        INDArray output = modelloSuperRis.output(input);
                        Mat superResolvedPatch = convertINDArrayToMat(output);

                        int targetX = x * upscale;
                        int targetY = y * upscale;
                        superResolvedPatch.copyTo(finalImage.submat(new org.opencv.core.Rect(targetX, targetY, 400, 400)));
                    }
                }

                File outputDir = new File(OUTPUT_SUPER_RES_DIR);
                if (!outputDir.exists()) {
                    outputDir.mkdirs();
                }

                String filename = "superres_" + System.currentTimeMillis() + ".png";
                String outputPath = OUTPUT_SUPER_RES_DIR + "\\" + filename;

                Imgcodecs.imwrite(outputPath, finalImage);

                long endTime = System.nanoTime();  // Fine misurazione
                double elapsedSeconds = (endTime - startTime) / 1_000_000_000.0;
                areaLog.appendText(String.format("⏱ Tempo elaborazione: %.3f secondi\n", elapsedSeconds));

                return outputPath;
            }


        };

        task.setOnSucceeded(e -> {
            String path = task.getValue();
            areaLog.appendText("Super risoluzione patch completata. Immagine salvata: " + path + "\n");
            mostraImmagine(path, "Super Risoluzione (completo)");
        });

        task.setOnFailed(e -> areaLog.appendText("Errore durante super risoluzione: " + task.getException().getMessage() + "\n"));

        new Thread(task).start();
    }

    /**
     * Applica la rimozione dello sfondo o il denoising all'immagine caricata suddividendola in patch,
     * utilizzando il modello DL4J corrispondente.
     */
    private void applicaRimozioneSfondoODenoisingConPatch() {
        if (modello == null) {
            areaLog.appendText("Caricare prima un modello di rimozione sfondo o denoising.\n");
            return;
        }
        if (originalImage == null || originalImage.empty()) {
            areaLog.appendText("Caricare prima un'immagine.\n");
            return;
        }

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                long startTime = System.nanoTime();  // Inizio misurazione

                int patchSize = 100;
                int width = originalImage.cols();
                int height = originalImage.rows();
                Mat finalImage = new Mat(height, width, originalImage.type());
                ImagePreProcessingScaler scaler = new ImagePreProcessingScaler(-1, 1);

                for (int y = 0; y + patchSize <= height; y += patchSize) {
                    for (int x = 0; x + patchSize <= width; x += patchSize) {
                        Mat patch = new Mat(originalImage, new org.opencv.core.Rect(x, y, patchSize, patchSize));
                        INDArray input = convertMatToINDArray(patch);
                        scaler.transform(input);
                        INDArray output = modello.output(input);
                        Mat resultPatch = convertINDArrayToMat(output);
                        resultPatch.copyTo(finalImage.submat(new org.opencv.core.Rect(x, y, patchSize, patchSize)));
                    }
                }

                String baseDir = isDenoisingModel ? OUTPUT_DENOISE_DIR : OUTPUT_BG_DIR;
                File outputDir = new File(baseDir);
                if (!outputDir.exists()) outputDir.mkdirs();

                String prefix = isDenoisingModel ? "denoised_" : "background_removed_";
                String filename = prefix + System.currentTimeMillis() + ".png";
                String outputPath = baseDir + "\\" + filename;

                Imgcodecs.imwrite(outputPath, finalImage);

                long endTime = System.nanoTime();  // ⏱️ Fine misurazione
                double elapsedSeconds = (endTime - startTime) / 1_000_000_000.0;
                areaLog.appendText(String.format("⏱ Tempo elaborazione: %.3f secondi\n", elapsedSeconds));

                return outputPath;
            }

        };

        task.setOnSucceeded(e -> {
            String path = task.getValue();
            String tipo = isDenoisingModel ? "Denoising" : "Rimozione Sfondo";
            areaLog.appendText(tipo + " completato. Immagine salvata: " + path + "\n");
            mostraImmagine(path, tipo + " (completo)");
        });

        task.setOnFailed(e -> {
            String tipo = isDenoisingModel ? "denoising" : "rimozione sfondo";
            areaLog.appendText("Errore durante la " + tipo + ": " + task.getException().getMessage() + "\n");
        });

        new Thread(task).start();
    }

    /**
     * Mostra un'immagine in una nuova finestra con titolo specificato.
     *
     * @param path percorso del file immagine da visualizzare
     * @param titolo titolo della finestra di visualizzazione
     */
    private void mostraImmagine(String path, String titolo) {
        Stage stage = new Stage();
        stage.setTitle(titolo);
        ImageView iv = new ImageView(new Image(new File(path).toURI().toString()));
        ScrollPane sp = new ScrollPane(iv);
        Scene scene = new Scene(sp, 800, 600);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Carica il modello DL4J per la rimozione dello sfondo o il denoising.
     * Il tipo viene determinato automaticamente dal nome del file.
     *
     * @param stage finestra corrente per il FileChooser
     */
    private void caricaModello(Stage stage) {
        fileChooser.setTitle("Seleziona Modello DL4J");
        fileChooser.getExtensionFilters().setAll(new FileChooser.ExtensionFilter("Modelli DL4J", "*.zip"));
        File modelFile = fileChooser.showOpenDialog(stage);
        if (modelFile != null) {
            try {
                modello = MultiLayerNetwork.load(modelFile, true);

                // Determina automaticamente il tipo di modello in base al nome file
                String nomeFile = modelFile.getName().toLowerCase();
                if (nomeFile.contains("modello_sfondo")) {
                    isDenoisingModel = false; // rimozione sfondo
                } else if (nomeFile.contains("modello_denoising")) {
                    isDenoisingModel = true;  // denoising
                } else {
                    // Default: se non contiene nessuna delle due parole chiave, assumiamo rimozione sfondo
                    isDenoisingModel = false;
                }

                String tipoModello = isDenoisingModel ? "Denoising" : "Rimozione Sfondo";
                areaLog.appendText("Modello caricato: " + modelFile.getName() + " (" + tipoModello + ")\n");

            } catch (Exception ex) {
                areaLog.appendText("Errore caricamento modello: " + ex.getMessage() + "\n");
            }
        }
    }

    /**
     * Carica il modello DL4J per la super risoluzione.
     *
     * @param stage finestra corrente per il FileChooser
     */
    private void caricaModelloSuperRisoluzione(Stage stage) {
        fileChooser.setTitle("Seleziona Modello Super Risoluzione DL4J");
        fileChooser.getExtensionFilters().setAll(new FileChooser.ExtensionFilter("Modelli DL4J", "*.zip"));
        File modelFile = fileChooser.showOpenDialog(stage);
        if (modelFile != null) {
            try {
                modelloSuperRis = MultiLayerNetwork.load(modelFile, true);
                areaLog.appendText("Modello super risoluzione caricato: " + modelFile.getName() + "\n");
            } catch (Exception ex) {
                areaLog.appendText("Errore caricamento super risoluzione: " + ex.getMessage() + "\n");
            }
        }
    }

    /**
     * Applica il filtro OpenCV selezionato all'immagine e salva il risultato,
     * registrando nel log anche il tempo di elaborazione.
     *
     * @param filtro nome del filtro da applicare
     */
    private void applicaFiltro(String filtro) {
        if (originalImage == null || originalImage.empty()) {
            areaLog.appendText("Caricare prima un'immagine.\n");
            return;
        }

        long startTime = System.nanoTime(); // ⏱️ Inizio misurazione

        Mat filtered = originalImage.clone();

        switch (filtro) {
            case "Filtro Gaussiano":
                Imgproc.GaussianBlur(filtered, filtered, new Size(5, 5), 0);
                break;
            case "Filtro Mediano":
                Imgproc.medianBlur(filtered, filtered, 5);
                break;
            case "Edge Detection":
                Imgproc.cvtColor(filtered, filtered, Imgproc.COLOR_BGR2GRAY);
                Imgproc.Canny(filtered, filtered, 100, 200);
                Imgproc.cvtColor(filtered, filtered, Imgproc.COLOR_GRAY2BGR);
                break;
            case "Adaptive Thresholding":
                Imgproc.cvtColor(filtered, filtered, Imgproc.COLOR_BGR2GRAY);
                Imgproc.adaptiveThreshold(filtered, filtered, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C,
                        Imgproc.THRESH_BINARY, 11, 2);
                Imgproc.cvtColor(filtered, filtered, Imgproc.COLOR_GRAY2BGR);
                break;
            default:
                areaLog.appendText("Filtro non valido.\n");
                return;
        }

        File outputDir = new File(OUTPUT_NO_FILTER_DIR);
        if (!outputDir.exists()) outputDir.mkdirs();

        String outputPath = OUTPUT_NO_FILTER_DIR + "\\filtered_" + filtro.replace(" ", "_") + "_" + System.currentTimeMillis() + ".png";
        Imgcodecs.imwrite(outputPath, filtered);

        long endTime = System.nanoTime(); // Fine misurazione
        double elapsedSeconds = (endTime - startTime) / 1_000_000_000.0;

        // Log in stile modelli
        areaLog.appendText(String.format("⏱ Tempo elaborazione: %.3f secondi\n", elapsedSeconds));
        areaLog.appendText(String.format("Filtro '%s' applicato. Immagine salvata: %s\n", filtro, outputPath));

        mostraImmagine(outputPath, "Filtro: " + filtro);
    }

    /**
     * Carica un'immagine da file.
     *
     * @param stage finestra corrente per il FileChooser
     */
    private void caricaImmagine(Stage stage) {
        fileChooser.setTitle("Seleziona Immagine");
        fileChooser.getExtensionFilters().setAll(new FileChooser.ExtensionFilter("Immagini", "*.jpg", "*.png", "*.jpeg"));
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            originalImage = ImageUtils.loadImage(file.getAbsolutePath());
            if (originalImage.empty()) {
                areaLog.appendText("Errore nel caricamento dell'immagine.\n");
                return;
            }
            areaLog.appendText("Immagine caricata: " + file.getName() + "\n");
        }
    }

    /**
     * Cancella tutte le immagini presenti nelle cartelle di output:
     * rimozione sfondo, denoising, super risoluzione e filtri.
     */
    private void clearOutputImages() {
        String[] outputDirs = {
                OUTPUT_BG_DIR,
                OUTPUT_DENOISE_DIR,
                OUTPUT_SUPER_RES_DIR,
                OUTPUT_NO_FILTER_DIR
        };

        int totalDeleted = 0;

        for (String dirPath : outputDirs) {
            File dir = new File(dirPath);
            if (dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile() && file.delete()) {
                            totalDeleted++;
                        }
                    }
                }
            }
        }

        areaLog.appendText("Cancellate " + totalDeleted + " immagini da tutte le cartelle di output.\n");
    }

    /**
     * Converte una Mat di OpenCV in un INDArray utilizzabile dal modello DL4J.
     *
     * @param mat immagine in formato Mat di OpenCV
     * @return INDArray corrispondente per input al modello DL4J
     * @throws IOException se la conversione fallisce
     */
    private INDArray convertMatToINDArray(Mat mat) throws IOException {
        org.datavec.image.loader.NativeImageLoader loader =
                new org.datavec.image.loader.NativeImageLoader((int) mat.size().height, (int) mat.size().width, mat.channels());
        return loader.asMatrix(mat);
    }

    /**
     * Converte un INDArray di output del modello DL4J in un'immagine Mat di OpenCV.
     *
     * @param output INDArray prodotto dal modello DL4J
     * @return immagine Mat convertita dal modello
     */
    private Mat convertINDArrayToMat(INDArray output) {
        int height = (int) output.size(2);
        int width = (int) output.size(3);
        int channels = (int) output.size(1);

        Mat mat = new Mat(height, width, channels == 1 ? CvType.CV_8UC1 : CvType.CV_8UC3);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (channels == 1) {
                    float v = output.getFloat(0, 0, y, x);  // v ∈ [-1, 1]
                    int pixel = Math.round((v + 1f) * 127.5f);  // --> [0, 255]
                    mat.put(y, x, pixel);
                } else if (channels == 3) {
                    float r = output.getFloat(0, 0, y, x);
                    float g = output.getFloat(0, 1, y, x);
                    float b = output.getFloat(0, 2, y, x);

                    int ir = Math.round((r + 1f) * 127.5f);
                    int ig = Math.round((g + 1f) * 127.5f);
                    int ib = Math.round((b + 1f) * 127.5f);

                    mat.put(y, x, ir, ig, ib);


                }
            }
        }

        return mat;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
