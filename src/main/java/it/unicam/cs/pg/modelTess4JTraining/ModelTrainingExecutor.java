package it.unicam.cs.pg.modelTess4JTraining;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Classe per l'esecuzione del training di modelli LSTM tramite Tesseract.
 * Gestisce la validazione degli input, l'esecuzione del processo e il logging degli output.
 */
public class ModelTrainingExecutor {
    private final String trainedDataPath;
    private final String lstmModelPath;
    private final String projectRoot;

    /**
     * Costruttore per l'executor del training
     *
     * @param projectRoot Percorso radice del progetto
     * @param trainedDataPath Percorso al file .traineddata
     * @param lstmModelPath Percorso al modello LSTM iniziale
     */
    public ModelTrainingExecutor(String projectRoot, String trainedDataPath, String lstmModelPath) {
        this.projectRoot = projectRoot;
        this.trainedDataPath = trainedDataPath;
        this.lstmModelPath = lstmModelPath;
    }

    /**
     * Avvia il processo di addestramento del modello
     *
     * @param maxIterazioni Numero massimo di iterazioni per il training
     * @throws IOException In caso di errori I/O o file mancanti
     * @throws InterruptedException Se il processo viene interrotto
     */
    public void executeTraining(int maxIterazioni) throws IOException, InterruptedException {
        // Verifica preliminare dei file necessari
        validateInputFiles();

        // Path di output per il modello addestrato
        String modelOutputPath = projectRoot + File.separator + "tess4j training" + File.separator + "output_model";

        Process process = buildTrainingProcess(modelOutputPath, maxIterazioni);
        executeProcess(process);

        logSuccess("Addestramento completato con successo");
    }

    /**
     * Verifica la presenza di tutti i file necessari per il training
     */
    private void validateInputFiles() throws IOException {
        checkFileExists(trainedDataPath, "File traineddata");
        checkFileExists(lstmModelPath, "File LSTM");
        checkFileExists(projectRoot + File.separator + "tess4j training" + File.separator + "train_listfile.txt", "Train listfile");
    }

    /**
     * Controlla l'esistenza di un file specificato
     */
    private void checkFileExists(String path, String tipoFile) throws IOException {
        if (!new File(path).exists()) {
            throw new IOException(tipoFile + " non trovato: " + path);
        }
    }

    /**
     * Costruisce il comando per l'addestramento
     */
    private Process buildTrainingProcess(String modelOutputPath, int maxIterazioni) throws IOException {
        // Costruzione del comando lstmtraining con tutti i parametri
        String command = String.format(
                "lstmtraining --traineddata \"%s\" --train_listfile \"%s\" --model_output \"%s\" --continue_from \"%s\" --max_iterations %d",
                trainedDataPath,
                projectRoot + File.separator + "tess4j training" + File.separator + "train_listfile.txt",
                modelOutputPath,
                lstmModelPath,
                maxIterazioni
        );

        logDebug("Esecuzione comando training: " + command);

        // Avvio processo con redirect degli errori sullo stdout
        return new ProcessBuilder("cmd.exe", "/c", command)
                .redirectErrorStream(true)
                .start();
    }

    /**
     * Esegue il processo e gestisce il suo output
     */
    private void executeProcess(Process process) throws IOException, InterruptedException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            // Lettura continua dell'output del processo
            while ((line = reader.readLine()) != null) {
                logProcessOutput(line);
            }
        }

        // Controllo exit code per rilevare errori
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Errore durante l'addestramento (Codice: " + exitCode + ")");
        }
    }

    /**
     * Logga un messaggio di successo.
     *
     * @param message Messaggio da loggare.
     */
    private void logSuccess(String message) {
        System.out.println("[SUCCESS]: " + message);
    }

    /**
     * Logga un messaggio di debug.
     *
     * @param debugInfo Informazioni di debug da loggare.
     */
    private void logDebug(String debugInfo) {
        System.out.println("[DEBUG]: " + debugInfo);
    }

    /**
     * Logga l'output di un processo.
     *
     * @param line Linea di output del processo.
     */
    private void logProcessOutput(String line) {
        System.out.println("[PROCESS]: " + line);
    }
}
