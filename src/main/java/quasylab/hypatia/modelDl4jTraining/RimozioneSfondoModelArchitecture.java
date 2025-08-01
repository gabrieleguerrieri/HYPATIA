package quasylab.hypatia.modelDl4jTraining;

import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer;
import org.deeplearning4j.nn.conf.layers.CnnLossLayer;
import org.deeplearning4j.nn.conf.layers.Deconvolution2D;
import org.deeplearning4j.nn.conf.layers.SubsamplingLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

/**
 * Modello CNN per rimozione dello sfondo da immagini a colori.
 */
public class RimozioneSfondoModelArchitecture {

    public static MultiLayerNetwork createModel(int width, int height) {
        int channels = 3;
        double lr = 1e-3;

        /**
         * Crea e inizializza il modello.
         *
         * @param width  larghezza input
         * @param height altezza input
         * @return modello CNN pronto all'uso
         */
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(123)
                .updater(new Adam(lr))
                .weightInit(WeightInit.XAVIER)
                .list()
                // Encoder
                .layer(new ConvolutionLayer.Builder(3, 3).nIn(channels).nOut(32).stride(1, 1).padding(1, 1).activation(Activation.RELU).build())
                .layer(new ConvolutionLayer.Builder(3, 3).nOut(32).stride(1, 1).padding(1, 1).activation(Activation.RELU).build())
                .layer(new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX).kernelSize(2, 2).stride(2, 2).build())
                .layer(new ConvolutionLayer.Builder(3, 3).nOut(64).stride(1, 1).padding(1, 1).activation(Activation.RELU).build())
                .layer(new ConvolutionLayer.Builder(3, 3).nOut(64).stride(1, 1).padding(1, 1).activation(Activation.RELU).build())
                .layer(new SubsamplingLayer.Builder(SubsamplingLayer.PoolingType.MAX).kernelSize(2, 2).stride(2, 2).build())
                .layer(new ConvolutionLayer.Builder(3, 3).nOut(128).stride(1, 1).padding(1, 1).activation(Activation.RELU).build())
                // Decoder
                .layer(new Deconvolution2D.Builder(2, 2).nOut(64).stride(2, 2).padding(0, 0).activation(Activation.RELU).build())
                .layer(new ConvolutionLayer.Builder(3, 3).nOut(64).stride(1, 1).padding(1, 1).activation(Activation.RELU).build())
                .layer(new Deconvolution2D.Builder(2, 2).nOut(32).stride(2, 2).padding(0, 0).activation(Activation.RELU).build())
                .layer(new ConvolutionLayer.Builder(3, 3).nOut(32).stride(1, 1).padding(1, 1).activation(Activation.RELU).build())
                // Output layer: IDENTITY su 3 canali
                .layer(new ConvolutionLayer.Builder(1, 1).nOut(3).stride(1, 1).padding(0, 0).activation(Activation.IDENTITY).build())
                .layer(new CnnLossLayer.Builder()
                        .activation(Activation.IDENTITY)
                        .lossFunction(LossFunctions.LossFunction.MSE)
                        .build())
                .setInputType(InputType.convolutional(height, width, channels))
                .build();

        MultiLayerNetwork model = new MultiLayerNetwork(conf);
        model.init();
        return model;
    }


}
