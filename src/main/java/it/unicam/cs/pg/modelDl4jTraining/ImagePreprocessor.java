package it.unicam.cs.pg.modelDl4jTraining;

import javafx.scene.control.TextArea;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;

public class ImagePreprocessor {

    /**
     * Carica, elabora e salva un'immagine mantenendo le proporzioni, ridimensionandola a 1200x1700 px.
     *
     * @param file              il file immagine da processare
     * @param destinationFolder la cartella in cui salvare l'immagine processata
     * @param logger            un TextArea per il log (può essere null)
     */
    public static void processAndSaveImage(File file, String destinationFolder, TextArea logger) {
        if (!file.exists() || file.length() == 0) {
            if (logger != null) {
                logger.appendText("[ERRORE] File non valido: " + file.getAbsolutePath() + "\n");
            }
            return;
        }

        Mat img = Imgcodecs.imread(file.getAbsolutePath(), Imgcodecs.IMREAD_UNCHANGED);
        if (img.empty()) {
            if (logger != null) {
                logger.appendText("[ERRORE] OpenCV non ha caricato l'immagine: " + file.getAbsolutePath() + "\n");
            }
            return;
        }

        // Conversione se necessario
        if (img.channels() == 4) {
            Mat converted = new Mat();
            Imgproc.cvtColor(img, converted, Imgproc.COLOR_BGRA2BGR);
            img = converted;
        } else if (img.channels() == 1) {
            Mat converted = new Mat();
            Imgproc.cvtColor(img, converted, Imgproc.COLOR_GRAY2BGR);
            img = converted;
        }

        int targetWidth = 1200;
        int targetHeight = 1700;
        double aspectRatio = (double) img.width() / img.height();

        int newWidth, newHeight;
        if (aspectRatio > (double) targetWidth / targetHeight) {
            newWidth = targetWidth;
            newHeight = (int) (targetWidth / aspectRatio);
        } else {
            newHeight = targetHeight;
            newWidth = (int) (targetHeight * aspectRatio);
        }

        Mat imgResized = new Mat();
        Imgproc.resize(img, imgResized, new Size(newWidth, newHeight));

        Mat imgPadded = new Mat(targetHeight, targetWidth, CvType.CV_8UC3, new Scalar(255, 255, 255));
        int x = (targetWidth - newWidth) / 2;
        int y = (targetHeight - newHeight) / 2;
        Rect roi = new Rect(x, y, newWidth, newHeight);
        imgResized.copyTo(imgPadded.submat(roi));

        String fileName = file.getName();
        int index = fileName.lastIndexOf(".");
        String extension = (index > 0) ? fileName.substring(index + 1) : "png";
        String nomeFileSenzaEstensione = fileName.substring(0, index);
        String nomeNuovoFile = nomeFileSenzaEstensione + "." + extension;
        File outputFile = new File(destinationFolder, nomeNuovoFile);

        MatOfInt parametri = new MatOfInt(Imgcodecs.IMWRITE_PNG_COMPRESSION, 3);
        boolean risultato = Imgcodecs.imwrite(outputFile.getAbsolutePath(), imgPadded, parametri);

        if (logger != null) {
            if (risultato) {
                logger.appendText("[SUCCESSO] Immagine salvata: " + outputFile.getAbsolutePath() + "\n");
            } else {
                logger.appendText("[ERRORE] Salvataggio immagine fallito: " + file.getName() + "\n");
            }
        }
    }
}
