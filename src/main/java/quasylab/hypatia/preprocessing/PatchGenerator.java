package quasylab.hypatia.preprocessing;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;

/**
 * Genera patch da immagini per super-risoluzione o rimozione dello sfondo.
 */
public class PatchGenerator {

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    /**
     * Modalità di generazione patch.
     */
    public enum Mode {
        SUPER_RESOLUTION,
        BACKGROUND_REMOVAL
    }

    /**
     * Genera patch da immagini in base alla modalità specificata.
     *
     * @param dirInput      Cartella immagini originali
     * @param dirTarget     Cartella immagini target
     * @param dirOutLR      Cartella output immagini LR o input
     * @param dirOutHR      Cartella output immagini HR o maschere
     * @param patchSizeIn   Dimensione patch input
     * @param patchSizeOut  Dimensione patch target
     * @param stride        Passo di scorrimento
     * @param mode          Modalità di generazione
     */
    public static void generatePatches(String dirInput, String dirTarget, String dirOutLR, String dirOutHR,
                                       int patchSizeIn, int patchSizeOut, int stride, Mode mode) {
        File[] inputFiles = new File(dirInput).listFiles((d, name) -> name.toLowerCase().endsWith(".png"));
        if (inputFiles == null || inputFiles.length == 0) {
            System.err.println("[ERRORE] Cartella input vuota: " + dirInput);
            return;
        }

        ensureDirectoryExists(dirOutLR);
        ensureDirectoryExists(dirOutHR);

        for (File fileIn : inputFiles) {
            String name = fileIn.getName();
            File fileTarget = new File(dirTarget, name);

            if (!fileTarget.exists()) {
                System.out.println("[WARN] Salto " + name + ": non trovato in " + dirTarget);
                continue;
            }

            Mat imgIn = Imgcodecs.imread(fileIn.getAbsolutePath(), Imgcodecs.IMREAD_COLOR);
            Mat imgTarget = Imgcodecs.imread(fileTarget.getAbsolutePath(), Imgcodecs.IMREAD_COLOR);

            if (imgIn.empty() || imgTarget.empty()) {
                System.out.println("[WARN] Immagine vuota: " + name);
                continue;
            }

            if (mode == Mode.SUPER_RESOLUTION) {
                generatePatchesSuperResolution(imgIn, imgTarget, patchSizeIn, patchSizeOut, stride, name, dirOutLR, dirOutHR);
            } else {
                generatePatchesBackgroundRemoval(imgIn, imgTarget, patchSizeIn, stride, name, dirOutLR, dirOutHR);
            }
        }
    }

    /**
     * Genera patch per super-risoluzione.
     *
     * @param imgLR       Immagine a bassa risoluzione
     * @param imgHR       Immagine ad alta risoluzione
     * @param patchSizeLR Dimensione patch LR
     * @param patchSizeHR Dimensione patch HR
     * @param stride      Passo di scorrimento
     * @param baseName    Nome base del file
     * @param outLR       Directory output LR
     * @param outHR       Directory output HR
     */
    private static void generatePatchesSuperResolution(Mat imgLR, Mat imgHR,
                                                       int patchSizeLR, int patchSizeHR, int stride,
                                                       String baseName, String outLR, String outHR) {
        int patchesY = (imgLR.rows() - patchSizeLR) / stride + 1;
        int patchesX = (imgLR.cols() - patchSizeLR) / stride + 1;

        for (int y = 0; y < patchesY; y++) {
            for (int x = 0; x < patchesX; x++) {
                int xLR = x * stride;
                int yLR = y * stride;
                int xHR = xLR * patchSizeHR / patchSizeLR;
                int yHR = yLR * patchSizeHR / patchSizeLR;

                if (xHR + patchSizeHR > imgHR.cols() || yHR + patchSizeHR > imgHR.rows()) continue;

                Rect roiLR = new Rect(xLR, yLR, patchSizeLR, patchSizeLR);
                Rect roiHR = new Rect(xHR, yHR, patchSizeHR, patchSizeHR);

                Mat patchLR = new Mat(imgLR, roiLR).clone();
                Mat patchHR = new Mat(imgHR, roiHR).clone();

                String patchName = baseName.replace(".png", "") + "_x" + x + "_y" + y + ".png";

                Imgcodecs.imwrite(outLR + File.separator + patchName, patchLR);
                Imgcodecs.imwrite(outHR + File.separator + patchName, patchHR);
            }
        }
    }

    /**
     * Genera patch per rimozione dello sfondo.
     *
     * @param imgOrig   Immagine originale
     * @param imgMask   Maschera corrispondente
     * @param patchSize Dimensione patch
     * @param stride    Passo di scorrimento
     * @param baseName  Nome base del file
     * @param outDirIn  Directory patch input
     * @param outDirOut Directory patch target
     */
    private static void generatePatchesBackgroundRemoval(Mat imgOrig, Mat imgMask,
                                                         int patchSize, int stride,
                                                         String baseName,
                                                         String outDirIn, String outDirOut) {
        if (imgOrig.rows() != imgMask.rows() || imgOrig.cols() != imgMask.cols()) {
            System.err.println("[ERRORE] Dimensioni diverse per: " + baseName);
            return;
        }

        int patchesY = (imgOrig.rows() - patchSize) / stride + 1;
        int patchesX = (imgOrig.cols() - patchSize) / stride + 1;

        for (int y = 0; y < patchesY; y++) {
            for (int x = 0; x < patchesX; x++) {
                int x0 = x * stride;
                int y0 = y * stride;

                Rect roi = new Rect(x0, y0, patchSize, patchSize);

                Mat patchIn = new Mat(imgOrig, roi).clone();
                Mat patchOut = new Mat(imgMask, roi).clone();

                String patchName = baseName.replace(".png", "") + "_x" + x + "_y" + y + ".png";

                new File(outDirIn).mkdirs();
                new File(outDirOut).mkdirs();

                Imgcodecs.imwrite(outDirIn + File.separator + patchName, patchIn);
                Imgcodecs.imwrite(outDirOut + File.separator + patchName, patchOut);
            }
        }
    }

    /**
     * Crea la cartella se non esiste.
     *
     * @param path Percorso della cartella
     */
    private static void ensureDirectoryExists(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    /**
     * Metodo di test locale per generare patch.
     *
     * @param args Argomenti da linea di comando (non usati)
     */
    public static void main(String[] args) {
        String dirInput = "dl4j dataset/originali";
        String dirTarget = "dl4j dataset/maschere";
        String dirOutIn = "dl4j dataset/patch_in";
        String dirOutOut = "dl4j dataset/patch_out";

        int patchSize = 100;
        int stride = 50;

        generatePatches(dirInput, dirTarget, dirOutIn, dirOutOut,
                patchSize, patchSize, stride, Mode.BACKGROUND_REMOVAL);

        System.out.println("✅ Generazione patch completata.");
    }
}
