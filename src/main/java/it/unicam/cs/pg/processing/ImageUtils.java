package it.unicam.cs.pg.processing;

import javafx.scene.image.WritableImage;
import org.datavec.image.loader.ImageLoader;
import org.datavec.image.loader.NativeImageLoader;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.nd4j.linalg.api.ndarray.INDArray;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayOutputStream;
import java.io.File;

/**
 * Classe di utilità per l'elaborazione e conversione di immagini tra diversi formati.
 * Supporta operazioni con OpenCV, JavaFX e AWT BufferedImage.
 */
public class ImageUtils {

    static {
        System.setProperty("java.library.path", "C:\\opencv\\build\\java\\x64");
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    /**
     * Carica un'immagine da file in formato OpenCV Mat a colori.
     *
     * @param path percorso del file immagine
     * @return immagine caricata come Mat a colori
     */
    public static Mat loadImage(String path) {
        return Imgcodecs.imread(path, Imgcodecs.IMREAD_COLOR); // <-- invece di IMREAD_GRAYSCALE
    }

    /**
     * Converte un oggetto Mat di OpenCV in un BufferedImage Java.
     *
     * @param image immagine Mat da convertire
     * @return immagine convertita in BufferedImage
     */
    public static BufferedImage matToBufferedImage(Mat image) {
        int type = (image.channels() > 1) ? BufferedImage.TYPE_3BYTE_BGR : BufferedImage.TYPE_BYTE_GRAY;

        byte[] buffer = new byte[image.channels() * image.cols() * image.rows()];
        image.get(0, 0, buffer);

        BufferedImage bufferedImage = new BufferedImage(image.cols(), image.rows(), type);
        System.arraycopy(buffer, 0,
                ((DataBufferByte) bufferedImage.getRaster().getDataBuffer()).getData(),
                0,
                buffer.length);

        return bufferedImage;
    }

    /**
     * Converte un'immagine JavaFX WritableImage in un Mat OpenCV.
     * La conversione finale restituisce un Mat in formato BGR.
     *
     * @param image immagine WritableImage di JavaFX
     * @return immagine convertita in Mat BGR
     */
    public static Mat convertFXImageToMat(WritableImage image) {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();

        byte[] buffer = new byte[width * height * 4];
        image.getPixelReader().getPixels(0, 0, width, height,
                javafx.scene.image.PixelFormat.getByteBgraInstance(),
                buffer, 0, width * 4);

        Mat mat = new Mat(height, width, CvType.CV_8UC4);
        mat.put(0, 0, buffer);

        Mat bgrMat = new Mat();
        Imgproc.cvtColor(mat, bgrMat, Imgproc.COLOR_BGRA2BGR);

        return bgrMat;
    }

    /**
     * Carica un'immagine da file in modo sicuro, usando ImageIO come fallback e ridimensionando.
     *
     * @param file     File immagine da caricare
     * @param channels Numero di canali (tipicamente 1 o 3)
     * @param width    Larghezza desiderata
     * @param height   Altezza desiderata
     * @return INDArray rappresentante l'immagine, normalizzata nel formato adatto
     */
    public static INDArray loadWithImageIOFallback(File file, int channels, int width, int height) {
        try {
            BufferedImage img = ImageIO.read(file);
            if (img == null) throw new Exception("ImageIO returned null");

            // Ridimensiona
            BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
            Graphics2D g2d = resized.createGraphics();
            g2d.drawImage(img.getScaledInstance(width, height, Image.SCALE_SMOOTH), 0, 0, null);
            g2d.dispose();

            // Usa NativeImageLoader per garantire [1, C, H, W]
            NativeImageLoader loader = new NativeImageLoader(height, width, channels);
            INDArray image = loader.asMatrix(resized);

            return image;
        } catch (Exception e) {
            System.out.println("[ERROR] Errore in loadWithImageIOFallback: " + e.getMessage());
            return org.nd4j.linalg.factory.Nd4j.ones(1, channels, height, width); // fallback
        }
    }

}
