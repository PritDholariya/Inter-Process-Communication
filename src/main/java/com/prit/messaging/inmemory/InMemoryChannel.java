package com.prit.messaging.inmemory;



import com.prit.messaging.Message;
import com.prit.messaging.MessageChannel;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public final class InMemoryChannel implements MessageChannel {

    /** Sentinel placed on a queue to unblock a waiting receiver when the channel closes. */
    private static final Message POISON = new Message("__POISON_PILL__");

    private final BlockingQueue<Message> inbound;
    private final BlockingQueue<Message> outbound;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private InMemoryChannel(BlockingQueue<Message> inbound, BlockingQueue<Message> outbound) {
        this.inbound = inbound;
        this.outbound = outbound;
    }

    /**
     * Create two cross-wired channels for a single conversation.
     *
     * @return an array of length 2; what {@code [0]} writes, {@code [1]} reads, and
     *         vice versa. Give one to each player.
     */
    public static InMemoryChannel[] createPair() {
        BlockingQueue<Message> a = new LinkedBlockingQueue<>();
        BlockingQueue<Message> b = new LinkedBlockingQueue<>();
        return new InMemoryChannel[] {
                new InMemoryChannel(a, b), // reads from a, writes to b
                new InMemoryChannel(b, a)  // reads from b, writes to a
        };
    }

    @Override
    public void send(Message message) {
        if (closed.get()) {
            return;
        }
        outbound.add(message);
    }

    @Override
    public Message receive() {
        try {
            Message message = inbound.take();
            return (message == POISON) ? null : message;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            // Unblock both ends: our own pending receive and the peer's pending receive.
            inbound.add(POISON);
            outbound.add(POISON);
        }
    }
}

