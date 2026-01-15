package server;

import java.io.*;
import java.net.*;

/**
 * Gestisce la connessione con un singolo client.
 * Implementa il protocollo di autenticazione e simulazione.
 */
public class ClientHandler extends Thread {
    private static final int MAX_AUTH_ATTEMPTS = 3;

    private final Socket socket;
    private final BallisticServer server;
    private BufferedReader in;
    private PrintWriter out;
    private String username;
    private boolean authenticated = false;

    public ClientHandler(Socket socket, BallisticServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Fase 1: Autenticazione
            if (!handleAuthentication()) {
                return;
            }

            // Fase 2: Gestione comandi
            handleCommands();

        } catch (IOException e) {
            log("Errore I/O: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }

    /**
     * Gestisce il processo di autenticazione.
     * Permette massimo 3 tentativi.
     */
    private boolean handleAuthentication() throws IOException {
        int attempts = 0;

        while (attempts < MAX_AUTH_ATTEMPTS) {
            String line = in.readLine();
            if (line == null) {
                log("Client disconnesso durante autenticazione");
                return false;
            }

            line = line.trim();
            if (line.startsWith("AUTH ")) {
                String[] parts = line.substring(5).split(" ", 2);

                if (parts.length == 2) {
                    String user = parts[0].trim();
                    String pass = parts[1].trim();

                    if (server.authenticate(user, pass)) {
                        this.username = user;
                        this.authenticated = true;
                        out.println("OK");
                        log("Utente '" + username + "' autenticato con successo");
                        return true;
                    }
                }

                attempts++;
                int remaining = MAX_AUTH_ATTEMPTS - attempts;
                if (remaining > 0) {
                    out.println("ERROR Invalid credentials. Tentativi rimasti: " + remaining);
                    log("Autenticazione fallita. Tentativi rimasti: " + remaining);
                } else {
                    out.println("ERROR Troppi tentativi falliti. Connessione chiusa.");
                    log("Troppi tentativi di autenticazione falliti");
                }
            } else {
                out.println("ERROR Comando non valido. Usa: AUTH username password");
            }
        }

        return false;
    }

    /**
     * Gestisce i comandi dopo l'autenticazione.
     */
    private void handleCommands() throws IOException {
        String line;

        while ((line = in.readLine()) != null) {
            line = line.trim();

            if (line.isEmpty()) {
                continue;
            }

            if (line.equalsIgnoreCase("QUIT") || line.equalsIgnoreCase("EXIT")) {
                out.println("BYE");
                log("Client '" + username + "' disconnesso");
                break;
            }

            if (line.startsWith("SIMULATE ")) {
                handleSimulation(line.substring(9).trim());
            } else if (line.equalsIgnoreCase("HELP")) {
                sendHelp();
            } else {
                out.println("ERROR Comando sconosciuto. Usa HELP per la lista comandi.");
            }
        }
    }

    /**
     * Gestisce una richiesta di simulazione.
     */
    private void handleSimulation(String params) {
        String[] parts = params.split("\\s+");

        if (parts.length != 4) {
            out.println("ERROR Formato: SIMULATE velocity angle mass dragCoeff");
            return;
        }

        try {
            double velocity = Double.parseDouble(parts[0]);
            double angle = Double.parseDouble(parts[1]);
            double mass = Double.parseDouble(parts[2]);
            double dragCoeff = Double.parseDouble(parts[3]);

            // Validazione parametri
            StringBuilder errors = new StringBuilder();
            if (velocity <= 0) errors.append("velocity deve essere > 0; ");
            if (angle < 0 || angle > 90) errors.append("angle deve essere tra 0 e 90; ");
            if (mass <= 0) errors.append("mass deve essere > 0; ");
            if (dragCoeff <= 0) errors.append("dragCoeff deve essere > 0; ");

            if (errors.length() > 0) {
                out.println("ERROR Parametri invalidi: " + errors.toString());
                return;
            }

            log("Simulazione richiesta da '" + username + "': v=" + velocity +
                ", angle=" + angle + ", mass=" + mass + ", cd=" + dragCoeff);

            // Esegui simulazione
            String result = BallisticCalculator.simulate(velocity, angle, mass, dragCoeff);

            // Invia risultato (multilinea con terminatore)
            out.println("BEGIN_RESULT");
            out.println(result);
            out.println("END_RESULT");

            log("Risultati inviati al client '" + username + "'");

        } catch (NumberFormatException e) {
            out.println("ERROR Parametri devono essere numeri validi");
        }
    }

    /**
     * Invia la guida dei comandi.
     */
    private void sendHelp() {
        out.println("BEGIN_RESULT");
        out.println("=== COMANDI DISPONIBILI ===");
        out.println("SIMULATE velocity angle mass dragCoeff");
        out.println("  - velocity: velocità iniziale in m/s (> 0)");
        out.println("  - angle: angolo di lancio in gradi (0-90)");
        out.println("  - mass: massa del proiettile in kg (> 0)");
        out.println("  - dragCoeff: coefficiente di drag (> 0, tipico 0.47 per sfere)");
        out.println("");
        out.println("HELP  - Mostra questo messaggio");
        out.println("QUIT  - Disconnetti dal server");
        out.println("END_RESULT");
    }

    /**
     * Chiude la connessione.
     */
    private void closeConnection() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            log("Errore chiusura socket: " + e.getMessage());
        }
    }

    private void log(String message) {
        BallisticServer.log(message);
    }
}
