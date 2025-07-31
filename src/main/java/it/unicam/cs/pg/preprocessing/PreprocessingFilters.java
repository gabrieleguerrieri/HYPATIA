package it.unicam.cs.pg.preprocessing;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

/**
 * Classe dedicata all'applicazione di filtri di pre-elaborazione per immagini utilizzando OpenCV.
 * Offre metodi statici per migliorare la qualità delle immagini prima di ulteriori elaborazioni.
 */
public class PreprocessingFilters {

    // Caricamento della libreria nativa OpenCV
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    /**
     * Applica l'adaptive thresholding per la binarizzazione dell'immagine.
     *
     * @param input Immagine sorgente in formato BGR o scala di grigi
     * @return Immagine binarizzata con soglia adattativa
     */
    public static Mat adaptiveThreshold(Mat input) {
        Mat gray = new Mat();

        // Conversione in scala di grigi se necessario
        if (input.channels() > 1) {
            Imgproc.cvtColor(input, gray, Imgproc.COLOR_BGR2GRAY);
        } else {
            gray = input.clone();
        }

        Mat output = new Mat();
        Imgproc.adaptiveThreshold(
                gray,
                output,
                255,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY,
                11,
                2
        );

        return output;
    }

    /**
     * Rileva i bordi nell'immagine utilizzando l'algoritmo Canny.
     *
     * @param input Immagine sorgente in scala di grigi
     * @return Mappa dei bordi rilevati
     */
    public static Mat edgeDetection(Mat input) {
        Mat blurred = new Mat();
        Mat edges = new Mat();

        // Riduzione del rumore con filtro Gaussiano
        Imgproc.GaussianBlur(input, blurred, new Size(5, 5), 0);

        // Applicazione edge detection con soglie ottimali
        Imgproc.Canny(blurred, edges, 30, 150);

        return edges;
    }

    /**
     * Applica un filtro mediano per la riduzione del rumore.
     *
     * @param image Immagine sorgente
     * @return Immagine filtrata con kernel 5x5
     */
    public static Mat medianFilter(Mat image) {
        Mat result = new Mat();
        Imgproc.medianBlur(image, result, 5);
        return result;
    }

    /**
     * Applica un filtro Gaussiano per la riduzione del rumore.
     *
     * @param image Immagine sorgente
     * @return Immagine filtrata con kernel 5x5
     */
    public static Mat gaussianFilter(Mat image) {
        Mat result = new Mat();
        Imgproc.GaussianBlur(image, result, new Size(5, 5), 0);
        return result;
    }
}