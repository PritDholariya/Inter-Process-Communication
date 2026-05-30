package com.prit.messaging;

public interface MessageChannel extends AutoCloseable {

    /**
     * Send one message to the peer.
     *
     * @param message the message to deliver (never {@code null})
     */
    void send(Message message);

    /**
     * Block until a message arrives from the peer.
     *
     * @return the received message, or {@code null} once the channel has been
     *         closed and no further messages can ever arrive. This {@code null}
     *         is the agreed end-of-stream signal and is what drives graceful
     *         shutdown on both sides of the conversation.
     */
    Message receive();

    /**
     * Release any underlying resources and unblock a pending {@link #receive()}
     * by causing it to return {@code null}. Implementations must be idempotent.
     */
    @Override
    void close();
    
}
