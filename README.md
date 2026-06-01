# Player Messaging

A small Java project where two players exchange messages with each other.
One player starts the conversation, they keep replying back and forth,
and the whole thing stops cleanly after 10 messages each way.

The interesting part is that the same player logic works in two completely
different setups — both players in one JVM, or each player in its own
separate process — without changing a single line of the Player class.

---

## Project structure

```
src/main/java/com/prit/messaging/
├── Message.java                  # the message object
├── MessageChannel.java           # interface — the key abstraction
├── Player.java                   # conversation logic lives here
├── PlayerRole.java               # INITIATOR or RESPONDER
├── inmemory/
│   └── InMemoryChannel.java      # same-process transport (blocking queues)
├── socket/
│   └── TcpChannel.java           # separate-process transport (TCP)
└── app/
    ├── SameProcessApp.java       # runs both players in one JVM
    └── SeparateProcessApp.java   # runs one player per JVM
```

---

## How to build

Requires JDK 17+ and Maven 3.8+.

```bash
mvn clean compile
```

To run the tests:

```bash
mvn test
```

---

## How to run

### Same process (both players, one JVM)

```bash
java -cp target/classes com.prit.messaging.app.SameProcessApp
```

On macOS/Linux you can also use the script:
```bash
./run-same-process.sh
```

On Windows (Git Bash):
```bash
bash run-same-process.sh
```

### Separate processes (each player in its own JVM)

Open two terminals. In the first one start the responder:

```bash
java -cp target/classes com.prit.messaging.app.SeparateProcessApp responder 127.0.0.1 5050
```

In the second terminal start the initiator:

```bash
java -cp target/classes com.prit.messaging.app.SeparateProcessApp initiator 127.0.0.1 5050
```

Or via script on macOS/Linux:
```bash
./run-separate-processes.sh
```

On Windows (Git Bash):
```bash
bash run-separate-processes.sh
```

---

## What the output looks like

Same-process mode — notice every line has the same PID:

```
=== SAME-PROCESS mode (single JVM, pid=18376) ===
[pid=18376][Alice/INITIATOR] seed -> "1"
[pid=18376][Bob  /RESPONDER] recv <- "1" (received=1)
[pid=18376][Bob  /RESPONDER] send -> "1-1"
[pid=18376][Alice/INITIATOR] recv <- "1-1" (received=1)
[pid=18376][Alice/INITIATOR] send -> "1-1-2"
...
[pid=18376][Bob  /RESPONDER] finished (sent=10, received=10)
[pid=18376][Alice/INITIATOR] finished (sent=10, received=10)
=== DONE. initiator sent=10, received=10 | responder sent=10, received=10 ===
```

Separate-process mode — two different PIDs, proving two separate JVMs:

```
=== SEPARATE-PROCESS mode, role=RESPONDER (pid=16328) ===
=== SEPARATE-PROCESS mode, role=INITIATOR (pid=16329) ===
...
[pid=16328][Bob  /RESPONDER] finished (sent=10, received=10)
[pid=16329][Alice/INITIATOR] finished (sent=10, received=10)
```

---

## Design decisions

The core idea is that `Player` only depends on a `MessageChannel` interface.
It has no idea whether it is talking through a queue or a socket. This means
the same Player class handles both the single-process and multi-process case
with no changes — you just plug in a different channel implementation.


A few other decisions worth mentioning since the brief left them open:

- The seed message is just "1" — the initiator's first send counter.
  Each reply appends the sender's own counter, so the payload grows one
  token per hop: 1 → 1-1 → 1-1-2 → 1-1-2-2 and so on.
- Shutdown is driven by the channel closing, not a special message.
  When the initiator hits 10/10 it closes its channel, which makes the
  responder's receive() return null and exit the loop naturally.
- One thread per player in same-process mode. Straightforward for a
  strictly turn-based two-party conversation.
- TCP with plain text lines for separate-process mode. Simplest
  cross-process option in pure Java, no serialisation needed.

---

## Notes

- Production code is pure JDK — no frameworks, no Spring.
- JUnit 5 is used for tests (test scope only, not part of the deliverable).
- Submitted as source only, no compiled jars included.