package com.prit.messaging;

public record Message(String text) {

    public Message {
        if (text == null) {
            throw new IllegalArgumentException("Message text must not be null");
        }
    }
}

