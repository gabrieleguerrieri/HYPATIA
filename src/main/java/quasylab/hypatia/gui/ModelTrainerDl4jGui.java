package quasylab.hypatia.gui;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.datavec.image.loader.NativeImageLoader;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import quasylab.hypatia.modelDl4jTraining.ImagePreprocessor;
import quasylab.hypatia.modelDl4jTraining.RimozioneSfondoModelArchitecture;
import quasylab.hypatia.modelDl4jTraining.SuperRisoluzioneModelArchitecture;
import quasylab.hypatia.modelDl4jTraining.SuperRisoluzionePatchTrainer;
import quasylab.hypatia.preprocessing.PatchGenerator;
import quasylab.hypatia.modelDl4jTraining.RimozioneSfondoPatchTrainer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * Applicazione GUI per il training di un modello DL4J per la rimozione di sfondo e rumore.
 * Gestisce la preparazione del dataset (processamento immagini), l’addestramento e il salvataggio del modello.
 */
public class ModelTrainerDl4jGui extends Application {

    private static final String PROJECT_ROOT = System.getProperty("user.dir");
    private static final String DATASET_DIR = PROJECT_ROOT + File.separator + "dl4j dataset";
    private static final String PATH_ORIGINALI = DATASET_DIR + File.separator + "originali";
    private static final String PATH_MASCHE = DATASET_DIR + File.separator + "maschere";

    private TextArea areaLog;
    private ProgressBar barraProgresso;
    private MultiLayerNetwork modello;
    private MultiLayerNetwork modelloSuperResolution;
    private TextField txtEpochs;

