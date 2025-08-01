package quasylab.hypatia.modelTess4JTraining;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Editor per file .box che gestisce la separazione tra metadati e testo
 * Permette la modifica del testo conservando la formattazione originale dei metadati
 */
public class BoxTextEditor {

    private final List<String> metadataList = new ArrayList<>();
    private final List<String> textList = new ArrayList<>();
    private TextArea textArea;
    private final String boxFilePath;

    /**
     * Costruisce un nuovo editor per il file .box specificato.
     * Carica il contenuto del file separando metadati e testo.
     *
     * @param boxFilePath percorso del file .box da caricare
     */
    public BoxTextEditor(String boxFilePath) {
        this.boxFilePath = boxFilePath;
        try {
            loadBoxFile(boxFilePath);
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Errore", "Errore nel caricamento del file .box.");
        }
    }

    /**
     * Avvia l'interfaccia grafica dell'editor
     * @param primaryStage stage principale dell'applicazione
     * @param fontStyle stile del font da applicare all'area di testo
     */
    public void startEditor(Stage primaryStage, String fontStyle) {
        primaryStage.setTitle("Box Text Editor");

        BorderPane root = new BorderPane();
        HBox controlPanel = createControlPanel();
        setupTextArea(fontStyle);

        root.setTop(controlPanel);
        root.setCenter(textArea);

        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Crea e restituisce il pannello di controllo contenente i pulsanti dell'interfaccia.
     *
     * @return pannello orizzontale con i controlli dell'editor
     */
    private HBox createControlPanel() {
        HBox panel = new HBox(10);
        panel.setPadding(new Insets(10));
        Button saveButton = new Button("Salva Modifiche");
        saveButton.setOnAction(event -> saveBoxFile());
        panel.getChildren().add(saveButton);
        return panel;
    }

    /**
     * Configura l'area di testo con lo stile specificato
     * @param fontStyle stringa CSS per lo stile del font
     */
    private void setupTextArea(String fontStyle) {
        textArea = new TextArea();
        textArea.setStyle(fontStyle);
        textArea.setWrapText(true);
        populateTextArea();
    }

    /**
     * Salva le modifiche nel file .box originale
     */
    public void saveBoxFile() {
        if (boxFilePath == null) {
            showAlert(Alert.AlertType.ERROR, "Errore", "Nessun file .box caricato!");
            return;
        }
        try {
            updateTextListFromTextArea();
            saveBoxFile(boxFilePath);
            showAlert(Alert.AlertType.INFORMATION, "Successo", "File .box salvato con successo!");
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Errore", "Errore nel salvataggio del file .box.");
        }
    }

    /**
     * Carica e parserizza il file .box separando metadati e testo
     * @param filePath percorso del file .box da caricare
     */
    private void loadBoxFile(String filePath) throws IOException {
        metadataList.clear();
        textList.clear();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Separa i metadati dal testo usando il carattere #
                if (line.contains("#")) {
                    int hashIndex = line.indexOf("#");
                    metadataList.add(line.substring(0, hashIndex));
                    textList.add(line.substring(hashIndex + 1).trim());
                } else {
                    metadataList.add(line);
                    textList.add("");
                }
            }
        }
    }

    /**
     * Popola l'area di testo con i contenuti del file caricato
     */
    private void populateTextArea() {
        textArea.clear();
        for (String text : textList) {
            textArea.appendText(text + "\n");
        }
    }

    /**
     * Aggiorna la lista dei testi con i contenuti modificati dall'utente
     */
    private void updateTextListFromTextArea() {
        String[] lines = textArea.getText().split("\n");
        for (int i = 0; i < textList.size() && i < lines.length; i++) {
            textList.set(i, lines[i].trim());
        }
    }

    /**
     * Scrive i contenuti modificati nel file .box
     * @param filePath percorso del file di destinazione
     */
    private void saveBoxFile(String filePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (int i = 0; i < metadataList.size(); i++) {
                String outputLine = metadataList.get(i);
                // Aggiunge il testo solo se non vuoto
                if (!textList.get(i).isEmpty()) {
                    outputLine += "#" + textList.get(i);
                }
                writer.write(outputLine);
                writer.newLine();
            }
        }
    }

    /**
     * Mostra un dialog all'utente
     * @param alertType tipo di alert
     * @param title titolo della finestra
     * @param content messaggio da visualizzare
     */
    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}