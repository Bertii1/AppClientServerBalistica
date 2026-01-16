package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Client per connettersi al server di calcolo balistico.
 */
public class BallisticClient {
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 5000;

    private String host;
    private int port;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean connected = false;
    private boolean authenticated = false;

    public BallisticClient() {
        this(DEFAULT_HOST, DEFAULT_PORT);
    }

    public BallisticClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Stabilisce la connessione al server.
     * @return true se connesso con successo
     */
    public boolean connect() {
        try {
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            connected = true;
            return true;
        } catch (IOException e) {
            System.err.println("Errore connessione: " + e.getMessage());
            return false;
        }
    }

    /**
     * Autentica l'utente con il server.
     * @return messaggio di risposta dal server
     */
    public String authenticate(String username, String password) {
        if (!connected) {
            return "ERROR Non connesso al server";
        }

        out.println("AUTH " + username + " " + password);

        try {
            String response = in.readLine();
            if (response != null && response.equals("OK")) {
                authenticated = true;
            }
            return response;
        } catch (IOException e) {
            return "ERROR " + e.getMessage();
        }
    }

    /**
     * Invia una richiesta di simulazione.
     * @return risultati della simulazione
     */
    public String sendSimulation(ProjectileParams params) {
        if (!connected || !authenticated) {
            return "ERROR Non autenticato";
        }

        String validation = params.validate();
        if (validation != null) {
            return "ERROR Parametri non validi: " + validation;
        }

        out.println(params.toProtocolString());

        try {
            return readMultilineResponse();
        } catch (IOException e) {
            return "ERROR " + e.getMessage();
        }
    }

    /**
     * Invia una richiesta di aiuto.
     */
    public String sendHelp() {
        if (!connected || !authenticated || out == null) {
            return "ERROR Non autenticato";
        }

        out.println("HELP");

        try {
            return readMultilineResponse();
        } catch (IOException e) {
            return "ERROR " + e.getMessage();
        }
    }

    /**
     * Legge una risposta multilinea dal server.
     */
    private String readMultilineResponse() throws IOException {
        StringBuilder response = new StringBuilder();
        String line;

        while ((line = in.readLine()) != null) {
            if (line.equals("BEGIN_RESULT")) {
                continue;
            }
            if (line.equals("END_RESULT")) {
                break;
            }
            response.append(line).append("\n");
        }

        return response.toString();
    }

    /**
     * Disconnette dal server.
     */
    public void disconnect() {
        if (connected) {
            try {
                out.println("QUIT");
                socket.close();
            } catch (IOException e) {
                // Ignora errori durante la chiusura
            }
            connected = false;
            authenticated = false;
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }
}
