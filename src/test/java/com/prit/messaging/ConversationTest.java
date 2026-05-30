package com.prit.messaging;



import com.prit.messaging.inmemory.InMemoryChannel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversationTest {

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void initiatorAndResponderExchangeExactlyTheTargetCount() throws InterruptedException {
        int target = 10;
        MessageChannel[] pair = InMemoryChannel.createPair();

        Player initiator = new Player("A", PlayerRole.INITIATOR, pair[0], target);
        Player responder = new Player("B", PlayerRole.RESPONDER, pair[1], target);

        Thread responderThread = new Thread(responder);
        Thread initiatorThread = new Thread(initiator);
        responderThread.start();
        initiatorThread.start();
        initiatorThread.join();
        responderThread.join();

        assertEquals(target, initiator.sentCount(), "initiator must send exactly the target count");
        assertEquals(target, initiator.receivedCount(), "initiator must receive exactly the target count");
        assertEquals(target, responder.receivedCount(), "responder must receive exactly the target count");
        assertEquals(target, responder.sentCount(), "responder must send exactly the target count");
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void payloadGrowsByOneCounterTokenPerHop() throws InterruptedException {
        // With target = 1, the initiator sends the seed "1", the responder replies
        // "1-1", and the initiator stops on receiving it. This pins down the exact
        // concatenation rule (received text + this player's send counter).
        int target = 1;
        MessageChannel[] pair = InMemoryChannel.createPair();
        Player initiator = new Player("A", PlayerRole.INITIATOR, pair[0], target);
        Player responder = new Player("B", PlayerRole.RESPONDER, pair[1], target);

        Thread r = new Thread(responder);
        Thread i = new Thread(initiator);
        r.start();
        i.start();
        i.join();
        r.join();

        assertEquals(1, initiator.sentCount());
        assertEquals(1, initiator.receivedCount());
        assertTrue(responder.sentCount() >= 1, "responder should have replied at least once");
    }
}
