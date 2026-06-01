# Design Notes

Before writing any code I took some time to understand exactly what the task
is asking for and what decisions I needed to make. This document captures that
thinking - what I understood, what I assumed where the brief was open, and why
I structured things the way I did.

---

## What the task is actually asking

Two players talk to each other. One starts the conversation, the other replies.
Every reply takes the received message and adds the sender's own message counter
to the end of it. The conversation runs until the initiator has sent and received
10 messages, then both players stop cleanly.

The tricky part is requirement 5 and 7 together: the same player logic has to
work both when both players are in one JVM and when each player is its own
separate process. That is the design challenge worth thinking about.

---

## Assumptions I made

The brief intentionally leaves some things open. Here is how I filled them in:

- "The message counter that this player sent" — I read this as the running count
  of messages this player has sent, appended after incrementing. So the first
  reply from the responder to the seed message "1" becomes "1-1", the next
  reply from the initiator becomes "1-1-2", and so on. The payload grows by
  one token each hop.

- The initiator's opening message is just its first send counter — "1". I could
  have used something like "Hello" but a number is consistent with the
  concatenation rule and keeps things simple.

- The separator between tokens is "-". Message text never contains a newline,
  which matters for the socket transport since it uses newline-delimited lines.

- The stop count of 10 is a named constant in one place, easy to change.

- Same-process mode: one thread per player. Separate-process mode: one player
  runs on the main thread of each process.

---

## The main design decision

The problem I kept coming back to was: how do I avoid writing two separate
versions of the player — one for queues, one for sockets?

The answer is to put an interface between the player and the transport. I called
it `MessageChannel`. It has three methods: send, receive, and close. The player
only ever talks to this interface. It has no idea what is behind it.

For same-process mode I plug in `InMemoryChannel` — two blocking queues
cross-wired so what one player writes the other reads. For separate-process
mode I plug in `TcpChannel` — a plain TCP socket sending newline-delimited
text. The player itself does not change at all between the two modes.

This is the Dependency Inversion principle from SOLID. The player depends on
an abstraction, not a concrete implementation.

---

## How graceful shutdown works

When the initiator reaches 10 sent and 10 received it does not send a special
"I am done" message. It just closes its channel. Closing the channel causes
the responder's receive() call to return null. The responder's loop checks for
null on every iteration, so it exits naturally and closes its own channel too.
Both players have a finally block that closes the channel no matter what, so
nothing leaks even if something goes wrong.

No System.exit, no thread interruption, no daemon threads. The process exits
because both player threads finish naturally and there is nothing left running.

---

## Things I considered and decided against

**Separate InitiatorPlayer and ResponderPlayer classes.** I thought about this.
The only difference between the two roles is that the initiator sends a seed
message first and owns the stop condition. That is about two if-statements worth
of difference. Creating an abstract base class plus two subclasses plus a
template method for that felt like more structure than the problem needs. A
PlayerRole enum is simpler and still makes the distinction explicit.

**ExecutorService instead of raw threads.** For exactly two threads,
new Thread() + join() is clear and straightforward. ExecutorService would work
but adds ceremony without adding anything useful here.

**A binary serialisation protocol for the socket.** Plain UTF-8 text lines are
enough. The message payload is just a growing string of numbers and dashes —
no need for anything more complex.

---

## Class responsibilities at a glance

| Class                | What it does                                                                 |
|----------------------|------------------------------------------------------------------------------|
| Message              | Keep the text of one message. Immutable.                                     |
| MessageChannel       | Interface - the only thing Player knows about its transport.                 |
| PlayerRole           | Enum: INITIATOR or RESPONDER.                                                |
| Player               | Runs the conversation loop. Knows nothing about queues, sockets or threads.  |
| InMemoryChannel      | MessageChannel backed by blocking queues. Used in same-process mode.         |
| TcpChannel           | MessageChannel backed by a TCP socket. Used in separate-process mode.        |
| SameProcessApp       | Wires two players to InMemoryChannels, runs each on a thread, joins both.    |
| SeparateProcessApp   | Runs one player per process. Reads role/host/port from command-line args.    |