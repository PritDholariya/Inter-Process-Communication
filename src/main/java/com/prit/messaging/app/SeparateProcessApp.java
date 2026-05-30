package com.prit.messaging.app;

import com.prit.messaging.MessageChannel;
import com.prit.messaging.Player;
import com.prit.messaging.PlayerRole;
import com.prit.messaging.socket.TcpChannel;

import java.io.IOException;

public final class SeparateProcessApp {

    private static final int TARGET_MESSAGE_COUNT = 10;

    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.err.println("Usage: SeparateProcessApp <initiator|responder> <host> <port>");
            System.exit(2);
        }

        PlayerRole role = PlayerRole.valueOf(args[0].toUpperCase());
        String host = args[1];
        int port = Integer.parseInt(args[2]);

        System.out.println("=== SEPARATE-PROCESS mode, role=" + role
                + " (pid=" + ProcessHandle.current().pid() + ") ===");

        // The responder listens; the initiator connects. Both then use the identical
        // Player and MessageChannel contract as the same-process mode.
        MessageChannel channel = (role == PlayerRole.RESPONDER)
                ? TcpChannel.accept(port)
                : TcpChannel.connect(host, port);

        String name = (role == PlayerRole.INITIATOR) ? "Alice" : "Bob";
        Player player = new Player(name, role, channel, TARGET_MESSAGE_COUNT);
        player.run();

        System.out.println("=== Process pid=" + ProcessHandle.current().pid()
                + " done. sent=" + player.sentCount()
                + ", received=" + player.receivedCount() + " ===");
    }
}

