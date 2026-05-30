package com.prit.messaging;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

final class RecordingChannel implements MessageChannel {

    private final Deque<Message> inbound = new ArrayDeque<>();
    private final List<Message> sent = new ArrayList<>();
    private boolean closed;

    RecordingChannel feed(String... texts) {
        for (String text : texts) {
            inbound.add(new Message(text));
        }
        return this;
    }

    List<Message> sent() {
        return sent;
    }

    boolean isClosed() {
        return closed;
    }

    @Override
    public void send(Message message) {
        sent.add(message);
    }

    @Override
    public Message receive() {
        return inbound.isEmpty() ? null : inbound.poll();
    }

    @Override
    public void close() {
        closed = true;
    }
}

