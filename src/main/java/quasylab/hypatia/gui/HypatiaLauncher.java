package quasylab.hypatia.gui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.opencv.core.Core;

/**
 * Classe principale che rappresenta il launcher dell'applicazione HYPATIA.
 * HYPATIA sta per "Hierarchical Yielding Processing for Automatic Text Interpretation and Analysis".
 * Questa classe estende {@link Application} di JavaFX e fornisce un'interfaccia grafica
 * per selezionare e avviare diversi moduli dell'applicazione.
 */
public class HypatiaLauncher extends Application {

    static {
        // Carica la libreria nativa di OpenCV necessaria per le operazioni di elaborazione immagini
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    /**
     * Metodo principale di avvio dell'applicazione JavaFX.
     * @param args argomenti da linea di comando (non utilizzati)
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Avvia l'interfaccia grafica dell'applicazione, configurando la finestra principale
     * e i pulsanti per accedere ai diversi moduli della suite HYPATIA.
     * @param primaryStage lo stage principale fornito da JavaFX
     */
    @Override
    public void start(Stage primaryStage) {
        // Titolo finestra
        primaryStage.setTitle("HYPATIA - Elaborazione Gerarchica Automatica per l’Analisi del Testo");

        // Titolo e descrizione dell'applicazione
        Text title = new Text("H Y P A T I A");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setStyle("-fx-fill: #555555;");

        Text acronym = new Text("Hierarchical Yielding Processing for Automatic Text Interpretation and Analysis");
        acronym.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        acronym.setStyle("-fx-fill: #555555;");

        Label description = new Label("Seleziona un modulo per iniziare:");
        description.setFont(Font.font("Arial", FontWeight.NORMAL, 12));

        // Pulsante e descrizione per Pre-elaborazione Immagini
        Button preprocessingButton = new Button("Pre-elaborazione Immagini\nPre processing");
        Label preprocessingDescription = new Label(
                "Preelaborazione delle immagini con testo utilizzando CNN o filtri matematici per migliorare la qualità e l'analisi dei dati."
        );

        // Pulsante e descrizione per Addestramento Modelli Dl4j
        Button modelTrainerButton = new Button("Training Modelli DL4J");
        Label modelTrainerDescription = new Label(
                "Addestra modelli di rete convolutiva DL4J per la preelaborazione di immagini con testo."
        );

        // Pulsante e descrizione per Estrazione Testo OCR
        Button processingButton = new Button("Estrazione Testo OCR\nProcessing");
        Label processingDescription = new Label(
                "Analizza immagini contenenti testo, utilizzando l'OCR (Optical Character Recognition) per estrarne il contenuto."
        );

        // Pulsante e descrizione per Training Modelli Tesseract
        Button tess4jButton = new Button("Training Modelli Tesseract");
        Label tess4jDescription = new Label(
                "Effettua il fine-tuning dei modelli LSTM di Tesseract per migliorare la trascrizione OCR."
        );

        // Pulsante e descrizione per Analisi Lessicale
        Button textCorrectionButton = new Button("Analisi Lessicale\nPost processing");
        Label textCorrectionDescription = new Label(
                "Analizza il testo trascritto per individuare e correggere errori lessicali."
        );

        // Stili base
        String blueBase = "-fx-font-size: 14px; -fx-min-width: 200px; -fx-min-height: 40px; "
                + "-fx-border-color: #B0B0B0; -fx-border-width: 1px; "
                + "-fx-background-radius: 5px; -fx-border-radius: 5px;";
        String blueButtonStyle = blueBase + " -fx-background-color: #D3D3D3;";  // grigio chiaro
        String blueButtonHoverStyle = blueBase + " -fx-background-color: #A9A9A9;";  // grigio più scuro

        String orangeBase = "-fx-font-size: 14px; -fx-min-width: 200px; -fx-min-height: 40px; "
                + "-fx-border-color: #9BC2CF; -fx-border-width: 1px; "
                + "-fx-background-radius: 5px; -fx-border-radius: 5px;";
        String orangeButtonStyle = orangeBase + " -fx-background-color: #ADD8E6;";  // azzurro chiaro
        String orangeButtonHoverStyle = orangeBase + " -fx-background-color: #9ACCE0;";  // azzurro hover


        // Assegna stili
        preprocessingButton.setStyle(orangeButtonStyle);
        processingButton.setStyle(orangeButtonStyle);
        textCorrectionButton.setStyle(orangeButtonStyle);

        modelTrainerButton.setStyle(blueButtonStyle);
        tess4jButton.setStyle(blueButtonStyle);

        // Effetti hover
        preprocessingButton.setOnMouseEntered(e -> preprocessingButton.setStyle(orangeButtonHoverStyle));
        preprocessingButton.setOnMouseExited(e -> preprocessingButton.setStyle(orangeButtonStyle));
        processingButton.setOnMouseEntered(e -> processingButton.setStyle(orangeButtonHoverStyle));
        processingButton.setOnMouseExited(e -> processingButton.setStyle(orangeButtonStyle));
        textCorrectionButton.setOnMouseEntered(e -> textCorrectionButton.setStyle(orangeButtonHoverStyle));
        textCorrectionButton.setOnMouseExited(e -> textCorrectionButton.setStyle(orangeButtonStyle));

        modelTrainerButton.setOnMouseEntered(e -> modelTrainerButton.setStyle(blueButtonHoverStyle));
        modelTrainerButton.setOnMouseExited(e -> modelTrainerButton.setStyle(blueButtonStyle));
        tess4jButton.setOnMouseEntered(e -> tess4jButton.setStyle(blueButtonHoverStyle));
        tess4jButton.setOnMouseExited(e -> tess4jButton.setStyle(blueButtonStyle));

        // Stile descrizioni
        String descriptionStyle = "-fx-font-size: 12px; -fx-wrap-text: true;";
        preprocessingDescription.setStyle(descriptionStyle);
        modelTrainerDescription.setStyle(descriptionStyle);
        processingDescription.setStyle(descriptionStyle);
        tess4jDescription.setStyle(descriptionStyle);
        textCorrectionDescription.setStyle(descriptionStyle);

        // Azioni pulsanti
        preprocessingButton.setOnAction(e -> new PreprocessingDl4jGui().start(new Stage()));
        modelTrainerButton.setOnAction(e -> new ModelTrainerDl4jGui().start(new Stage()));
        processingButton.setOnAction(e -> new ProcessingTess4jGui().start(new Stage()));
        tess4jButton.setOnAction(e -> new ModelTrainerTess4jGui().start(new Stage()));
        textCorrectionButton.setOnAction(e -> new TextCorrectionGui().start(new Stage()));

        // Layout
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(20);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

// Aggiunta elementi
        grid.add(title, 0, 0, 2, 1);
        grid.add(acronym, 0, 1, 2, 1);
        grid.add(description, 0, 2, 2, 1);
        grid.add(preprocessingButton, 0, 3);
        grid.add(preprocessingDescription, 1, 3);
        grid.add(modelTrainerButton, 0, 4);
        grid.add(modelTrainerDescription, 1, 4);
        grid.add(processingButton, 0, 5);
        grid.add(processingDescription, 1, 5);
        grid.add(tess4jButton, 0, 6);
        grid.add(tess4jDescription, 1, 6);
        grid.add(textCorrectionButton, 0, 7);
        grid.add(textCorrectionDescription, 1, 7);


        // Scena e finestra
        Scene scene = new Scene(grid, 600, 400);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
