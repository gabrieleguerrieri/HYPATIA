package it.unicam.cs.pg.gui;

import it.unicam.cs.pg.postprocessing.TextCorrector;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;

import java.io.*;
import java.nio.file.Files;

/**
 * Classe principale per l'applicazione di correzione lessicale avanzata.
 */
public class TextCorrectionGui extends Application {
    private String selectedModelPath;
    private String selectedFilePath;
    private WordVectors wordVectors;
    private final int MAX_EDIT_DISTANCE = 2;
    private static final String PROJECT_ROOT = System.getProperty("user.dir"); // Ottieni la root del progetto
    private TextCorrector textCorrector;

    /**
     * Punto di ingresso dell'applicazione.
     *
     * @param args argomenti della riga di comando
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Inizializza e configura l'interfaccia utente.
     *
     * @param primaryStage stage principale dell'applicazione
     */
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Strumento di Correzione Lessicale Avanzato");
        // Layout principale con padding e gap uniformi
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10));
        grid.setVgap(10);
        grid.setHgap(10);
        // RIGA 0: HBox per "Carica Modello"
        Button caricaModelloButton = new Button("Carica Modello");
        caricaModelloButton.setPrefWidth(150);
        caricaModelloButton.setMaxWidth(150);
        Label modelloLabel = new Label("Scegli un modello Word2Vec (.vec), un file che contiene rappresentazioni vettoriali delle parole, utili per analizzare relazioni semantiche.");
        modelloLabel.setWrapText(true);
        modelloLabel.setMaxWidth(400);
        HBox row0 = new HBox(10, caricaModelloButton, modelloLabel);
        grid.add(row0, 0, 0, 2, 1);
        // RIGA 1: HBox per "Carica File"
        Button caricaFileButton = new Button("Carica File");
        caricaFileButton.setPrefWidth(150);
        caricaFileButton.setMaxWidth(150);
        Label fileLabel = new Label("Seleziona un file di testo (.txt) su cui eseguire l'analisi lessicale, l'identificazione e la classificazione delle parole.");
        fileLabel.setWrapText(true);
        fileLabel.setMaxWidth(400);
        HBox row1 = new HBox(10, caricaFileButton, fileLabel);
        grid.add(row1, 0, 1, 2, 1);
        // RIGA 2: HBox per "Correggi Testo"
        Button correggiTestoButton = new Button("Correggi Testo");
        correggiTestoButton.setPrefWidth(150);
        correggiTestoButton.setMaxWidth(150);
        Label correggiLabel = new Label("Applica la correzione lessicale al testo.");
        correggiLabel.setWrapText(true);
        correggiLabel.setMaxWidth(400);
        HBox row2 = new HBox(10, correggiTestoButton, correggiLabel);
        grid.add(row2, 0, 2, 2, 1);
        // RIGA 3: HBox per "Salva File"
        Button salvaFileButton = new Button("Salva File");
        salvaFileButton.setPrefWidth(150);
        salvaFileButton.setMaxWidth(150);
        Label salvaLabel = new Label("Salva il testo corretto su file.");
        salvaLabel.setWrapText(true);
        salvaLabel.setMaxWidth(400);
        HBox row3 = new HBox(10, salvaFileButton, salvaLabel);
        grid.add(row3, 0, 3, 2, 1);
        // RIGA 4: Area di testo per visualizzare e modificare il testo
        TextArea textArea = new TextArea();
        textArea.setWrapText(true);
        textArea.setPrefHeight(400);
        textArea.setPrefWidth(600);
        textArea.setStyle("-fx-font-size: 16px;");
        grid.add(textArea, 0, 4, 2, 1);
        // Disabilita inizialmente i pulsanti che dipendono dal caricamento del modello/file
        caricaFileButton.setDisable(true);
        correggiTestoButton.setDisable(true);
        salvaFileButton.setDisable(true);
        // Gestione degli eventi
        // Carica Modello
        caricaModelloButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setInitialDirectory(new File(PROJECT_ROOT));
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Modelli Word2Vec (.vec)", "*.vec"));
            File selectedFile = fileChooser.showOpenDialog(primaryStage);
            if (selectedFile != null) {
                selectedModelPath = selectedFile.getAbsolutePath();
                modelloLabel.setText(selectedModelPath);
                Task<Void> loadModelTask = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        File cachedModel = new File(PROJECT_ROOT + "\\cached_model.dl4jmodel");
                        if (cachedModel.exists()) {
                            wordVectors = WordVectorSerializer.readWord2VecModel(cachedModel);
                            System.out.println("Modello caricato dalla cache.");
                        } else {
                            wordVectors = WordVectorSerializer.loadStaticModel(new File(selectedModelPath));
                            System.out.println("Modello caricato correttamente.");
                        }
                        if (wordVectors == null) {
                            throw new Exception("Modello non caricato correttamente.");
                        }
                        textCorrector = new TextCorrector(wordVectors, MAX_EDIT_DISTANCE);
                        return null;
                    }
                };
                loadModelTask.setOnSucceeded(event -> {
                    caricaFileButton.setDisable(false);
                    showAlert(Alert.AlertType.INFORMATION, "Successo", "Modello caricato con successo.");
                });
                loadModelTask.setOnFailed(event -> handleError(new Exception(loadModelTask.getException()), "Errore nel caricamento del modello"));
                new Thread(loadModelTask).start();
            }
        });
        // Carica File
        caricaFileButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setInitialDirectory(new File(PROJECT_ROOT));
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("File di Testo", "*.txt"));
            File selectedFile = fileChooser.showOpenDialog(primaryStage);
            if (selectedFile != null) {
                selectedFilePath = selectedFile.getAbsolutePath();
                fileLabel.setText(selectedFilePath);
                Task<String> loadFileTask = new Task<>() {
                    @Override
                    protected String call() throws Exception {
                        return new String(Files.readAllBytes(selectedFile.toPath()));
                    }
                };
                loadFileTask.setOnSucceeded(event -> {
                    textArea.setText(loadFileTask.getValue());
                    correggiTestoButton.setDisable(false);
                    System.out.println("File caricato con successo: " + selectedFilePath);
                });
                loadFileTask.setOnFailed(event -> handleError(new Exception(loadFileTask.getException()), "Errore nella lettura del file"));
                new Thread(loadFileTask).start();
            }
        });
        // Correggi Testo
        correggiTestoButton.setOnAction(e -> {
            String text = textArea.getText();
            if (text.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Attenzione", "Il testo è vuoto.");
                return;
            }
            try {
                String correctedText = textCorrector.advancedCorrectText(text);
                textArea.setText(correctedText);
                salvaFileButton.setDisable(false);
            } catch (Exception ex) {
                handleError(ex, "Errore durante la correzione del testo");
            }
        });
        // Salva File
        salvaFileButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setInitialDirectory(new File(PROJECT_ROOT));
            fileChooser.setInitialFileName("testo_corretto.txt");
            File file = fileChooser.showSaveDialog(primaryStage);
            if (file != null) {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                    writer.write(textArea.getText());
                    showAlert(Alert.AlertType.INFORMATION, "Successo", "File salvato correttamente.");
                } catch (IOException ex) {
                    handleError(ex, "Errore nel salvataggio del file");
                }
            }
        });
        // Imposta la scena con dimensioni fisse e disabilita il ridimensionamento
        Scene scene = new Scene(grid, 600, 600);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    /**
     * Gestisce gli errori mostrando un alert di errore.
     *
     * @param ex eccezione catturata
     * @param message messaggio di errore personalizzato
     */
    private void handleError(Exception ex, String message) {
        showAlert(Alert.AlertType.ERROR, "Errore", message + ": " + ex.getMessage());
    }

    /**
     * Mostra un alert di diversi tipi (es. informazione, errore).
     *
     * @param type tipo di alert
     * @param title titolo dell'alert
     * @param message messaggio dell'alert
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}