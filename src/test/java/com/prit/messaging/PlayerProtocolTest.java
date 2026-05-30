package com.prit.messaging;



import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerProtocolTest {

    @Test
    void responderConcatenatesReceivedTextWithItsOwnSendCounter() {
        RecordingChannel channel = new RecordingChannel().feed("1", "1-1-2");

        Player responder = new Player("B", PlayerRole.RESPONDER, channel, 10);
        responder.run();

        // recv "1"     -> send "1"     + "-" + 1  -> "1-1"
        // recv "1-1-2" -> send "1-1-2" + "-" + 2  -> "1-1-2-2"
        assertEquals(List.of(new Message("1-1"), new Message("1-1-2-2")), channel.sent());
        assertEquals(2, responder.sentCount());
        assertEquals(2, responder.receivedCount());
        assertTrue(channel.isClosed(), "player must close its channel when done");
    }

    @Test
    void initiatorSeedIsItsFirstSendCounter() {
        RecordingChannel channel = new RecordingChannel(); // no replies -> stop immediately

        Player initiator = new Player("A", PlayerRole.INITIATOR, channel, 10);
        initiator.run();

        assertEquals(1, initiator.sentCount());
        assertEquals(new Message("1"), channel.sent().get(0));
    }

    @Test
    void initiatorStopsAfterSendingAndReceivingTargetCount() {
        // Ten scripted replies drive ten receives; their text is irrelevant to the count.
        RecordingChannel channel = new RecordingChannel()
                .feed("r1", "r2", "r3", "r4", "r5", "r6", "r7", "r8", "r9", "r10");

        Player initiator = new Player("A", PlayerRole.INITIATOR, channel, 10);
        initiator.run();

        assertEquals(10, initiator.sentCount(), "seed + 9 replies = 10 sends");
        assertEquals(10, initiator.receivedCount());
        // Crucially, it must NOT reply to the 10th received message.
        assertEquals(10, channel.sent().size(), "no 11th message after the stop condition");
    }
}

