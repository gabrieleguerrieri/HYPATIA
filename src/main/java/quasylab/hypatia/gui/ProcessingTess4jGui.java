package quasylab.hypatia.gui;

import quasylab.hypatia.processing.ImageSelectionWindow;
import quasylab.hypatia.processing.ImageUtils;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import quasylab.hypatia.processing.OCR;
import org.opencv.core.Mat;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Applicazione GUI per l'elaborazione di immagini e trascrizione OCR utilizzando Tesseract.
 * Fornisce funzionalità per:
 * - Caricare immagini
 * - Selezionare regioni di interesse
 * - Scegliere modelli OCR e lingue
 * - Salvare trascrizioni
 */
public class ProcessingTess4jGui extends Application {
    private FileChooser fileChooser;
    private ComboBox<String> ocrModelComboBox;
    private ComboBox<String> languageComboBox;
    private File customTrainedDataFile = null;
    private Mat selectedImageRegion;
    private TextArea logArea;
    private static final String PROJECT_ROOT = System.getProperty("user.dir");


    /**
     * Punto di ingresso principale per l'applicazione JavaFX
     *
     * @param primaryStage Lo stage principale dell'applicazione
     */
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Processing Immagini e Trascrizione OCR");
        fileChooser = new FileChooser();
        File initialDirectory = new File(PROJECT_ROOT);
        if (initialDirectory.exists() && initialDirectory.isDirectory()) {
            fileChooser.setInitialDirectory(initialDirectory);
        }

        // Inizializzazione componenti UI
        Button btnCaricaImmagine = new Button("Carica Immagine");
        btnCaricaImmagine.setPrefWidth(120);

        ocrModelComboBox = new ComboBox<>();
        ocrModelComboBox.getItems().addAll("Modello Standard", "Modello Personalizzato");
        ocrModelComboBox.getSelectionModel().selectFirst();
        HBox ocrModelHBox = new HBox(10, ocrModelComboBox, creaEtichetta("Valutazione dell'utilizzo di modelli standard di Tesseract o modelli ottimizzati tramite fine-tuning per il riconoscimento OCR"));

        languageComboBox = new ComboBox<>();
        languageComboBox.getItems().addAll("eng", "ita", "ita_old");
        languageComboBox.getSelectionModel().select("ita");
        HBox languageHBox = new HBox(10, languageComboBox, creaEtichetta("Scelta della lingua del modello standard per l'elaborazione del testo"));

        Button selectModelButton = new Button("Seleziona Modello Personalizzato");
        selectModelButton.setDisable(true);
        HBox customModelHBox = new HBox(10, selectModelButton, creaEtichetta("Scelta del modello personalizzato .traineddata per l'elaborazione del testo con Tesseract"));

        Button ocrTranscribeButton = new Button("Trascrivi Testo con OCR");
        logArea = new TextArea();
        logArea.setPrefSize(580, 150);
        Button clearOutputButton = new Button("Cancella Output Trascrizioni");

        // Configurazione layout principale
        GridPane grid = configuraGridPane(btnCaricaImmagine, ocrModelHBox, languageHBox, customModelHBox, ocrTranscribeButton, clearOutputButton);

        // Configurazione gestori eventi
        configuraGestoriEventi(primaryStage, btnCaricaImmagine, selectModelButton, ocrTranscribeButton, clearOutputButton);

