package com.prit.messaging;

import java.util.Objects;

public final class Player implements Runnable {

    /** Separator used when concatenating the running counter onto the payload. */
    private static final String SEPARATOR = "-";

    private final String name;
    private final PlayerRole role;
    private final MessageChannel channel;
    private final int targetMessageCount;

    private int sentCount = 0;
    private int receivedCount = 0;

    /**
     * @param name               human-readable name, used only for logging
     * @param role               whether this player opens the conversation
     * @param channel            the transport to its single peer
     * @param targetMessageCount stop condition for the initiator: the number of
     *                           messages it must both send and receive (e.g. 10)
     */
    public Player(String name, PlayerRole role, MessageChannel channel, int targetMessageCount) {
        this.name = Objects.requireNonNull(name, "name");
        this.role = Objects.requireNonNull(role, "role");
        this.channel = Objects.requireNonNull(channel, "channel");
        if (targetMessageCount <= 0) {
            throw new IllegalArgumentException("targetMessageCount must be positive");
        }
        this.targetMessageCount = targetMessageCount;
    }

    @Override
    public void run() {
        try {
            if (role == PlayerRole.INITIATOR) {
                sendSeed();
            }
            converse();
        } finally {
            channel.close();
            log("finished (sent=" + sentCount + ", received=" + receivedCount + ")");
        }
    }

    /** The initiator opens the conversation with a message carrying just its counter. */
    private void sendSeed() {
        sentCount++;
        Message seed = new Message(String.valueOf(sentCount));
        log("seed -> \"" + seed.text() + "\"");
        channel.send(seed);
    }

    /** Reply to each received message until the channel closes or the stop condition is hit. */
    private void converse() {
        Message incoming;
        while ((incoming = channel.receive()) != null) {
            receivedCount++;
            log("recv <- \"" + incoming.text() + "\" (received=" + receivedCount + ")");

            if (role == PlayerRole.INITIATOR && receivedCount >= targetMessageCount) {
                // Stop condition reached: initiator has now sent and received the
                // target count. Deliberately do NOT reply; the channel close in
                // run()'s finally block signals the peer to finish gracefully.
                break;
            }

            sentCount++;
            Message reply = new Message(incoming.text() + SEPARATOR + sentCount);
            log("send -> \"" + reply.text() + "\"");
            channel.send(reply);
        }
    }

    public int sentCount() {
        return sentCount;
    }

    public int receivedCount() {
        return receivedCount;
    }

    public String name() {
        return name;
    }

    private void log(String what) {
        System.out.printf("[pid=%d][%-5s/%-9s] %s%n",
                ProcessHandle.current().pid(), name, role, what);
    }
}
