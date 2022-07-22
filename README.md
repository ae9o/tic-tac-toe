![logo](logo.gif)

[![Latest release](https://img.shields.io/github/v/release/ae9o/tic-tac-toe?display_name=tag)](https://github.com/ae9o/tic-tac-toe/releases)

# Advanced tic-tac-toe for custom fields

This implementation of tic-tac-toe allows to play against advanced AI on a field of custom size. The development of this
app was started as part of a test task. I continue to develop it and its code is distributed under the Apache License
2.0. Feel free to use it for any purpose.

In order to maintain playability on very large fields, a small change in gameplay was required. A player must collect
a combo of only 5 marks in a row on a field of size 6 or more (see the gif above for an example). It seems to me that
this is the only version of the game that is exciting. The limited size of the combo on a large field creates a wide
scope of possible strategies and makes the game tense. Awesome!

## Implementation details

### AI

AI is based on the MTD(f) algorithm described by Aske Plaat<sup>[\[1\]](#references)</sup>. This is an advanced implementation of the Minimax algorithm
and is even faster than the NegaScout algorithm. In a nutshell, its speed is achieved through the use of a
dynamically expanding search window with alpha-beta pruning, as well as through heavy use of memory to store
intermediate characteristics of visited nodes of the tree of possible moves.

For the MTD(f) algorithm to work, it is required to clearly identify each game state in order to navigate the state tree
and correctly use the cache with previously calculated results. In other words, it is required to implement a 
transposition table. This app uses a simple and efficient approach — Zobrist hashing<sup>[\[2\]](#references)</sup>.

### Code optimization

The core of the game is written in pure Java<sup>TM</sup> and can be easily ported from Android<sup>TM</sup> to any 
other platform.

The core is optimized to avoid autoboxing and re-creation of various objects as much as possible. HashMaps with 
primitive keys are used from the `fastutil` package. Everything is written in such an approach that the garbage
collector can go on vacation.

## Future modifications

The current version is single-threaded. This fact becomes noticeable when playing on very large fields. It is planned to
move AI to a separate thread and parallelize its calculations.

## References

Here you can find links to additional resources if you are interested in learning more about the algorithmic details 
behind the application.

1. [Aske Plaat: MTD(f), a new chess algorithm](https://people.csail.mit.edu/plaat/mtdf.html)
2. [Zobrist hashing](https://en.wikipedia.org/wiki/Zobrist_hashing)

