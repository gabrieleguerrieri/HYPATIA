package it.unicam.cs.pg.processing;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.opencv.core.Mat;
import java.io.File;
import java.io.IOException;

/**
 * Classe per l'elaborazione OCR (Optical Character Recognition) utilizzando Tesseract.
 * Fornisce metodi per l'estrazione di testo da immagini utilizzando sia modelli predefiniti
 * che modelli personalizzati addestrati.
 */
public class OCR {
    private static final Tesseract tesseract;

    // Inizializzazione del motore OCR con configurazioni di base
    static {
        tesseract = new Tesseract();
        tesseract.setLanguage("eng");
    }

    /**
     * Esegue il riconoscimento del testo usando il modello standard di Tesseract.
     *
     * @param image Matrice OpenCV contenente l'immagine da processare
     * @param language Lingua da utilizzare per il riconoscimento (es. "ita", "eng")
     * @return Stringa contenente il testo riconosciuto
     * @throws TesseractException in caso di errori durante il riconoscimento
     * @throws IOException in caso di problemi di conversione dell'immagine
     */
    public static String easyOCRStandardModel(Mat image, String language) throws TesseractException, IOException {
        tesseract.setLanguage(language);
        String result = tesseract.doOCR(ImageUtils.matToBufferedImage(image));
        return result;
    }

    /**
     * Esegue il riconoscimento del testo usando un modello personalizzato.
     *
     * @param image Matrice OpenCV contenente l'immagine da processare
     * @param trainedDataFile File .traineddata del modello personalizzato
     * @return Stringa contenente il testo riconosciuto
     * @throws TesseractException in caso di errori durante il riconoscimento
     * @throws IOException in caso di problemi di conversione dell'immagine
     */
    public static String customModel(Mat image, File trainedDataFile) throws TesseractException, IOException {
        Tesseract customTesseract = new Tesseract();
        String parentDir = trainedDataFile.getParent();

        // Configurazione del percorso dati e lingua dal nome file
        customTesseract.setDatapath(parentDir);
        String language = trainedDataFile.getName().replace(".traineddata", "");
        customTesseract.setLanguage(language);

        return customTesseract.doOCR(ImageUtils.matToBufferedImage(image));
    }

}