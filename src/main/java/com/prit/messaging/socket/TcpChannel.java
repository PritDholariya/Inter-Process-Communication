package com.prit.messaging.socket;


import com.prit.messaging.Message;
import com.prit.messaging.MessageChannel;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;


public final class TcpChannel implements MessageChannel {

    private static final int CONNECT_ATTEMPTS = 50;
    private static final long CONNECT_RETRY_MILLIS = 100L;

    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;

    private TcpChannel(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.out = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
    }

    /** Listen on the given port and block until the peer connects. */
    public static TcpChannel accept(int port) throws IOException {
        try (ServerSocket server = new ServerSocket(port)) {
            Socket socket = server.accept();
            return new TcpChannel(socket);
        }
    }

    /**
     * Connect to a listening peer, retrying briefly so the two processes may be
     * started in any order without a race.
     */
    public static TcpChannel connect(String host, int port) throws IOException {
        IOException lastFailure = null;
        for (int attempt = 1; attempt <= CONNECT_ATTEMPTS; attempt++) {
            try {
                return new TcpChannel(new Socket(host, port));
            } catch (IOException e) {
                lastFailure = e;
                sleep(CONNECT_RETRY_MILLIS);
            }
        }
        throw lastFailure;
    }

    @Override
    public void send(Message message) {
        // Payload never contains a newline, so a line is exactly one message.
        out.println(message.text());
    }

    @Override
    public Message receive() {
        try {
            String line = in.readLine();
            return (line == null) ? null : new Message(line);
        } catch (IOException e) {
            // A broken connection is treated as a normal end-of-stream.
            return null;
        }
    }

    @Override
    public void close() {
        try {
            socket.close();
        } catch (IOException ignored) {
            // Closing is best-effort; nothing useful to do on failure.
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
