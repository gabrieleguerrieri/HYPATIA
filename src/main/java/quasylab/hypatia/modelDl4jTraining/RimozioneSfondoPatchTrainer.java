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
 * Addestra modelli DL4J per la rimozione dello sfondo tramite patch di immagini.
 */
public class RimozioneSfondoPatchTrainer {

    private final int patchSize;

    /**
     * Costruttore.
     *
     * @param patchSize dimensione delle patch quadrate
     */
    public RimozioneSfondoPatchTrainer(int patchSize) {
        this.patchSize = patchSize;
    }

    /**
     * Carica dati di training da directory di input/output come liste di DataSet.
     *
     * @param dirInput  directory delle immagini di input
     * @param dirOutput directory delle immagini di output (ground truth)
     * @return lista di DataSet per il training
     */
    public List<DataSet> loadTrainingPatchData(File dirInput, File dirOutput) {
        List<DataSet> datasetList = new ArrayList<>();
        File[] files = dirInput.listFiles((f, name) -> name.toLowerCase().endsWith(".png"));

        ImagePreProcessingScaler scaler = new ImagePreProcessingScaler(-1, 1);

        for (File fInput : files) {
            String name = fInput.getName();
            File fOutput = new File(dirOutput, name);
            if (!fOutput.exists()) continue;

            INDArray input = ImageUtils.loadWithImageIOFallback(fInput, 3, patchSize, patchSize);
            INDArray label = ImageUtils.loadWithImageIOFallback(fOutput, 3, patchSize, patchSize);


            scaler.transform(input);
            scaler.transform(label);

            datasetList.add(new DataSet(input, label));
        }

        return datasetList;
    }

    /**
     * Esegue il training del modello su batch di dati per un numero di epoche specificato.
     *
     * @param model       modello DL4J da addestrare
     * @param datasetList lista di DataSet per il training
     * @param batchSize   dimensione batch
     * @param epochs      numero di epoche di training
     */
    public void trainModel(MultiLayerNetwork model, List<DataSet> datasetList, int batchSize, int epochs) {
        ListDataSetIterator<DataSet> iterator = new ListDataSetIterator<>(datasetList, batchSize);

        for (int i = 0; i < epochs; i++) {
            System.out.println("[EPOCA " + (i + 1) + "/" + epochs + "] Inizio");

            model.fit(iterator);
            iterator.reset();

            // Pulizia memoria Java e nativa
            System.gc(); // memoria heap
            org.nd4j.linalg.factory.Nd4j.getMemoryManager().invokeGc(); // memoria off-heap (ND4J)

            System.out.println("[EPOCA " + (i + 1) + "] Fine");
        }
    }

}
