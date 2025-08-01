
# HYPATIA

HYPATIA (Hierarchical Yielding Processing for Automatic Text Interpretation and Analysis) è un'applicazione Java che utilizza tecniche di OCR (Optical Character Recognition) per l'elaborazione di documenti storici. Il progetto si basa su librerie avanzate come Tess4J, OpenCV, DeepLearning4j e JavaFX per fornire un'interfaccia grafica utente (GUI) intuitiva e funzionalità di elaborazione avanzate.

## Indice

1. [Descrizione del Progetto](#descrizione-del-progetto)
2. [Requisiti di Sistema](#requisiti-di-sistema)
3. [Installazione](#installazione)
    - [Clonazione del Repository](#clonazione-del-repository)
    - [Dipendenze Maven](#dipendenze-maven)
    - [Configurazione Manuale delle Librerie](#configurazione-manuale-delle-librerie)
4. [Esecuzione del Progetto](#esecuzione-del-progetto)
5. [Struttura del Progetto](#struttura-del-progetto)
6. [Librerie Esterne Utilizzate](#librerie-esterne-utilizzate)
7. [Contributi](#contributi)
8. [Licenza](#licenza)

## Descrizione del Progetto

Il progetto HYPATIA è stato sviluppato per facilitare l'estrazione di testo da documenti storici tramite tecniche di OCR. L'applicazione include:

- Un'interfaccia grafica utente (GUI) realizzata con JavaFX.
- Funzionalità di elaborazione delle immagini tramite OpenCV.
- Integrazione con Tess4J per il riconoscimento ottico dei caratteri.
- Modelli di apprendimento automatico basati su DeepLearning4j per migliorare l'accuratezza dell'OCR.

## Nuove funzionalità introdotte

- Aggiunte architetture DL4J dedicate a:
   - **Rimozione sfondo / Denoising** (100×100 RGB → 100×100 RGB)
   - **Super‑risoluzione** (100×100 RGB → 400×400 RGB)
- Nuova GUI di training per generare patch, addestrare i modelli e salvarli in ZIP.
- Aggiornata GUI di preprocessing per caricare modelli e salvare immagini elaborate in cartelle dedicate:
   - `background-remove`, `denoising`, `super-resolution`.
- Log, barra di progresso e campo per numero di epoche integrati nell’interfaccia.

**Motivo delle estensioni:** ottimizzare la qualità delle immagini storiche e migliorare la precisione dell’OCR, semplificando al contempo l’uso e la gestione dei modelli tramite interfacce dedicate.

## Requisiti di Sistema

Prima di iniziare, assicurati di soddisfare i seguenti requisiti:

- Java Development Kit (JDK): Versione 19 o superiore.
- Maven: Versione 3.8.1 o superiore.
- Sistema Operativo: Compatibile con le librerie native di OpenCV e ND4J (Windows, macOS, Linux).

## Installazione

### Clonazione del Repository

Per clonare il repository del progetto, esegui il seguente comando:

```
git clone https://github.com/grrgrl71e19d451b/HYPATIA.git
cd HYPATIA
```

### Dipendenze Maven

Le dipendenze del progetto sono gestite tramite Maven. Quando esegui il comando mvn install, Maven scaricherà automaticamente tutte le librerie necessarie dal repository centrale. Non è richiesta alcuna installazione manuale delle dipendenze.

Per installare le dipendenze, esegui:

```
mvn clean install
```

### Configurazione Manuale delle Librerie

La maggior parte delle librerie utilizzate nel progetto (ad esempio, Tess4J, OpenCV, DeepLearning4j) sono gestite automaticamente da Maven. Tuttavia, alcune librerie potrebbero richiedere configurazioni aggiuntive:

- **Tess4J**: Tess4J richiede l'installazione di Tesseract OCR sul sistema. Scarica e installa Tesseract dal sito ufficiale: Tesseract OCR Downloads. Dopo l'installazione, assicurati che il percorso di Tesseract sia aggiunto alla variabile di ambiente PATH.
- **OpenCV**: OpenCV viene gestito tramite la dipendenza Maven (org.bytedeco:opencv-platform). Non è richiesta alcuna installazione manuale, ma assicurati che il sistema supporti le librerie native di OpenCV.
- **ND4J**: ND4J utilizza librerie native per il calcolo efficiente. Maven scaricherà automaticamente le versioni appropriate per il tuo sistema operativo.

## Esecuzione del Progetto

Per eseguire il progetto, puoi utilizzare uno dei seguenti metodi:

### Esecuzione tramite IDE

1. Importa il progetto in un IDE Java (ad esempio, IntelliJ IDEA o Eclipse).
2. Configura il JDK corretto (versione 19 o superiore).
3. Esegui la classe principale specificata nel file pom.xml (gui.it.unicam.cs.pg.HypatiaLauncher).

### Esecuzione tramite JAR

L'applicazione utilizza JavaFX, che deve essere scaricato separatamente dal JDK (versione 11 e superiori). Dopo aver eseguito `mvn clean package`, esegui il JAR con il comando:

```
java --module-path C:\javafx-sdk-21.0.5\lib --add-modules javafx.controls,javafx.fxml -jar C:\HYPATIA\target\HYPATIA-1.0-SNAPSHOT-shaded.jar
```
Assicurati che i percorsi di JavaFX e del file JAR siano corretti in base alle tue installazioni personali.

## Struttura del Progetto

La struttura del progetto è organizzata come segue:

```
HYPATIA/
├── src/
│   ├── main/java/it/unicam/cs/pg/
│   │   ├── gui/
│   │   │   ├── ModelTrainerDl4jGui
│   │   │   ├── ModelTrainerTess4jGui
│   │   │   ├── HypatiaLauncher
│   │   │   ├── PreprocessingDl4jGui
│   │   │   ├── ProcessingTess4jGui
│   │   │   ├── TextCorrectionGui
│   │   │
│   │   ├── modelDl4jTraining/
│   │   │   ├── ImagePreprocessor
│   │   │   ├── RimozioneSfondoModelArchitecture
│   │   │   ├── RimozioneSfondoPatchTrainer
│   │   │   ├── SuperRisoluzioneModelArchitecture
│   │   │   ├── SuperRisoluzionePatchTrainer
│   │   │
│   │   ├── modelTess4JTraining/
│   │   │   ├── BoxCleaner
│   │   │   ├── BoxTextEditor
│   │   │   ├── ModelTrainingExecutor
│   │   │
│   │   ├── postprocessing/
│   │   │   ├── TextCorrector
│   │   │
│   │   ├── preprocessing/
│   │   │   ├── PatchGenerator
│   │   │   ├── PreprocessingFilters
│   │   │
│   │   ├── processing/
│   │   │   ├── ImageSelectionWindow
│   │   │   ├── ImageUtils
│   │   │
├── .gitignore
├── LICENSE
├── README.md
├── pom.xml

```

Nota: Ogni modulo dell'interfaccia grafica nella cartella gui/ ha una sottocartella con classi ausiliarie specifiche, alcune delle quali sono condivise tra più moduli per favorire modularità e riutilizzo del codice. Il progetto include esclusivamente il codice sorgente organizzato nelle cartelle principali come gui/, modelDl4jTraining/, modelTess4JTraining/, preprocessing/, processing/ e postprocessing/.
## Librerie Esterne Utilizzate

Di seguito sono elencate le principali librerie esterne utilizzate nel progetto:

| Libreria       | Versione     | Descrizione                                                   |
|----------------|--------------|---------------------------------------------------------------|
| Tess4J         | 5.0.0        | Wrapper Java per Tesseract OCR.                               |
| OpenCV         | 4.9.0-1.5.10 | Libreria per l'elaborazione delle immagini.                   |
| DeepLearning4j | 1.0.0-beta7  | Framework di deep learning per Java.                          |
| JavaFX         | 20           | Framework per la creazione di interfacce grafiche utente.     |
| Jackson        | 2.15.2       | Libreria per la serializzazione/deserializzazione JSON.       |
| ND4J           | 1.0.0-beta7  | Libreria per il calcolo numerico (backend di DeepLearning4j). |

## Contributi

Se desideri contribuire al progetto, segui questi passaggi:

1. Forka il repository.
2. Crea una nuova branch (git checkout -b feature/nome-feature).
3. Effettua le modifiche e invia una pull request.

## Licenza

Questo progetto è distribuito sotto la licenza Apache License 2.0.

Puoi utilizzarlo, modificarlo e distribuirlo liberamente nel rispetto dei termini indicati nel file LICENSE. Per maggiori dettagli, consulta il testo completo della licenza su:  
[https://www.apache.org/licenses/LICENSE-2.0](https://www.apache.org/licenses/LICENSE-2.0)
