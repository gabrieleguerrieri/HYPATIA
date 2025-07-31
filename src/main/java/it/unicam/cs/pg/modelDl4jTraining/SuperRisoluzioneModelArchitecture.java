package it.unicam.cs.pg.modelDl4jTraining;

import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer;
import org.deeplearning4j.nn.conf.layers.Deconvolution2D;
import org.deeplearning4j.nn.conf.layers.CnnLossLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

/**
 * Definisce l'architettura del modello di super-risoluzione basato su reti neurali convoluzionali.
 */
public class SuperRisoluzioneModelArchitecture {

    /**
     * Crea e inizializza un modello DL4J per super-risoluzione con input specificato.
     *
     * @param inputHeight altezza dell'immagine di input
     * @param inputWidth  larghezza dell'immagine di input
     * @param channels    numero di canali dell'immagine di input (es. 3 per RGB)
     * @return modello MultiLayerNetwork pronto per il training
     */
    public static MultiLayerNetwork createModel(int inputHeight, int inputWidth, int channels) {
        double learningRate = 1e-4;

        MultiLayerConfiguration config = new NeuralNetConfiguration.Builder()
                .seed(1234)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .weightInit(WeightInit.XAVIER)
                .updater(new Adam(learningRate))
                .list()
                // Encoder
                .layer(new ConvolutionLayer.Builder(3, 3)
                        .nIn(channels)
                        .nOut(64)
                        .stride(1, 1)
                        .padding(1, 1)
                        .activation(Activation.RELU)
                        .build())
                .layer(new ConvolutionLayer.Builder(3, 3)
                        .nOut(64)
                        .stride(1, 1)
                        .padding(1, 1)
                        .activation(Activation.RELU)
                        .build())
                // Primo upsampling: 100x100 → 200x200
                .layer(new Deconvolution2D.Builder(2, 2)
                        .nOut(64)
                        .stride(2, 2)
                        .padding(0, 0)
                        .activation(Activation.RELU)
                        .build())
                // Secondo upsampling: 200x200 → 400x400
                .layer(new Deconvolution2D.Builder(2, 2)
                        .nOut(64)
                        .stride(2, 2)
                        .padding(0, 0)
                        .activation(Activation.RELU)
                        .build())
                // Ricostruzione
                .layer(new ConvolutionLayer.Builder(3, 3)
                        .nOut(32)
                        .stride(1, 1)
                        .padding(1, 1)
                        .activation(Activation.RELU)
                        .build())
                .layer(new ConvolutionLayer.Builder(1, 1)
                        .nOut(channels)
                        .stride(1, 1)
                        .padding(0, 0)
                        .activation(Activation.IDENTITY)
                        .build())
                // Loss layer
                .layer(new CnnLossLayer.Builder(LossFunctions.LossFunction.MSE)
                        .activation(Activation.IDENTITY)
                        .build())
                .setInputType(InputType.convolutional(inputHeight, inputWidth, channels))
                .build();

        MultiLayerNetwork model = new MultiLayerNetwork(config);
        model.init();
        return model;
    }
}

