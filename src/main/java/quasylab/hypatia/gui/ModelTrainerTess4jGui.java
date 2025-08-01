package quasylab.hypatia.gui;

import quasylab.hypatia.modelTess4JTraining.BoxCleaner;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import quasylab.hypatia.modelTess4JTraining.BoxTextEditor;
import quasylab.hypatia.modelTess4JTraining.ModelTrainingExecutor;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Classe principale per l'interfaccia grafica dell'applicazione di addestramento del modello Tesseract.
 * Implementa l'interfaccia ModelTrainingLogger per gestire i log durante il processo di addestramento.
 */
public class ModelTrainerTess4jGui extends Application {

    private static final String PROJECT_ROOT = System.getProperty("user.dir"); // Ottieni la root del progetto
    private String selectedImagePath;
    private TextField epochsField; // Campo per il numero di epoche

    /**
     * Metodo principale per avviare l'applicazione JavaFX.
     *
     * @param args Argomenti della riga di comando.
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Metodo chiamato all'avvio dell'applicazione JavaFX.
     * Inizializza l'interfaccia grafica e gestisce gli eventi degli elementi UI.
     *
     * @param primaryStage Stage principale dell'applicazione.
     */
    @Override
    public void start(Stage primaryStage) {
        initializeDirectories();
        primaryStage.setTitle("Addestramento Modello Personalizzato Tesseract");

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10));
        grid.setVgap(10);
        grid.setHgap(10);

        // Creazione dei bottoni e delle etichette per l'interfaccia
        Button caricaImmagineButton = createButton("Carica Immagine", 120);
        HBox row0 = new HBox(10, caricaImmagineButton);
        grid.add(row0, 0, 0, 2, 1);

        Button generaBoxFileButton = createButton("Genera File .box", 150);
        Label generaBoxFileLabel = createLabel("Creazione dei file .box che registrano le coordinate delle righe di testo rilevate in ogni immagine");
        HBox row1 = new HBox(10, generaBoxFileButton, generaBoxFileLabel);
        grid.add(row1, 0, 1, 2, 1);

        Button pulisciBoxButton = createButton("Pulisci Box", 150);
        Label pulisciBoxLabel = createLabel("Elimina errori e incoerenze dal file .box, consentendo la ridefinizione manuale dei box.");
        HBox row2 = new HBox(10, pulisciBoxButton, pulisciBoxLabel);
        grid.add(row2, 0, 2, 2, 1);

        Button editorTestoBoxButton = createButton("Editor di Testo per Box", 150);
        Label editorTestoBoxLabel = createLabel("Consente la modifica manuale del file .box, offrendo la possibilità di regolare testo e annotazioni.");
        HBox row3 = new HBox(10, editorTestoBoxButton, editorTestoBoxLabel);
        grid.add(row3, 0, 3, 2, 1);

        Button generaLstmfFileButton = createButton("Genera File .lstmf", 150);
        Label generaLstmfFileLabel = createLabel("Genera il file .lstmf per l'addestramento. Questo file raccoglie i dati preprocessati per il modello LSTM.");
        HBox row4 = new HBox(10, generaLstmfFileButton, generaLstmfFileLabel);
        grid.add(row4, 0, 4, 2, 1);

        Button creaTrainListFileButton = createButton("Crea train_listfile.txt", 150);
        Label creaTrainListFileLabel = createLabel("Crea un file di testo (train_listfile.txt) che elenca i file da utilizzare per l'addestramento del modello.");
        HBox row5 = new HBox(10, creaTrainListFileButton, creaTrainListFileLabel);
        grid.add(row5, 0, 5, 2, 1);

        Label epochsLabel = new Label("Numero di Epoche:");
        epochsField = new TextField("5000");
        epochsField.setPrefWidth(80);
        HBox row6 = new HBox(10, epochsLabel, epochsField);
        grid.add(row6, 0, 6, 2, 1);

        Button avviaAddestramentoButton = createButton("Avvia Addestramento", 150);
        Label avviaAddestramentoLabel = createLabel("Avvia il processo di perfezionamento (fine tuning) utilizzando i file predisposti per l'addestramento.");
        HBox row7 = new HBox(10, avviaAddestramentoButton, avviaAddestramentoLabel);
        grid.add(row7, 0, 7, 2, 1);

        Button finalizzaModelloButton = createButton("Finalizza Modello", 150);
        Label finalizzaModelloLabel = createLabel("Conclude l'addestramento, finalizza il modello e lo rende pronto all'uso attraverso il file .traineddata generato.");
        HBox row8 = new HBox(10, finalizzaModelloButton, finalizzaModelloLabel);
        grid.add(row8, 0, 8, 2, 1);

        TextArea logArea = createLogArea();
        grid.add(logArea, 0, 9, 2, 1);

        Button cancellaOutputButton = createButton("Cancella Output", 150);
        grid.add(cancellaOutputButton, 0, 10, 2, 1);

        // Gestione degli eventi
        setupEventHandlers(caricaImmagineButton, generaBoxFileButton, pulisciBoxButton, editorTestoBoxButton,
                generaLstmfFileButton, creaTrainListFileButton, avviaAddestramentoButton, finalizzaModelloButton,
                cancellaOutputButton, logArea);

        Scene scene = new Scene(grid, 600, 600);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    /**
     * Inizializza le directory necessarie per il progetto.
     */
    private void initializeDirectories() {
        String[] directories = {
                PROJECT_ROOT + "\\tess4j dataset",
                PROJECT_ROOT + "\\tess4j training",
                PROJECT_ROOT + "\\tess4j model"
        };
        for (String dirPath : directories) {
            File dir = new File(dirPath);
            if (!dir.exists()) {
                if (dir.mkdirs()) {
                    System.out.println("Directory creata: " + dirPath);
                } else {
                    System.out.println("Errore durante la creazione della directory: " + dirPath);
                }
            }
        }
    }

    /**
     * Crea un bottone con larghezza predefinita.
     *
     * @param text      Testo del bottone.
     * @param maxWidth  Larghezza massima del bottone.
     * @return Bottone configurato.
     */
    private Button createButton(String text, int maxWidth) {
        Button button = new Button(text);
        button.setPrefWidth(maxWidth);
        button.setMaxWidth(maxWidth);
        return button;
    }

    /**
     * Crea un'etichetta con testo a capo abilitato.
     *
     * @param text Testo dell'etichetta.
     * @return Etichetta configurata.
     */
    private Label createLabel(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(400);
        return label;
    }

    /**
     * Crea un'area di testo per i log.
     *
     * @return Area di testo configurata.
     */
    private TextArea createLogArea() {
        TextArea logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setPrefHeight(150);
        logArea.setPrefWidth(580);
        logArea.setMaxWidth(580);
        return logArea;
    }

    /**
     * Configura i gestori di eventi per i bottoni.
     */
    private void setupEventHandlers(Button caricaImmagineButton, Button generaBoxFileButton, Button pulisciBoxButton,
                                    Button editorTestoBoxButton, Button generaLstmfFileButton, Button creaTrainListFileButton,
                                    Button avviaAddestramentoButton, Button finalizzaModelloButton, Button cancellaOutputButton,
                                    TextArea logArea) {
        caricaImmagineButton.setOnAction(e -> selectFile(logArea));
        generaBoxFileButton.setOnAction(e -> handleGenerateBoxFile(logArea));
        pulisciBoxButton.setOnAction(e -> handleCleanBox(logArea));
        editorTestoBoxButton.setOnAction(e -> handleEditBox(logArea));
        generaLstmfFileButton.setOnAction(e -> handleGenerateLstmfFile(logArea));
        creaTrainListFileButton.setOnAction(e -> handleCreateTrainListFile(logArea));
        avviaAddestramentoButton.setOnAction(e -> handleStartTraining(logArea));
        finalizzaModelloButton.setOnAction(e -> handleFinalizeModel(logArea));
        cancellaOutputButton.setOnAction(e -> handleClearOutput(logArea));
    }

    /**
     * Apre una finestra di dialogo per selezionare un file immagine.
     *
     * @param logArea Area di testo per i log.
     */
    private void selectFile(TextArea logArea) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(PROJECT_ROOT));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Files", "*.png"));
        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null && selectedFile.getName().endsWith(".png")) {
            copyImageToDataset(selectedFile, logArea);
        } else {
            logArea.appendText("Errore: Selezionare un file .png.\n");
        }
    }

    /**
     * Copia l'immagine selezionata nella directory dataset.
     *
     * @param selectedFile File immagine selezionato.
     * @param logArea      Area di testo per i log.
     */
    private void copyImageToDataset(File selectedFile, TextArea logArea) {
        String outputDirectory = PROJECT_ROOT + "\\tess4j dataset";
        File directory = new File(outputDirectory);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        String destinationImagePath = outputDirectory + File.separator + selectedFile.getName();
        try {
            Files.copy(selectedFile.toPath(), new File(destinationImagePath).toPath(), StandardCopyOption.REPLACE_EXISTING);
            selectedImagePath = destinationImagePath;
            logArea.appendText("Immagine caricata con successo: " + destinationImagePath + "\n");
        } catch (IOException ex) {
            ex.printStackTrace();
            logArea.appendText("Errore: " + ex.getMessage() + "\n");
        }
    }

    /**
     * Genera il file .box utilizzando Tesseract.
     *
     * @param imagePath Percorso dell'immagine.
     * @throws IOException Errore durante la generazione del file.
     */
    private void generateBoxFile(String imagePath) throws IOException {
        String outputDirectory = PROJECT_ROOT + "\\tess4j dataset";
        File directory = new File(outputDirectory);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        String fileName = new File(imagePath).getName().replaceFirst("[.][^.]+$", "");
        String boxFilePath = outputDirectory + File.separator + fileName + ".box";

        ProcessBuilder processBuilder = new ProcessBuilder(
                "tesseract", imagePath, outputDirectory + File.separator + fileName, "--psm", "6", "wordstrbox"
        );
        processBuilder.redirectErrorStream(true);
        executeProcess(processBuilder, boxFilePath, "File .box generato con successo.");
    }

    /**
     * Genera il file .lstmf utilizzando Tesseract.
     *
     * @param imagePath Percorso dell'immagine.
     * @throws IOException Errore durante la generazione del file.
     */
    private void generateLstmfFile(String imagePath) throws IOException, InterruptedException {
        if (!imagePath.toLowerCase().endsWith(".tif") &&
                !imagePath.toLowerCase().endsWith(".tiff") &&
                !imagePath.toLowerCase().endsWith(".png")) {
            throw new IOException("Formato file non supportato. Usa .tif, .tiff o .png.");
        }

        // Percorso della cartella di output con spazio nel nome
        String outputDirectory = PROJECT_ROOT + File.separator + "tess4j dataset";
        File directory = new File(outputDirectory);
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Impossibile creare la directory di output: " + outputDirectory);
        }

        File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            throw new IOException("Il file immagine non esiste: " + imagePath);
        }

        String fileName = imageFile.getName().replaceFirst("[.][^.]+$", "");
        String lstmfFilePath = outputDirectory + File.separator + fileName + ".lstmf";

        // Creazione del comando con virgolette per gestire i percorsi con spazi
        ProcessBuilder processBuilder = new ProcessBuilder(
                "tesseract",
                imageFile.getAbsolutePath(),
                outputDirectory + File.separator + fileName,
                "--psm", "6", "wordstrbox", "lstm.train"
        );

        processBuilder.redirectErrorStream(true);
        executeProcess(processBuilder, lstmfFilePath, "File .lstmf generato con successo.");

        // Verifica se il file .lstmf è stato effettivamente creato
        File lstmfFile = new File(lstmfFilePath);
        if (!lstmfFile.exists()) {
            throw new IOException("Errore: il file .lstmf non è stato generato correttamente.");
        }
    }

    /**
     * Crea il file train_listfile.txt contenente i percorsi dei file .lstmf.
     *
     * @throws IOException Errore durante la creazione del file.
     */
    private void createTrainListFile() throws IOException {
        String generatedFilesDirectory = PROJECT_ROOT + "\\tess4j dataset";
        File directory = new File(generatedFilesDirectory);
        if (!directory.exists()) {
            throw new IOException("Directory " + generatedFilesDirectory + " does not exist.");
        }

        String trainingDirectory = PROJECT_ROOT + "\\tess4j training";
        File trainingDir = new File(trainingDirectory);
        if (!trainingDir.exists()) {
            trainingDir.mkdirs();
        }

        String trainListFilePath = trainingDirectory + File.separator + "train_listfile.txt";
        File[] lstmfFiles = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".lstmf"));
        if (lstmfFiles == null || lstmfFiles.length == 0) {
            System.out.println("No .lstmf files found in " + generatedFilesDirectory);
            return;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(trainListFilePath))) {
            for (File lstmfFile : lstmfFiles) {
                String filePath = lstmfFile.getAbsolutePath().replace("\\", "/");
                writer.write(filePath);
                writer.write("\n");
            }
        }
        System.out.println("train_listfile.txt created at: " + trainListFilePath);
    }


    /**
     * Finalizza il modello addestrato permettendo di selezionare i file .checkpoint e .traineddata tramite GUI.
     *
     * @throws IOException        Errore durante la finalizzazione.
     * @throws InterruptedException Errore nell'esecuzione del processo.
     */
    private void finalizeModel() throws IOException, InterruptedException {
        String checkpointPath = selectCheckpointFile();
        if (checkpointPath == null) {
            System.err.println("Errore: Nessun file .checkpoint selezionato.");
            return;
        }

        String trainedDataPath = selectTrainedDataFile();
        if (trainedDataPath == null) {
            System.err.println("Errore: Nessun file .traineddata selezionato.");
            return;
        }

        String modelName = "model_" + System.currentTimeMillis() + ".traineddata";
        String modelOutputPath = PROJECT_ROOT + "\\tess4j model\\" + modelName;

        File outputDir = new File(PROJECT_ROOT + "\\tess4j model");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        // Creazione dell'array di argomenti
        ProcessBuilder processBuilder = new ProcessBuilder(
                "lstmtraining",
                "--stop_training",
                "--continue_from", checkpointPath,
                "--traineddata", trainedDataPath,
                "--model_output", modelOutputPath
        );

        processBuilder.redirectErrorStream(true);

        // Esegue il processo
        Process process = processBuilder.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            System.err.println("Errore durante la finalizzazione del modello. Controllare i file selezionati.");
            return;
        }

        System.out.println("Modello finalizzato con successo: " + modelOutputPath);
    }

    /**
     * Seleziona un file .traineddata per la finalizzazione del modello, utilizzando un FileChooser.
     *
     * @return Percorso del file .traineddata selezionato.
     */
    private String selectTrainedDataFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(PROJECT_ROOT + "\\tess4j training"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Traineddata Files", "*.traineddata"));
        File selectedFile = fileChooser.showOpenDialog(null);

        if (selectedFile != null && selectedFile.getName().endsWith(".traineddata")) {
            return selectedFile.getAbsolutePath();
        } else {
            System.out.println("Errore: Selezionare un file .traineddata valido.");
            return null;
        }
    }

    /**
     * Seleziona un file .checkpoint per la finalizzazione del modello.
     *
     * @return Percorso del file .checkpoint selezionato.
     */
    private String selectCheckpointFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(PROJECT_ROOT));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Checkpoint Files", "*.checkpoint"));
        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null && selectedFile.getName().endsWith(".checkpoint")) {
            return selectedFile.getAbsolutePath();
        } else {
            System.out.println("Error: Please select a .checkpoint file.");
            return null;
        }
    }

    /**
     * Cancella i file di output generati durante il processo.
     *
     * @throws IOException Errore durante la cancellazione dei file.
     */
    private void clearOutput() throws IOException {
        String datasetDirectory = PROJECT_ROOT + "\\tess4j dataset";
        File datasetDir = new File(datasetDirectory);
        if (datasetDir.exists() && datasetDir.isDirectory()) {
            deleteFilesInDirectory(datasetDir);
            System.out.println("Tutti i file in " + datasetDirectory + " sono stati eliminati.");
        }

        String trainingDirectory = PROJECT_ROOT + "\\tess4j training";
        File trainingDir = new File(trainingDirectory);
        if (trainingDir.exists() && trainingDir.isDirectory()) {
            deleteFilesWithExtension(trainingDir, ".checkpoint");
            System.out.println("Tutti i file .checkpoint in " + trainingDirectory + " sono stati eliminati.");
        }
    }

    /**
     * Elimina tutti i file in una directory.
     *
     * @param directory Directory da cui eliminare i file.
     * @throws IOException Errore durante l'eliminazione dei file.
     */
    private void deleteFilesInDirectory(File directory) throws IOException {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && !file.delete()) {
                    throw new IOException("Impossibile eliminare il file: " + file.getAbsolutePath());
                }
            }
        }
    }

    /**
     * Elimina i file con una specifica estensione in una directory.
     *
     * @param directory Directory in cui cercare i file.
     * @param extension Estensione dei file da eliminare.
     * @throws IOException Errore durante l'eliminazione dei file.
     */
    private void deleteFilesWithExtension(File directory, String extension) throws IOException {
        File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(extension));
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && !file.delete()) {
                    throw new IOException("Impossibile eliminare il file: " + file.getAbsolutePath());
                }
            }
        }
    }

    /**
     * Esegue un processo esterno e gestisce l'output.
     *
     * @param processBuilder ProcessBuilder configurato.
     * @param filePath       Percorso del file generato.
     * @param successMessage Messaggio di successo da stampare.
     * @throws IOException Errore durante l'esecuzione del processo.
     */
    private void executeProcess(ProcessBuilder processBuilder, String filePath, String successMessage) throws IOException {
        Process process = processBuilder.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }
        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("Process failed with exit code " + exitCode);
            }
            System.out.println(successMessage + ": " + filePath);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Process interrupted.", ex);
        }
    }

    /**
     * Gestisce l'evento di generazione del file .box.
     *
     * @param logArea Area di testo per i log.
     */
    private void handleGenerateBoxFile(TextArea logArea) {
        if (selectedImagePath == null) {
            logArea.appendText("Errore: Selezionare un file immagine.\n");
            return;
        }
        try {
            generateBoxFile(selectedImagePath);
            logArea.appendText("File .box generato con successo.\n");
        } catch (IOException ex) {
            ex.printStackTrace();
            logArea.appendText("Errore: " + ex.getMessage() + "\n");
        }
    }

    /**
     * Gestisce l'evento di pulizia del file .box.
     *
     * @param logArea Area di testo per i log.
     */
    private void handleCleanBox(TextArea logArea) {
        if (selectedImagePath == null) {
            logArea.appendText("Errore: Selezionare un file immagine.\n");
            return;
        }
        String boxFilePath = getBoxFilePath(selectedImagePath);
        if (!new File(boxFilePath).exists()) {
            logArea.appendText("Errore: File .box non trovato.\n");
            return;
        }
        BoxCleaner boxCleaner = new BoxCleaner();
        boxCleaner.boxCleaner(selectedImagePath, boxFilePath);
        logArea.appendText("Pulisci Box eseguito con successo.\n");
    }

    /**
     * Gestisce l'evento di modifica del file .box.
     *
     * @param logArea Area di testo per i log.
     */
    private void handleEditBox(TextArea logArea) {
        if (selectedImagePath == null) {
            logArea.appendText("Errore: Selezionare un file immagine.\n");
            return;
        }
        String boxFilePath = getBoxFilePath(selectedImagePath);
        if (!new File(boxFilePath).exists()) {
            logArea.appendText("Errore: File .box non trovato.\n");
            return;
        }
        BoxTextEditor editor = new BoxTextEditor(boxFilePath);
        Stage editorStage = new Stage();
        String fontStyle = "-fx-font-size: 16px; -fx-font-family: 'Arial';";
        editor.startEditor(editorStage, fontStyle);
        logArea.appendText("Editor di Testo per Box avviato.\n");
    }

    /**
     * Gestisce l'evento di generazione del file .lstmf.
     *
     * @param logArea Area di testo per i log.
     */
    private void handleGenerateLstmfFile(TextArea logArea) {
        if (selectedImagePath == null) {
            logArea.appendText("Errore: Selezionare un file immagine.\n");
            return;
        }
        try {
            generateLstmfFile(selectedImagePath);
            logArea.appendText("File .lstmf generato con successo.\n");
        } catch (Exception ex) {
            ex.printStackTrace();
            logArea.appendText("Errore: " + ex.getMessage() + "\n");
        }
    }

    /**
     * Gestisce l'evento di creazione del file train_listfile.txt.
     *
     * @param logArea Area di testo per i log.
     */
    private void handleCreateTrainListFile(TextArea logArea) {
        try {
            createTrainListFile();
            logArea.appendText("train_listfile.txt creato con successo.\n");
        } catch (Exception ex) {
            ex.printStackTrace();
            logArea.appendText("Errore: " + ex.getMessage() + "\n");
        }
    }

    /**
     * Gestisce l'evento di avvio dell'addestramento.
     *
     * @param logArea Area di testo per i log.
     */
    private void handleStartTraining(TextArea logArea) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(PROJECT_ROOT));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Tesseract traineddata Files", "*.traineddata"));
        File traineddataFile = fileChooser.showOpenDialog(null);
        if (traineddataFile == null) {
            logArea.appendText("Nessun file .traineddata selezionato.\n");
            return;
        }

        fileChooser.getExtensionFilters().clear();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("LSTM Model Files", "*.lstm"));
        File lstmFile = fileChooser.showOpenDialog(null);
        if (lstmFile == null) {
            logArea.appendText("Nessun file .lstm selezionato.\n");
            return;
        }

        int maxIterations;
        try {
            maxIterations = Integer.parseInt(epochsField.getText());
            if (maxIterations <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            logArea.appendText("Errore: Inserisci un numero valido di epoche.\n");
            return;
        }

        ModelTrainingExecutor executor = new ModelTrainingExecutor(
                PROJECT_ROOT,
                traineddataFile.getAbsolutePath(),
                lstmFile.getAbsolutePath()
        );
        Task<Void> trainingTask = new Task<>() {
            @Override
            protected Void call() {
                try {
                    executor.executeTraining(maxIterations);
                    logArea.appendText("Addestramento completato con successo.\n");
                } catch (Exception ex) {
                    ex.printStackTrace();
                    logArea.appendText("Errore durante l'addestramento: " + ex.getMessage() + "\n");
                }
                return null;
            }
        };
        new Thread(trainingTask).start();
    }

    /**
     * Gestisce l'evento di finalizzazione del modello.
     *
     * @param logArea Area di testo per i log.
     */
    private void handleFinalizeModel(TextArea logArea) {
        try {
            finalizeModel();
            logArea.appendText("Modello finalizzato con successo.\n");
        } catch (Exception ex) {
            ex.printStackTrace();
            logArea.appendText("Errore: " + ex.getMessage() + "\n");
        }
    }

    /**
     * Gestisce l'evento di cancellazione dell'output.
     *
     * @param logArea Area di testo per i log.
     */
    private void handleClearOutput(TextArea logArea) {
        try {
            clearOutput();
            logArea.appendText("Output cancellato con successo.\n");
        } catch (IOException ex) {
            ex.printStackTrace();
            logArea.appendText("Errore durante la cancellazione dell'output: " + ex.getMessage() + "\n");
        }
    }

    /**
     * Restituisce il percorso del file .box associato all'immagine.
     *
     * @param imagePath Percorso dell'immagine.
     * @return Percorso del file .box.
     */
    private String getBoxFilePath(String imagePath) {
        String fileName = new File(imagePath).getName().replaceFirst("[.][^.]+$", "");
        return PROJECT_ROOT + "\\tess4j dataset\\" + fileName + ".box";
    }

}