package com.prit.messaging.app;

import com.prit.messaging.MessageChannel;
import com.prit.messaging.Player;
import com.prit.messaging.PlayerRole;
import com.prit.messaging.inmemory.InMemoryChannel;

public final class SameProcessApp {

    private static final int TARGET_MESSAGE_COUNT = 10;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== SAME-PROCESS mode (single JVM, pid="
                + ProcessHandle.current().pid() + ") ===");

        MessageChannel[] pair = InMemoryChannel.createPair();

        Player initiator = new Player("Alice", PlayerRole.INITIATOR, pair[0], TARGET_MESSAGE_COUNT);
        Player responder = new Player("Bob", PlayerRole.RESPONDER, pair[1], TARGET_MESSAGE_COUNT);

        Thread initiatorThread = new Thread(initiator, "initiator-thread");
        Thread responderThread = new Thread(responder, "responder-thread");

        // Start the responder first so it is ready before the seed message arrives.
        responderThread.start();
        initiatorThread.start();

        initiatorThread.join();
        responderThread.join();

        System.out.println("=== DONE. initiator sent=" + initiator.sentCount()
                + ", received=" + initiator.receivedCount()
                + " | responder sent=" + responder.sentCount()
                + ", received=" + responder.receivedCount() + " ===");
    }
}

