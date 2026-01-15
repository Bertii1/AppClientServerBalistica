# Calcolatore Balistico TCP

Applicazione client-server TCP per il calcolo di traiettorie balistiche con resistenza dell'aria.

## Descrizione

Questo progetto implementa un sistema distribuito che permette di simulare il lancio di proiettili considerando:
- Gravità terrestre (g = 9.81 m/s²)
- Resistenza dell'aria (drag aerodinamico)
- Diversi parametri fisici del proiettile

Il server gestisce multiple connessioni contemporanee tramite un ThreadPool e fornisce risultati dettagliati con:
- Grafico ASCII della traiettoria nella console
- **Grafico interattivo JFreeChart** in una finestra separata

## Struttura del Progetto

```
ballistic-calculator/
├── src/
│   ├── server/
│   │   ├── BallisticServer.java     # Server principale
│   │   ├── ClientHandler.java       # Gestione singolo client
│   │   ├── BallisticCalculator.java # Motore di calcolo
│   │   └── TrajectoryFormatter.java # Formattazione output
│   └── client/
│       ├── Main.java                # Interfaccia utente
│       ├── BallisticClient.java     # Connessione al server
│       ├── ProjectileParams.java    # Parametri proiettile
│       └── TrajectoryChart.java     # Grafico JFreeChart
├── data/
│   └── users.txt                    # Credenziali utenti
├── pom.xml                          # Configurazione Maven
└── README.md
```

## Compilazione ed Esecuzione

### Con Maven (consigliato)

```bash
# Compila il progetto (scarica automaticamente JFreeChart)
mvn clean compile

# Avvia il server (in un terminale)
mvn exec:java -Dexec.mainClass="server.BallisticServer"

# Avvia il client (in un altro terminale)
mvn exec:java -Dexec.mainClass="client.Main"
```

### Senza Maven (solo console, senza grafico JFreeChart)

```bash
cd src

# Compila server
javac server/*.java

# Compila client
javac client/*.java

# Avvia server
java server.BallisticServer

# Avvia client (in un altro terminale)
java client.Main
```

### Output Server
```
[SERVER] Ballistic Server avviato su porta 5000
[SERVER] In attesa di connessioni...
```

## Esempi di Utilizzo

### Simulazione Interattiva
1. Avvia il server
2. Avvia il client
3. Inserisci credenziali (es. `filippo` / `test2024`)
4. Scegli "1. Nuova simulazione"
5. Inserisci i parametri richiesti

### Esempi Preimpostati
- **Cannone medievale**: v=100 m/s, θ=45°, massa=5 kg, Cd=0.47
- **Proiettile moderno**: v=800 m/s, θ=30°, massa=0.15 kg, Cd=0.295
- **Lancio parabolico**: v=20 m/s, θ=60°, massa=0.5 kg, Cd=0.47

## Grafico JFreeChart

Dopo ogni simulazione, il client apre automaticamente una finestra con il grafico della traiettoria:

- **Titolo**: mostra velocità iniziale e angolo di lancio
- **Sottotitolo**: gittata, altezza massima e tempo di volo
- **Grafico**: traiettoria con punti e linea su griglia
- **Interattività**: zoom, pan e tooltip sui punti

Il grafico ASCII nella console rimane disponibile per compatibilità con terminali senza supporto grafico.

## Protocollo di Comunicazione

### Autenticazione
```
Client → Server: AUTH username password
Server → Client: OK | ERROR messaggio
```

### Simulazione
```
Client → Server: SIMULATE velocity angle mass dragCoeff
Server → Client: BEGIN_RESULT
                 [risultati multilinea]
                 END_RESULT
```

### Altri Comandi
```
HELP  - Mostra guida comandi
QUIT  - Disconnessione
```

## Fisica della Simulazione

### Forza di Drag
La resistenza dell'aria è calcolata con la formula:

```
F_drag = 0.5 × ρ × Cd × A × v²
```

Dove:
- ρ = 1.225 kg/m³ (densità aria a livello del mare)
- Cd = coefficiente di drag (0.47 per sfera, 0.295 per proiettile aerodinamico)
- A = 0.01 m² (area frontale)
- v = velocità istantanea

### Integrazione Numerica
Il calcolo usa il **metodo di Eulero** con passo temporale dt = 0.01s:

```
aₓ = -(F_drag × vₓ / v) / m
aᵧ = -g - (F_drag × vᵧ / v) / m

vₓ(t+dt) = vₓ(t) + aₓ × dt
vᵧ(t+dt) = vᵧ(t) + aᵧ × dt

x(t+dt) = x(t) + vₓ × dt
y(t+dt) = y(t) + vᵧ × dt
```

### Gittata Teorica (senza drag)
Per confronto, la gittata teorica senza resistenza dell'aria è:

```
R = v₀² × sin(2θ) / g
```

La gittata reale sarà sempre minore a causa del drag.

## Utenti di Default

| Username | Password |
|----------|----------|
| admin | password123 |
| filippo | test2024 |
| studente | prova |

Per aggiungere utenti, modifica `data/users.txt` (formato: `username:password`).

## Requisiti Tecnici

- Java 17 o superiore
- Maven 3.6+ (per compilazione con JFreeChart)
- Porta 5000 disponibile

## Dipendenze

| Libreria | Versione | Descrizione |
|----------|----------|-------------|
| JFreeChart | 1.5.4 | Grafici interattivi |

Le dipendenze vengono scaricate automaticamente da Maven.

## Estensioni Future

- [ ] Salvataggio simulazioni in JSON
- [ ] Supporto vento laterale
- [ ] Storico simulazioni (comando HISTORY)
- [ ] Logging strutturato
- [ ] Supporto SSL/TLS
- [ ] Export grafico come immagine PNG

## Autore

Progetto didattico per lo studio delle applicazioni client-server TCP in Java.