        Scene scene = new Scene(grid, 600, 600);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    /**
     * Crea un GridPane configurato
     */
    private GridPane configuraGridPane(Button btnCarica, HBox ocrModel, HBox language, HBox customModel, Button ocrButton, Button clearButton) {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10));
        grid.setVgap(10);
        grid.setHgap(10);
        grid.add(btnCarica, 0, 0, 2, 1);
        grid.add(ocrModel, 0, 1, 2, 1);
        grid.add(language, 0, 2, 2, 1);
        grid.add(customModel, 0, 3, 2, 1);
        grid.add(ocrButton, 0, 4, 2, 1);
        grid.add(logArea, 0, 5, 2, 1);
        grid.add(clearButton, 0, 6, 2, 1);
        return grid;
    }

    /**
     * Configura tutti i gestori di eventi per i componenti UI
     */
    private void configuraGestoriEventi(Stage stage, Button loadBtn, Button modelBtn, Button ocrBtn, Button clearBtn) {
        loadBtn.setOnAction(e -> gestisciCaricamentoImmagine(stage));
        modelBtn.setOnAction(e -> gestisciSelezioneModello(stage));
        ocrModelComboBox.setOnAction(e -> gestisciCambioTipoModello(modelBtn));
        ocrBtn.setOnAction(e -> gestisciTrascrizioneOCR());
        clearBtn.setOnAction(e -> gestisciPuliziaOutput());
    }

    /**
     * Gestisce il caricamento dell'immagine e la selezione della regione
     */
    private void gestisciCaricamentoImmagine(Stage stage) {
        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            Mat originalImage = ImageUtils.loadImage(selectedFile.getAbsolutePath());
            if (originalImage.empty()) {
                loggaErrore("Errore nel caricamento dell'immagine.");
            } else {
                loggaMessaggio("Immagine caricata: " + selectedFile.getAbsolutePath());
                new ImageSelectionWindow().show(selectedFile, this::gestisciSelezioneRegione);
            }
        }
    }

    /**
     * Callback per la gestione della regione selezionata
     */
    private void gestisciSelezioneRegione(Mat region) {
        selectedImageRegion = region;
        loggaMessaggio("Porzione selezionata salvata per le elaborazioni successive.");
        mostraAlert("Porzione selezionata salvata con successo!");
    }

    /**
     * Gestisce la selezione del file del modello personalizzato
     */
    private void gestisciSelezioneModello(Stage stage) {
        // Imposta un filtro per i file .traineddata
        fileChooser.getExtensionFilters().clear(); // Pulisce eventuali filtri precedenti
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Tesseract Model Files", "*.traineddata")
        );

        // Mostra il dialogo di selezione file
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            // Verifica che il file abbia l'estensione corretta
            if (!selectedFile.getName().endsWith(".traineddata")) {
                loggaErrore("Il file selezionato non è un modello Tesseract valido.");
            } else {
                customTrainedDataFile = selectedFile;
                loggaMessaggio("Modello personalizzato selezionato: " + selectedFile.getAbsolutePath());
                mostraAlert("Modello selezionato: " + selectedFile.getAbsolutePath());
            }
        } else {
            loggaMessaggio("Selezione annullata.");
        }
    }

    /**
     * Aggiorna i componenti UI in base al tipo di modello OCR selezionato
     */
    private void gestisciCambioTipoModello(Button modelButton) {
        boolean isCustomModel = "Modello Personalizzato".equals(ocrModelComboBox.getValue());
        modelButton.setDisable(!isCustomModel);
        languageComboBox.setDisable(isCustomModel);
    }

    /**
     * Avvia il processo di trascrizione OCR
     */
    private void gestisciTrascrizioneOCR() {
        if (selectedImageRegion == null || selectedImageRegion.empty()) {
            loggaErrore("Nessuna porzione di immagine selezionata.");
            return;
        }

        Task<String> ocrTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                return eseguiOCR();
            }

            @Override
            protected void succeeded() {
                gestisciSuccessoOCR(getValue());
            }

            @Override
            protected void failed() {
                gestisciErroreOCR(getException());
            }
        };

        new Thread(ocrTask).start();
    }

    /**
     * Esegue l'elaborazione OCR in base alle impostazioni correnti
     */
    private String eseguiOCR() throws Exception {
        creaDirectoryOutput();
        String ocrModel = ocrModelComboBox.getValue();
        String language = languageComboBox.getValue();

        if ("Modello Personalizzato".equals(ocrModel)) {
            return gestisciOCRModelloPersonalizzato();
        }

        return gestisciOCRModelloStandard(language);
    }

    /**
     * Gestisce la pulizia delle directory di output
     */
    private void gestisciPuliziaOutput() {
        try {
            svuotaDirectory(PROJECT_ROOT + "\\ocr-transcriptions\\");
            loggaMessaggio("Output cancellato.");
        } catch (IOException ex) {
            loggaErrore("Errore durante la cancellazione: " + ex.getMessage());
        }
    }

    /**
     * Crea etichette formattate con testo a capo
     */
    private Label creaEtichetta(String testo) {
        Label etichetta = new Label(testo);
        etichetta.setWrapText(true);
        etichetta.setMaxWidth(400);
        return etichetta;
    }

    /**
     * Mostra un alert informativo
     */
    private void mostraAlert(String messaggio) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(messaggio);
        alert.showAndWait();
    }

    /**
     * Mostra il risultato della trascrizione in una nuova finestra
     */
    private void mostraTrascrizione(String testo) {
        Stage stage = new Stage();
        TextArea textArea = new TextArea(testo);
        textArea.setPrefSize(600, 600);

        // Imposta il titolo della finestra
        stage.setTitle("Trascrizione OCR");

        stage.setScene(new Scene(textArea));
        // Imposta lo stile CSS per aumentare il font solo del testo
        textArea.setStyle("-fx-font-size: 16px; -fx-font-family: 'Arial';");
        stage.show();
    }

    /**
     * Garantisce l'esistenza delle directory di output
     */
    private void creaDirectoryOutput() {
        creaDirectory(PROJECT_ROOT + "\\ocr-transcriptions\\standard-model");
        creaDirectory(PROJECT_ROOT + "\\ocr-transcriptions\\custom-model");
    }

    /**
     * Crea una directory se non esiste
     */
    private void creaDirectory(String percorso) {
        File dir = new File(percorso);
        if (!dir.exists()) dir.mkdirs();
    }

    /**
     * Salva la trascrizione OCR nella directory appropriata
     */
    private void salvaTrascrizioneOCR(String testo, String tipoModello) throws IOException {
        String percorso = PROJECT_ROOT + "\\ocr-transcriptions\\" + tipoModello + "\\transcription_"
                + System.currentTimeMillis() + ".txt";
        Files.write(Paths.get(percorso), testo.getBytes());
    }

    // Metodi di supporto per il logging
    /**
     * Scrive un messaggio informativo nell'area di log della GUI.
     *
     * @param messaggio Il messaggio da visualizzare.
     */
    private void loggaMessaggio(String messaggio) {
        logArea.appendText(messaggio + "\n");
    }

    /**
     * Mostra un messaggio di errore tramite alert e lo scrive nell'area di log.
     *
     * @param errore Il messaggio di errore da visualizzare.
     */
    private void loggaErrore(String errore) {
        mostraAlert(errore);
        logArea.appendText("ERRORE: " + errore + "\n");
    }

    // Metodi helper aggiuntivi
    /**
     * Esegue il riconoscimento OCR utilizzando un modello personalizzato.
     *
     * @return La trascrizione del testo OCR.
     * @throws Exception se il modello non è selezionato o si verifica un errore.
     */
    private String gestisciOCRModelloPersonalizzato() throws Exception {
        if (customTrainedDataFile == null) throw new Exception("Nessun modello personalizzato selezionato");
        String risultato = OCR.customModel(selectedImageRegion, customTrainedDataFile);
        salvaTrascrizioneOCR(risultato, "custom-model");
        return risultato;
    }

    /**
     * Esegue il riconoscimento OCR utilizzando un modello standard di Tesseract.
     *
     * @param lingua La lingua da utilizzare per il riconoscimento.
     * @return La trascrizione OCR.
     * @throws Exception se la lingua non è selezionata o si verifica un errore.
     */
    private String gestisciOCRModelloStandard(String lingua) throws Exception {
        if (lingua == null) throw new Exception("Lingua non selezionata");
        String risultato = OCR.easyOCRStandardModel(selectedImageRegion, lingua);
        salvaTrascrizioneOCR(risultato, "standard-model");
        return risultato;
    }

    /**
     * Gestisce la visualizzazione e il logging del risultato OCR in caso di successo.
     *
     * @param trascrizione Il testo trascritto ottenuto dal riconoscimento.
     */
    private void gestisciSuccessoOCR(String trascrizione) {
        if (trascrizione != null) {
            mostraTrascrizione(trascrizione);
            loggaMessaggio("Trascrizione completata: " + trascrizione);
        } else {
            loggaErrore("Trascrizione fallita");
        }
    }

    /**
     * Gestisce gli errori verificatisi durante il processo OCR.
     *
     * @param ex L'eccezione generata durante l'elaborazione.
     */
    private void gestisciErroreOCR(Throwable ex) {
        loggaErrore("Errore OCR: " + ex.getMessage());
    }

    /**
     * Cancella tutti i file nella directory specificata.
     *
     * @param percorso Il percorso della directory da svuotare.
     * @throws IOException se si verifica un errore durante l'eliminazione dei file.
     */
    private void svuotaDirectory(String percorso) throws IOException {
        Files.walk(Paths.get(percorso))
                .filter(Files::isRegularFile)
                .forEach(file -> {
                    try {
                        Files.delete(file);
                    } catch (IOException e) {

                    }
                });
    }
}