    /**
     * Punto d'ingresso principale dell'applicazione.
     *
     * @param args argomenti della riga di comando
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Metodo principale per avviare l'applicazione JavaFX.
     *
     * @param stagePrincipale la finestra principale dell'applicazione
     */
    @Override
    public void start(Stage stagePrincipale) {
        stagePrincipale.setTitle("Training - Rimozione Sfondo, Riduzione Rumore e Super-risoluzione");
        creaCartelleDataset();

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10));
        grid.setVgap(8);
        grid.setHgap(10);

        // === BLOCCO 1: Patch e training rimozione sfondo / denoising ===
        HBox hBoxPatchGen = new HBox(10);
        Button btnGenPatches = new Button("Genera Patch Rimozione Sfondo / Denoising");
        btnGenPatches.setPrefWidth(280);
        btnGenPatches.setOnAction(e -> generaPatchesRimozioneSfondo());
        Label lblGenPatches = new Label("Crea patch (100x100) da originali e maschere per training (sfondo o denoising).");
        lblGenPatches.setWrapText(true);
        lblGenPatches.setMaxWidth(280);
        hBoxPatchGen.getChildren().addAll(btnGenPatches, lblGenPatches);
        grid.add(hBoxPatchGen, 0, 0, 2, 1);

        HBox hBoxTrain = new HBox(10);
        Button btnTrain = new Button("Addestra Modello Rimozione Sfondo / Denoising");
        btnTrain.setPrefWidth(280);
        btnTrain.setOnAction(e -> addestraModelloRimozioneSfondoPatch());
        Label lblTrain = new Label("Addestra il modello usando le patch generate.");
        lblTrain.setWrapText(true);
        lblTrain.setMaxWidth(280);
        hBoxTrain.getChildren().addAll(btnTrain, lblTrain);
        grid.add(hBoxTrain, 0, 1, 2, 1);

        // === BLOCCO 2: Patch e training super risoluzione ===
        HBox hBoxBtn3 = new HBox(10);
        hBoxBtn3.setPadding(new Insets(20, 0, 0, 0));
        Button btnGeneraPatches = new Button("Genera Patch Super Resolution");
        btnGeneraPatches.setPrefWidth(280);
        btnGeneraPatches.setOnAction(e -> generaPatchesSuperResolution());
        Label lblGeneraPatches = new Label("Crea patch (100x100 → 400x400) per la super risoluzione.");
        lblGeneraPatches.setWrapText(true);
        lblGeneraPatches.setMaxWidth(280);
        hBoxBtn3.getChildren().addAll(btnGeneraPatches, lblGeneraPatches);
        grid.add(hBoxBtn3, 0, 2, 2, 1);

        HBox hBoxBtn4 = new HBox(10);
        Button btnAddestraSR = new Button("Addestra Modello Super Resolution");
        btnAddestraSR.setPrefWidth(280);
        btnAddestraSR.setOnAction(e -> addestraModelloSuperResolution());
        Label lblAddestraSR = new Label("Addestra un modello su patch per la super risoluzione.");
        lblAddestraSR.setWrapText(true);
        lblAddestraSR.setMaxWidth(280);
        hBoxBtn4.getChildren().addAll(btnAddestraSR, lblAddestraSR);
        grid.add(hBoxBtn4, 0, 3, 2, 1);

        // Campo epoche
        HBox hBoxEpoche = new HBox(10);
        hBoxEpoche.setPadding(new Insets(20, 0, 0, 0));
        Label lblEpochs = new Label("Numero di Epoche:");
        txtEpochs = new TextField("50");
        txtEpochs.setPrefWidth(60);
        hBoxEpoche.getChildren().addAll(lblEpochs, txtEpochs);
        grid.add(hBoxEpoche, 0, 4, 2, 1);

        // Area log
        areaLog = new TextArea();
        areaLog.setEditable(false);
        areaLog.setWrapText(true);
        areaLog.setPrefHeight(150);
        grid.add(areaLog, 0, 5, 2, 1);

        // Pulsante salva modello
        HBox hBoxSalva = new HBox(10);
        hBoxSalva.setPadding(new Insets(5, 0, 5, 0));
        Button btnSalvaModello = new Button("Salva Modello DL4J");
        btnSalvaModello.setPrefWidth(280);
        btnSalvaModello.setOnAction(e -> {
            if (modello == null && modelloSuperResolution == null) {
                areaLog.appendText("[ERRORE] Nessun modello da salvare.\n");
                return;
            }

            File dir = new File(PROJECT_ROOT + File.separator + "dl4j model");
            if (!dir.exists() && !dir.mkdirs()) {
                areaLog.appendText("[ERRORE] Impossibile creare la cartella 'dl4j model'.\n");
                return;
            }

            try {
                // Modello rimozione sfondo / denoising
                if (modello != null) {
                    Optional<String> nomeOpt = chiediNomeModello("modello_sfondo");
                    if (nomeOpt.isPresent()) {
                        String nomeFile = nomeOpt.get().trim();
                        if (!nomeFile.endsWith(".zip")) nomeFile += ".zip";
                        File fileDestinazione = new File(dir, nomeFile);
                        modello.save(fileDestinazione);
                        areaLog.appendText("[SUCCESSO] Modello salvato in: " + fileDestinazione.getAbsolutePath() + "\n");
                    } else {
                        areaLog.appendText("[INFO] Salvataggio modello sfondo annullato.\n");
                    }
                }

                // Modello super risoluzione
                if (modelloSuperResolution != null) {
                    Optional<String> nomeOpt = chiediNomeModello("modello_superrisoluzione");
                    if (nomeOpt.isPresent()) {
                        String nomeFile = nomeOpt.get().trim();
                        if (!nomeFile.endsWith(".zip")) nomeFile += ".zip";
                        File fileDestinazione = new File(dir, nomeFile);
                        modelloSuperResolution.save(fileDestinazione);
                        areaLog.appendText("[SUCCESSO] Modello SUPER RISOLUZIONE salvato in: " + fileDestinazione.getAbsolutePath() + "\n");
                    } else {
                        areaLog.appendText("[INFO] Salvataggio modello super risoluzione annullato.\n");
                    }
                }

            } catch (Exception ex) {
                areaLog.appendText("[ERRORE] Salvataggio modello fallito: " + ex.getMessage() + "\n");
                ex.printStackTrace();
            }
        });

        Label lblSalva = new Label("Salva il modello addestrato in un file ZIP.");
        lblSalva.setWrapText(true);
        lblSalva.setMaxWidth(280);
        hBoxSalva.getChildren().addAll(btnSalvaModello, lblSalva);
        grid.add(hBoxSalva, 0, 6, 2, 1);

        // Barra di progresso
        barraProgresso = new ProgressBar(0);
        barraProgresso.setVisible(false);
        grid.add(barraProgresso, 0, 7, 2, 1);

        stagePrincipale.setScene(new Scene(grid, 600, 600));
        stagePrincipale.setResizable(false);
        stagePrincipale.show();
    }

    /**
     * Crea le cartelle necessarie per il dataset se non esistono già.
     */
    private void creaCartelleDataset() {
        File dirDataset = new File(DATASET_DIR);
        if (!dirDataset.exists() && dirDataset.mkdirs()) {
            System.out.println("[INFO] Cartella dataset creata: " + dirDataset.getAbsolutePath());
        }
        File dirOriginali = new File(PATH_ORIGINALI);
        if (!dirOriginali.exists() && dirOriginali.mkdirs()) {
            System.out.println("[INFO] Cartella 'originali' creata: " + dirOriginali.getAbsolutePath());
        }
        File dirMaschere = new File(PATH_MASCHE);
        if (!dirMaschere.exists() && dirMaschere.mkdirs()) {
            System.out.println("[INFO] Cartella 'maschere' creata: " + dirMaschere.getAbsolutePath());
        }
    }

    /**
     * Processa le immagini presenti nelle cartelle specificate per prepararle al training.
     */
    public void processaImmagini() {
        areaLog.appendText("[INFO] Avvio preprocessing immagini...\n");
        File cartellaOriginali = new File(PATH_ORIGINALI);
        File cartellaMaschere = new File(PATH_MASCHE);
        if (!cartellaOriginali.exists() || !cartellaMaschere.exists()) {
            areaLog.appendText("[ERRORE] Cartelle non trovate: " + PATH_ORIGINALI + " o " + PATH_MASCHE + "\n");
            return;
        }
        List<File> listaOriginali = new ArrayList<>();
        List<File> listaMaschere = new ArrayList<>();
        caricaImmaginiDaCartella(cartellaOriginali, listaOriginali);
        caricaImmaginiDaCartella(cartellaMaschere, listaMaschere);

        for (File file : listaOriginali) {
            try {
                ImagePreprocessor.processAndSaveImage(file, PATH_ORIGINALI, areaLog);
            } catch (Exception ex) {
                areaLog.appendText("[ERRORE] Preprocessing immagine originale fallito: "
                        + file.getName() + " - " + ex.getMessage() + "\n");
            }
        }

        for (File file : listaMaschere) {
            try {
                ImagePreprocessor.processAndSaveImage(file, PATH_MASCHE, areaLog);
            } catch (Exception ex) {
                areaLog.appendText("[ERRORE] Preprocessing maschera fallito: "
                        + file.getName() + " - " + ex.getMessage() + "\n");
            }
        }

        areaLog.appendText("[INFO] Preprocessing completato.\n");
        verificaImmaginiOpenCV(PATH_ORIGINALI);
        verificaImmaginiOpenCV(PATH_MASCHE);
    }

    /**
     * Carica le immagini da una cartella specificata e le aggiunge a una lista.
     *
     * @param cartella      la cartella da cui caricare le immagini
     * @param listaImmagini la lista in cui aggiungere le immagini caricate
     */
    private void caricaImmaginiDaCartella(File cartella, List<File> listaImmagini) {
        File[] files = cartella.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
        if (files != null && files.length > 0) {
            listaImmagini.addAll(Arrays.asList(files));
            areaLog.appendText("[INFO] Trovate " + files.length + " immagini .png"
                    + " in " + cartella.getAbsolutePath() + "\n");
        } else {
            areaLog.appendText("[ERRORE] Nessuna immagine .png"
                    + " trovata in " + cartella.getAbsolutePath() + "\n");
        }
    }

    /**
     * Verifica che le immagini nella cartella specificata possano essere caricate correttamente da OpenCV.
     *
     * @param cartellaPath il percorso della cartella contenente le immagini
     */
    public void verificaImmaginiOpenCV(String cartellaPath) {
        File cartella = new File(cartellaPath);
        File[] files = cartella.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
        if (files == null) {
            areaLog.appendText("[ERRORE] Cartella non esiste o non contiene PNG: " + cartellaPath + "\n");
            return;
        }
        areaLog.appendText("[INFO] Verifica immagini in: " + cartellaPath + "\n");
        for (File file : files) {
            Mat img = Imgcodecs.imread(file.getAbsolutePath(), Imgcodecs.IMREAD_UNCHANGED);
            if (img.empty()) {
                areaLog.appendText("[ERRORE] OpenCV non ha caricato l'immagine: "
                        + file.getAbsolutePath() + "\n");
            } else {
                areaLog.appendText("[INFO] Immagine caricata: " + file.getAbsolutePath()
                        + " (" + img.width() + "x" + img.height() + ")\n");
            }
        }
    }

    /**
     * Carica un'immagine utilizzando ImageIO e applica un padding bianco per uniformare le dimensioni.
     *
     * @param file      il file immagine da caricare
     * @param channels  il numero di canali dell'immagine (es. 3 per RGB)
     * @return un array INDArray contenente i dati dell'immagine
     * @throws IOException se si verifica un errore durante il caricamento dell'immagine
     */
    private INDArray loadImageWithImageIO(File file, int channels) throws IOException {
        BufferedImage bf = ImageIO.read(file);
        if (bf == null) {
            throw new IOException("ImageIO non riconosce il file: " + file.getName());
        }
        bf.getWidth();
        bf.getHeight();

        int imageType = (channels == 1) ? BufferedImage.TYPE_BYTE_GRAY : BufferedImage.TYPE_3BYTE_BGR;
        BufferedImage paddedImage = new BufferedImage(1200, 1700, imageType);

        Graphics2D g2d = paddedImage.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, 1200, 1700);

        int x = 0;
        int y = 0;
        g2d.drawImage(bf, x, y, null);
        g2d.dispose();

        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        String formatName = "png";
        ImageIO.write(paddedImage, formatName, baos);
        baos.flush();
        java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(baos.toByteArray());
        NativeImageLoader loader = new NativeImageLoader(1700, 1200, channels);
        return loader.asMatrix(bais);
    }

    /**
     * Genera patch a bassa e alta risoluzione per il training del modello di super risoluzione.
     * Le patch vengono create dalle immagini originali e dalle maschere.
     */
    private void generaPatchesSuperResolution() {
        areaLog.appendText("[INFO] Generazione dataset patch per super risoluzione...\n");
        String outLR = DATASET_DIR + File.separator + "patch_lr";
        String outHR = DATASET_DIR + File.separator + "patch_hr";
        new File(outLR).mkdirs();
        new File(outHR).mkdirs();
        PatchGenerator.generatePatches(PATH_ORIGINALI, PATH_MASCHE, outLR, outHR, 100, 400, 100, PatchGenerator.Mode.SUPER_RESOLUTION);

        areaLog.appendText("[SUCCESSO] Patch per super risoluzione generate in:\n  " + outLR + "\n  " + outHR + "\n");
    }

    /**
     * Genera patch di input e output per il training del modello di rimozione dello sfondo.
     * Le patch vengono create dalle immagini originali (input) e dalle maschere (output).
     */
    private void generaPatchesRimozioneSfondo() {
        areaLog.appendText("[INFO] Generazione patch per rimozione sfondo...\n");
        String outLR = DATASET_DIR + File.separator + "patch_in";
        String outHR = DATASET_DIR + File.separator + "patch_out";
        new File(outLR).mkdirs();
        new File(outHR).mkdirs();
        PatchGenerator.generatePatches(PATH_ORIGINALI, PATH_MASCHE, outLR, outHR, 100, 100, 100, PatchGenerator.Mode.BACKGROUND_REMOVAL);
        areaLog.appendText("[SUCCESSO] Patch generate in:\n  " + outLR + "\n  " + outHR + "\n");
    }

    /**
     * Addestra un modello di super risoluzione su patch di immagini.
     * Mostra i log e la barra di avanzamento durante il processo.
     */
    private void addestraModelloSuperResolution() {
        barraProgresso.setVisible(true);
        barraProgresso.setProgress(0.1);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                areaLog.appendText("[INFO] Inizio addestramento modello SUPER RISOLUZIONE...\n");

                try {
                    int numEpochs;
                    try {
                        numEpochs = Integer.parseInt(txtEpochs.getText());
                    } catch (NumberFormatException ex) {
                        areaLog.appendText("[ERRORE] Numero di epoche non valido, uso valore di default 50.\n");
                        numEpochs = 50;
                    }
                    var arch = new SuperRisoluzioneModelArchitecture();
                    MultiLayerNetwork model = arch.createModel(100, 100, 3);
                    SuperRisoluzionePatchTrainer trainer = new SuperRisoluzionePatchTrainer(100, 400);
                    File dirLR = new File(DATASET_DIR + File.separator + "patch_lr");
                    File dirHR = new File(DATASET_DIR + File.separator + "patch_hr");
                    List<DataSet> data = trainer.loadTrainingPatchData(dirLR, dirHR);
                    if (data.isEmpty()) {
                        areaLog.appendText("[ERRORE] Nessuna patch valida per l'addestramento.\n");
                        return null;
                    }
                    trainer.trainModel(model, data, 8, numEpochs);
                    modelloSuperResolution = model;
                    areaLog.appendText("[SUCCESSO] Addestramento modello SUPER RISOLUZIONE completato.\n");
                    areaLog.appendText("[INFO] Salva il modello se vuoi conservarlo (pulsante in basso).\n");

                    barraProgresso.setProgress(1.0);
                } catch (Exception ex) {
                    areaLog.appendText("[ERRORE] Eccezione durante l'addestramento: " + ex.getMessage() + "\n");
                    ex.printStackTrace();
                }
                return null;
            }
        };
        task.setOnSucceeded(e -> barraProgresso.setVisible(false));
        task.setOnFailed(e -> {
            barraProgresso.setVisible(false);
            areaLog.appendText("[ERRORE] Addestramento modello SUPER RISOLUZIONE fallito.\n");
        });
        new Thread(task).start();
    }

    /**
     * Addestra un modello per la rimozione dello sfondo utilizzando le patch di input/output.
     * Visualizza i log e la barra di avanzamento durante l'addestramento.
     */
    private void addestraModelloRimozioneSfondoPatch() {
        barraProgresso.setVisible(true);
        barraProgresso.setProgress(0.1);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                areaLog.appendText("[INFO] Inizio addestramento PATCH RIMOZIONE SFONDO...\n");

                try {
                    int numEpochs;
                    try {
                        numEpochs = Integer.parseInt(txtEpochs.getText());
                    } catch (NumberFormatException ex) {
                        areaLog.appendText("[ERRORE] Epoche non valide, uso 50.\n");
                        numEpochs = 50;
                    }

                    MultiLayerNetwork model = RimozioneSfondoModelArchitecture.createModel(100, 100);
                    RimozioneSfondoPatchTrainer trainer = new RimozioneSfondoPatchTrainer(100);

                    File dirInput = new File(DATASET_DIR + File.separator + "patch_in");
                    File dirOutput = new File(DATASET_DIR + File.separator + "patch_out");

                    List<DataSet> data = trainer.loadTrainingPatchData(dirInput, dirOutput);
                    if (data.isEmpty()) {
                        areaLog.appendText("[ERRORE] Nessuna patch valida per l'addestramento.\n");
                        return null;
                    }

                    trainer.trainModel(model, data, 8, numEpochs);
                    modello = model;

                    areaLog.appendText("[SUCCESSO] Addestramento PATCH RIMOZIONE SFONDO completato.\n");
                    areaLog.appendText("[INFO] Salva il modello se vuoi conservarlo.\n");

                    barraProgresso.setProgress(1.0);
                } catch (Exception ex) {
                    areaLog.appendText("[ERRORE] Eccezione durante l'addestramento: " + ex.getMessage() + "\n");
                    ex.printStackTrace();
                }

                return null;
            }
        };

        task.setOnSucceeded(e -> barraProgresso.setVisible(false));
        task.setOnFailed(e -> {
            barraProgresso.setVisible(false);
            areaLog.appendText("[ERRORE] Addestramento fallito.\n");
        });

        new Thread(task).start();
    }

    /**
     * Salva il modello addestrato in un file ZIP.
     */
    private void salvaModello() {
        if (modello == null && modelloSuperResolution == null) {
            areaLog.appendText("[ERRORE] Nessun modello da salvare.\n");
            return;
        }
        File dir = new File(PROJECT_ROOT + File.separator + "dl4j model");
        if (!dir.exists() && !dir.mkdirs()) {
            areaLog.appendText("[ERRORE] Impossibile creare la cartella 'dl4j model'.\n");
            return;
        }
        try {
            if (modello != null) {
                File fileDestinazione = new File(dir, "modello_rimozionesfondo.zip");
                modello.save(fileDestinazione);
                areaLog.appendText("[SUCCESSO] Modello RIMOZIONE SFONDO salvato in: " + fileDestinazione.getAbsolutePath() + "\n");

            }
            if (modelloSuperResolution != null) {
                File fileDestinazione = new File(dir, "modello_superrisoluzione.zip");
                modelloSuperResolution.save(fileDestinazione);
                areaLog.appendText("[SUCCESSO] Modello SUPER RISOLUZIONE salvato in: " + fileDestinazione.getAbsolutePath() + "\n");

            }
        } catch (Exception ex) {
            areaLog.appendText("[ERRORE] Salvataggio modello fallito: " + ex.getMessage() + "\n");
        }
    }

    /**
     * Mostra una finestra di dialogo per inserire il nome del modello da salvare.
     *
     * @param suggerimento il nome predefinito suggerito all'utente
     * @return un oggetto Optional contenente il nome del file inserito, se presente
     */
    private Optional<String> chiediNomeModello(String suggerimento) {
        TextInputDialog dialog = new TextInputDialog(suggerimento);
        dialog.setTitle("Salvataggio Modello");
        dialog.setHeaderText("Inserisci il nome del file (senza estensione .zip)");
        dialog.setContentText("Nome modello:");
        return dialog.showAndWait();
    }

}