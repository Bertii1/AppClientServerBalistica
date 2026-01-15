package server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Server principale per il calcolo delle traiettorie balistiche.
 * Gestisce connessioni multiple tramite ThreadPool.
 */
public class BallisticServer {
    private static final int PORT = 5000;
    private static final int MAX_THREADS = 10;
    private static final String USERS_FILE = "data/users.txt";

    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private Map<String, String> users;
    private volatile boolean running = true;

    public BallisticServer() {
        this.threadPool = Executors.newFixedThreadPool(MAX_THREADS);
        this.users = new ConcurrentHashMap<>();
    }

    /**
     * Carica le credenziali utente dal file.
     */
    private void loadUsers() {
        try (BufferedReader reader = new BufferedReader(new FileReader(USERS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && line.contains(":")) {
                    String[] parts = line.split(":", 2);
                    if (parts.length == 2) {
                        users.put(parts[0].trim(), parts[1].trim());
                        log("Utente caricato: " + parts[0]);
                    }
                }
            }
            log("Caricati " + users.size() + " utenti dal file");
        } catch (FileNotFoundException e) {
            log("ATTENZIONE: File utenti non trovato: " + USERS_FILE);
            // Crea utenti di default
            users.put("admin", "password123");
            users.put("filippo", "test2024");
            log("Creati utenti di default");
        } catch (IOException e) {
            log("Errore lettura file utenti: " + e.getMessage());
        }
    }

    /**
     * Verifica le credenziali di un utente.
     */
    public boolean authenticate(String username, String password) {
        String storedPassword = users.get(username);
        return storedPassword != null && storedPassword.equals(password);
    }

    /**
     * Avvia il server.
     */
    public void start() {
        loadUsers();

        try {
            serverSocket = new ServerSocket(PORT);
            log("Ballistic Server avviato su porta " + PORT);
            log("In attesa di connessioni...");

            // Shutdown hook per chiusura graceful
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    log("Client connesso: " + clientSocket.getInetAddress() + ":" + clientSocket.getPort());

                    ClientHandler handler = new ClientHandler(clientSocket, this);
                    threadPool.execute(handler);

                } catch (SocketException e) {
                    if (running) {
                        log("Errore socket: " + e.getMessage());
                    }
                }
            }

        } catch (IOException e) {
            log("Errore avvio server: " + e.getMessage());
        }
    }

    /**
     * Chiude il server in modo graceful.
     */
    public void shutdown() {
        log("Shutdown in corso...");
        running = false;

        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
        }

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            log("Errore chiusura server socket: " + e.getMessage());
        }

        log("Server terminato.");
    }

    /**
     * Log con timestamp.
     */
    public static void log(String message) {
        System.out.println("[SERVER] " + message);
    }

    public static void main(String[] args) {
        BallisticServer server = new BallisticServer();
        server.start();
    }
}
