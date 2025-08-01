package quasylab.hypatia.modelDl4jTraining;

import quasylab.hypatia.processing.ImageUtils;
import org.deeplearning4j.datasets.iterator.impl.ListDataSetIterator;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe per il training di un modello di super risoluzione tramite patch di immagini.
 */
public class SuperRisoluzionePatchTrainer {

    private final int inputSize;
    private final int outputSize;

    /**
     * Costruttore che imposta le dimensioni di input e output delle patch.
     *
     * @param inputSize  dimensione della patch in input
     * @param outputSize dimensione della patch in output
     */
    public SuperRisoluzionePatchTrainer(int inputSize, int outputSize) {
        this.inputSize = inputSize;
        this.outputSize = outputSize;
    }

    /**
     * Carica le patch di training dalle cartelle contenenti le immagini a bassa e alta risoluzione.
     * Applica la normalizzazione dei pixel nell'intervallo [-1,1].
     *
     * @param dirLR cartella contenente le patch a bassa risoluzione
     * @param dirHR cartella contenente le patch ad alta risoluzione
     * @return lista di DataSet con coppie input-label per il training
     */
    public List<DataSet> loadTrainingPatchData(File dirLR, File dirHR) {
        List<DataSet> datasetList = new ArrayList<>();
        File[] files = dirLR.listFiles((f, name) -> name.toLowerCase().endsWith(".png"));

        if (files == null || files.length == 0) {
            System.err.println("[ERROR] Cartella patch LR inesistente o vuota: " + dirLR);
            return datasetList;
        }

        ImagePreProcessingScaler scaler = new ImagePreProcessingScaler(-1, 1);
        System.out.println("[INFO] Patch LR trovate in " + dirLR + ": " + files.length);

        for (File fLR : files) {
            String name = fLR.getName();
            File fHR = new File(dirHR, name);

            if (!fHR.exists()) {
                System.out.println("[WARN] Patch HR mancante: " + name);
                continue;
            }

            INDArray input = ImageUtils.loadWithImageIOFallback(fLR, 3, inputSize, inputSize);
            INDArray label = ImageUtils.loadWithImageIOFallback(fHR, 3, outputSize, outputSize);

            scaler.transform(input);
            scaler.transform(label);

            datasetList.add(new DataSet(input, label));
        }

        System.out.println("[INFO] Patch caricate correttamente: " + datasetList.size());
        return datasetList;
    }

    /**
     * Allena il modello DL4J con i dataset forniti, suddividendo i dati in batch e per un certo numero di epoche.
     *
     * @param model       rete neurale da addestrare
     * @param datasetList lista dei DataSet di training
     * @param batchSize   dimensione del batch per ogni iterazione di training
     * @param epochs      numero di epoche di training
     */
    public void trainModel(MultiLayerNetwork model, List<DataSet> datasetList, int batchSize, int epochs) {
        ListDataSetIterator<DataSet> iterator = new ListDataSetIterator<>(datasetList, batchSize);

        for (int i = 0; i < epochs; i++) {
            System.out.println("[EPOCA " + (i + 1) + "/" + epochs + "] Inizio");

            model.fit(iterator);       // Esecuzione dell'epoca
            iterator.reset();          // Ripristina l'iteratore

            // Pulizia della memoria Java e ND4J
            System.gc(); // garbage collector Java
            org.nd4j.linalg.factory.Nd4j.getMemoryManager().invokeGc(); // libera memoria off-heap ND4J

            System.out.println("[EPOCA " + (i + 1) + "] Fine");
        }
    }


}
