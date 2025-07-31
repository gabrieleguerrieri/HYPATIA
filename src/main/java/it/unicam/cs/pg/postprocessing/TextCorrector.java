package it.unicam.cs.pg.postprocessing;

import javafx.scene.control.Alert;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.ops.transforms.Transforms;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Classe per la correzione avanzata del testo utilizzando modelli Word2Vec.
 */
public class TextCorrector {
    private final WordVectors wordVectors;
    private final int MAX_EDIT_DISTANCE;

    /**
     * Costruttore della classe TextCorrector.
     *
     * @param wordVectors        modello Word2Vec utilizzato per la correzione del testo
     * @param MAX_EDIT_DISTANCE  massima distanza di modifica consentita tra parole
     */
    public TextCorrector(WordVectors wordVectors, int MAX_EDIT_DISTANCE) {
        this.wordVectors = wordVectors;
        this.MAX_EDIT_DISTANCE = MAX_EDIT_DISTANCE;
    }

    /**
     * Corregge il testo fornito utilizzando i metodi interni di correzione.
     *
     * @param text testo da correggere
     * @return testo corretto
     */
    public String advancedCorrectText(String text) {
        StringBuilder corrected = new StringBuilder();
        Pattern pattern = Pattern.compile("(\\p{L}+-?\\p{L}*)|([^\\p{L}\\s]+)|\\s+");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String token = matcher.group();
            if (token.matches("\\p{L}+-?\\p{L}*")) {
                corrected.append(processWordToken(token));
            } else {
                corrected.append(token);
            }
        }
        return corrected.toString();
    }

    /**
     * Elabora un singolo token di parola, cercando una parola corretta nel modello Word2Vec.
     *
     * @param word parola da elaborare
     * @return parola corretta o originale se non trovata una corrispondenza valida
     */
    private String processWordToken(String word) {
        String normalized = normalizeWord(word);
        if (wordVectors.hasWord(normalized)) {
            return preserveOriginalFormatting(word, normalized);
        }
        List<String> candidates = new ArrayList<>();
        for (Object vocabWordObj : wordVectors.vocab().words()) {
            String vocabWord = (String) vocabWordObj;
            int distance = editDistance(normalized, vocabWord);
            if (distance <= MAX_EDIT_DISTANCE) {
                candidates.add(vocabWord);
            }
        }
        // Ordina i candidati in base alla distanza di modifica
        candidates.sort(Comparator.comparingInt(vocabWord -> editDistance(normalized, vocabWord)));
        if (!candidates.isEmpty()) {
            String bestMatch = findBestSemanticMatch(normalized, candidates);
            if (bestMatch != null) {
                return preserveOriginalFormatting(word, bestMatch);
            }
        }
        List<String> nearest = wordsNearest(normalized);
        if (!nearest.isEmpty()) {
            return preserveOriginalFormatting(word, nearest.get(0));
        }
        return word; // Restituisci la parola originale se non è stato trovato un sostituto valido
    }

    /**
     * Preserva la formattazione originale della parola corretta.
     *
     * @param original   parola originale
     * @param correction parola corretta
     * @return parola corretta con la formattazione preservata
     */
    private String preserveOriginalFormatting(String original, String correction) {
        if (original.isEmpty()) return correction;
        // Gestione delle parole composte con trattino
        if (original.contains("-")) {
            String[] originalParts = original.split("-");
            String[] correctionParts = correction.split("-");
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < originalParts.length; i++) {
                String originalPart = originalParts[i];
                String correctedPart = (i < correctionParts.length) ? correctionParts[i] : originalPart;
                // Applica la giusta capitalizzazione alla parte corretta
                correctedPart = formatCorrectly(originalPart, correctedPart);
                if (i > 0) {
                    result.append("-");
                }
                result.append(correctedPart);
            }
            return result.toString();
        }
        // Applicazione della giusta capitalizzazione alla parola singola
        return formatCorrectly(original, correction);
    }

    /**
     * Applica la giusta capitalizzazione alla parola corretta in base all'originale.
     *
     * @param original   parola originale
     * @param correction parola corretta
     * @return parola corretta con la capitalizzazione corretta
     */
    private String formatCorrectly(String original, String correction) {
        if (original.equals(original.toUpperCase())) {
            return correction.toUpperCase();
        } else if (Character.isUpperCase(original.charAt(0))) {
            return correction.substring(0, 1).toUpperCase() + correction.substring(1).toLowerCase();
        }
        return correction.toLowerCase();
    }

    /**
     * Normalizza la parola rimuovendo caratteri non alfabetici e convertendola in minuscolo.
     *
     * @param word parola da normalizzare
     * @return parola normalizzata
     */
    private String normalizeWord(String word) {
        return word.replaceAll("[^\\p{L}]", "").toLowerCase();
    }

    /**
     * Trova la parola più simile semanticamente tra i candidati.
     *
     * @param target    parola target da confrontare
     * @param candidates elenco di parole candidate
     * @return parola più simile semanticamente
     */
    private String findBestSemanticMatch(String target, List<String> candidates) {
        INDArray targetVector = wordVectors.getWordVectorMatrix(target);
        // Debug: Verifica se il vettore per la parola target esiste
        if (targetVector == null) {
            System.out.println("Parola fuori vocabolario, nessun vettore trovato: " + target);
            return candidates.get(0); // Restituisce il primo candidato se il target non è nel modello
        } else {
            System.out.println("Vettore trovato per: " + target);
        }
        String bestMatch = candidates.get(0);
        double maxSimilarity = -1;
        for (String candidate : candidates) {
            INDArray candidateVector = wordVectors.getWordVectorMatrix(candidate);
            if (candidateVector == null) {
                continue; // Salta i candidati che non sono nel modello
            }
            double similarity = Transforms.cosineSim(targetVector, candidateVector);
            if (similarity > maxSimilarity) {
                maxSimilarity = similarity;
                bestMatch = candidate;
            }
        }
        return bestMatch;
    }

    /**
     * Calcola la distanza di modifica (edit distance) tra due stringhe.
     *
     * @param a prima stringa
     * @param b seconda stringa
     * @return distanza di modifica tra le due stringhe
     */
    private int editDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }
        return dp[a.length()][b.length()];
    }

    /**
     * Trova le parole più vicine semanticamente a una parola data.
     *
     * @param word parola di riferimento
     * @return lista di parole più vicine semanticamente
     */
    private List<String> wordsNearest(String word) {
        INDArray wordVector = wordVectors.getWordVectorMatrix(word);
        if (wordVector == null) {
            return Collections.emptyList();
        }
        List<Map.Entry<String, Double>> similarities = new ArrayList<>();
        for (Object vocabWordObj : wordVectors.vocab().words()) {
            String vocabWord = (String) vocabWordObj;
            INDArray vocabVector = wordVectors.getWordVectorMatrix(vocabWord);
            if (vocabVector != null) {
                double similarity = Transforms.cosineSim(wordVector, vocabVector);
                similarities.add(new AbstractMap.SimpleEntry<>(vocabWord, similarity));
            }
        }
        similarities.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));
        return similarities.stream()
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Gestisce gli errori mostrando un alert di errore.
     *
     * @param ex eccezione catturata
     * @param message messaggio di errore personalizzato
     */
    private void handleError(Exception ex, String message) {
        ex.printStackTrace();
        showAlert(message + ": " + ex.getMessage());
    }

    /**
     * Mostra un alert di diversi tipi (es. informazione, errore).
     *
     * @param message messaggio dell'alert
     */
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Errore");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}