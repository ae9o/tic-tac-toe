/*
 * Copyright (C) 2022 Alexei Evdokimenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ae9o.tictactoe.core;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;

import ae9o.tictactoe.core.TicTacToeGame.Mark;

/**
 * Implements the opponent's AI.
 *
 * <p>This implementation tries to find such a move for a given game state, which will lead to a victory with
 * the highest probability in the minimum number of moves.
 *
 * <p>The AI is based on the MTD(f) algorithm, which is a modified version of the Minimax algorithm with a dynamically
 * expanding search window with alpha-beta pruning. Also, this algorithm actively uses memory for caching intermediate
 * results in the process of dynamically increasing the depth of analysis of moves.
 *
 * <p>The MTD(f) algorithm assumes a sequential increase in the search depth, starting from depth 1. With each step
 * of increasing the depth, the accuracy of the analysis of moves increases, but the complexity grows exponentially.
 * Caching of intermediate results and implementation of the transposition table in the core of the game can
 * significantly speed up the algorithm. But in the end, the algorithm is still limited in execution time. When the
 * limited analysis time has elapsed, this AI returns the move that best matches the given heuristic at the achieved
 * search depth.
 *
 * <p>While actively using memory for caching results, this implementation tries to avoid any garbage collection and
 * the resulting friezes as much as possible. Therefore, maps with primitive keys from the {@code fastutil} package
 * are used, and nodes of the cache are pooled.
 *
 * <p>The quality of the final result of this algorithm on large fields depends entirely on the quality
 * of the heuristic, which is calculated when the maximum search depth is reached. The version of the heuristic
 * selected in this implementation gives a good result. But there is no limit to perfection. If you can find a better
 * option please let me know and share with the community.
 *
 * @see <a href="https://people.csail.mit.edu/plaat/mtdf.html">MTD(f)</a>
 */
public class MtdfTicTacToeAi implements TicTacToeAi {
    /** Heuristic points that the player receives for [0, 1, 2, ...] marks in a row,
     * between which there are no opponent's marks, but there are empty cells.
     * This reward will make the AI collect the marks in a row. */
    private static final int[] HIT_POINTS = {0, 10, 100, 1000, 10000, 100000, 1000000};
    /** Heuristic points that the player receives for empty cells in a row.
     * This reward will cause the AI to preemptively fill in the empty cells between
     * the player's marks, which can then turn into winning combos.*/
    private static final int EMPTY_POINTS = 10;
    /** Heuristic points that the player receives for placing his marks in a row without empty cells between.
     * This reward is given starting from the minimum sequence length and allows the AI to track when a fork
     * is ready to appear or when an opponent is close to winning. */
    private static final int SEQUENCE_POINTS = 100000000;
    private static final int MIN_REWARDED_SEQUENCE = 3;
    /** The limit on the algorithm running time in nanoseconds (default is 1 second). */
    private static final long MAX_SEARCH_TIME = 1000000000L;

    /** The core of the game. */
    private TicTacToeGame game;

    /** The pool with nodes of the cache. */
    private final Long2ObjectMap<Node> nodes = new Long2ObjectOpenHashMap<>();
    private NodePool nodePool;
    private SoftReference<NodePool> nodePoolRef = new SoftReference<>(new NodePool());

    /** The result of the algorithm. */
    private final Cell guess = new Cell();

    /** Maximum search depth at the current iteration.
     * When the specified depth is reached, the algorithm uses heuristics and does not go deeper. */
    private int maxDepth;

    /** This flag is set when the algorithm reaches the maximum depth of analysis in at least one of
     * the branches of the state tree.
     *
     * If it turns out that the maximum depth has not been reached during the iteration,
     * then the game state is primitive (very small field) or alpha-beta pruning is effectively performed.
     * In both cases, it is possible to interrupt the execution of the algorithm.*/
    private boolean maxDepthTouched;

    /** Auxiliary stuff for calculating heuristics. */
    private final HeuristicPointAccumulator pa1 = new HeuristicPointAccumulator();
    private final HeuristicPointAccumulator pa2 = new HeuristicPointAccumulator();
    private int comboSize;
    private Mark maxPlayerMark;
    private Mark minPlayerMark;
    private Mark heuristicTargetMark;

    /**
     * Returns such a move for the current game state, which will lead to a victory with
     * the highest probability in the minimum number of moves.
     *
     * <p>The execution time of this method (a sequential increase in the search depth)
     * is limited by the value of {@code MAX_SEARCH_TIME}.
     *
     * <p>If the applied optimizations allow the algorithm to get a result during the next
     * iteration without reaching the maximum depth, the execution will be completed before
     * the {@code MAX_SEARCH_TIME} limit is reached.
     *
     * @return The coordinates of the cell in which the player should place a mark.
     */
    public Cell guessNextMove(TicTacToeGame game) {
        this.game = game;
        comboSize = game.getComboSize();

        setupPool();
        final long startTime = System.nanoTime();
        int firstGuess = 0;
        maxDepth = 1;
        do {
            maxDepthTouched = false;
            firstGuess = mtdf(firstGuess);
            ++maxDepth;
        } while (maxDepthTouched && !Thread.interrupted() && (System.nanoTime() - startTime < MAX_SEARCH_TIME));
        disposePool();

        return guess;
    }

    /**
     * Sets up the pool of nodes that will be used in the MTD(f) cache generation process.
     *
     * <p>Access to the pool is organized through the {@link SoftReference} so that the rest
     * of the app can more freely manage memory when the AI is idle.
     */
    private void setupPool() {
        nodePool = nodePoolRef.get();
        if (nodePool == null) {
            nodePool = new NodePool();
            nodePoolRef = new SoftReference<>(nodePool);
        }
    }

    /**
     * Prepares the pool of nodes used in the MTD(f) cache generation process for potential
     * garbage collection while the AI is idle.
     */
    private void disposePool() {
        nodes.clear();
        nodePool.freeAll();
        nodePool = null;
    }

    /**
     * Core logic of MTD(f). The search window expands here for later alpha-beta pruning during Minimax search.
     *
     * @param firstGuess A point that specifies the start position of the search window.
     * @return An improved start position of the search window for the next iteration.
     */
    private int mtdf(final int firstGuess) {
        int g = firstGuess;
        int upperBound = Integer.MAX_VALUE;
        int lowerBound = Integer.MIN_VALUE;
        do {
            final int beta = (g == lowerBound) ? (g + 1) : g;
            g = rootMinimax(beta - 1, beta);
            if (g < beta) {
                upperBound = g;
            } else {
                lowerBound = g;
            }
        } while (lowerBound < upperBound);
        return g;
    }

    /**
     * Entry point to Minimax search. The classical algorithm is optimized with alpha-beta pruning,
     * as well as with active caching of intermediate results (in fact, caching is used in the {@code nestedMinimax}).
     *
     * @param alpha An alpha border for pruning.
     * @param beta A beta border for pruning.
     * @return An improved start position of the search window for the next MTD(f) iteration.
     */
    private int rootMinimax(final int alpha, final int beta) {
        // Store to compute heuristics later.
        maxPlayerMark = game.getCurrentTurn();
        game.switchTurn();
        minPlayerMark = game.getCurrentTurn();

        int g = Integer.MIN_VALUE;
        int a = alpha;
        maximize:
        for (int row = 0, size = game.getFieldSize(); row < size; ++row) {
            for (int col = 0; col < size; ++col) {
                if (game.getMark(row, col) == Mark.EMPTY) {
                    game.setMarkInternal(row, col, maxPlayerMark);
                    final int tmp = nestedMinimax(true, row, col, 0, a, beta);
                    game.setMarkInternal(row, col, Mark.EMPTY);

                    // Save the move with the maximum score, in order to
                    // return it as the result of the work of the AI.
                    if (tmp > g) {
                        g = tmp;
                        guess.setRow(row);
                        guess.setCol(col);
                    }

                    a = Math.max(a, g);
                    if (g >= beta) {
                        break maximize;
                    }
                }
            }
        }

        game.switchTurn();
        return g;
    }

    /**
     * Nested Minimax search. Used at all levels of the game state tree, except for the root. The classical
     * algorithm is optimized with alpha-beta pruning, as well as with active caching of intermediate results.
     *
     * @param minimize Need to minimize or maximize the score?
     * @param prevRow The row used by the previous player.
     * @param prevCol The col used by the previous player.
     * @param depth The current search depth.
     * @param alpha An alpha border for pruning.
     * @param beta A beta border for pruning.
     * @return The score for the selected player.
     */
    private int nestedMinimax(final boolean minimize, final int prevRow, final int prevCol, final int depth, int alpha,
                              int beta) {
        // Get cached results for the current game state.
        Node node = nodes.get(game.getTranspositionHash());
        if (node != null) {
            if (node.lowerBound >= beta) {
                return node.lowerBound;
            }
            if (node.upperBound <= alpha) {
                return node.upperBound;
            }
            alpha = Math.max(alpha, node.lowerBound);
            beta = Math.min(beta, node.upperBound);
        }

        // Check terminal states.
        if (game.findCombo(prevRow, prevCol)) {
            if (minimize) {
                return Integer.MAX_VALUE - depth;
            } else {
                return Integer.MIN_VALUE + depth;
            }
        }
        if (game.isFinalTurn()) {
            return 0;
        }

        // Having reached the maximum search depth for the current iteration,
        // use heuristics to calculate the player's score.
        if (depth == maxDepth) {
            maxDepthTouched = true;
            return evaluateHeuristic(maxPlayerMark) - evaluateHeuristic(minPlayerMark);
        }

        // If the depth is insufficient, use the Minimax.
        game.switchTurn();

        final int d = depth + 1;
        int g;
        if (minimize) {
            g = Integer.MAX_VALUE;
            int b = beta;
            minimize:
            for (int row = 0, size = game.getFieldSize(); row < size; ++row) {
                for (int col = 0; col < size; ++col) {
                    if (game.getMark(row, col) == Mark.EMPTY) {
                        game.setMarkInternal(row, col, minPlayerMark);
                        g = Math.min(g, nestedMinimax(false, row, col, d, alpha, b));
                        b = Math.min(b, g);
                        game.setMarkInternal(row, col, Mark.EMPTY);
                        if (g <= alpha) {
                            break minimize;
                        }
                    }
                }
            }
        } else {
            g = Integer.MIN_VALUE;
            int a = alpha;
            maximize:
            for (int row = 0, size = game.getFieldSize(); row < size; ++row) {
                for (int col = 0; col < size; ++col) {
                    if (game.getMark(row, col) == Mark.EMPTY) {
                        game.setMarkInternal(row, col, maxPlayerMark);
                        g = Math.max(g, nestedMinimax(true, row, col, d, a, beta));
                        a = Math.max(a, g);
                        game.setMarkInternal(row, col, Mark.EMPTY);
                        if (g >= beta) {
                            break maximize;
                        }
                    }
                }
            }
        }

        game.switchTurn();

        // Cache new results for the current game state.
        if (node == null) {
            node = nodePool.obtain();
            nodes.put(game.getTranspositionHash(), node);
        }
        if (g <= alpha) {
            node.upperBound = g;
        } else if (g < beta) {
            node.lowerBound = g;
            node.upperBound = g;
        } else {
            node.lowerBound = g;
        }

        return g;
    }

    /**
     * Evaluates heuristically the score of the given player for the current game state. Used in deep
     * layers of the game state tree where deeper search is not possible.
     *
     * <p>To evaluate points, sequential scanning of rows, columns and diagonals is performed.
     * The player receives a certain number of points for:
     * <ol>
     * <li>Placing his marks in a row (a sequence of own marks containing empty cells is also awarded).</li>
     * <li>Lack of opponent's marks between own marks.</li>
     * <li>Long continuous sequences of own marks are awarded additionally.</li>
     * </ol>
     *
     * @param target The mark of the player being evaluated.
     * @return A heuristic score.
     */
    private int evaluateHeuristic(Mark target) {
        // Target for HeuristicPointAccumulator operation (optimization to not pass through params).
        heuristicTargetMark = target;

        pa1.reset();
        pa2.reset();

        // Scan the part of the field located above the given diagonals.
        for (int i = 0, size = game.getFieldSize(); i < size; ++i) {
            for (int j = 0, n = size - 1, m = n - i; j <= i; ++j) {
                // Scan lines above back-slash diagonal (\).
                // (0,9)
                // (0,8) -> (1,9)
                // (0,7) -> (1,8) -> (2,9)
                pa1.addMark(game.getMark(j, m + j));

                // Scan lines above slash diagonal (/).
                // (0,0)
                // (0,1) -> (1,0)
                // (0,2) -> (1,1) -> (2,0)
                pa2.addMark(game.getMark(j, i - j));

            }
            pa1.startNextLine();
            pa2.startNextLine();
        }

        // Scan the part of the field located below the given diagonal.
        for (int i = 0, n = game.getFieldSize() - 1; i < n; ++i) {
            for (int j = 0, m = n - i; j <= i; ++j) {
                // Scan lines below back-slash diagonal (\).
                // (9,0)
                // (8,0) -> (9,1)
                // (7,0) -> (8,1) -> (9,2)
                pa1.addMark(game.getMark(m + j, j));

                // Scan lines below slash diagonal (/).
                // (9,9)
                // (8,9) -> (9,8)
                // (7,9) -> (8,8) -> (9,7)
                pa2.addMark(game.getMark(n - (i - j), n - j));
            }
            pa1.startNextLine();
            pa2.startNextLine();
        }

        // Scan rows and columns.
        for (int i = 0, size = game.getFieldSize(); i < size; ++i) {
            for (int j = 0; j < size; ++j) {
                pa1.addMark(game.getMark(i, j));
                pa2.addMark(game.getMark(j, i));
            }
            pa1.startNextLine();
            pa2.startNextLine();
        }

        return pa1.getTotalPoints() + pa2.getTotalPoints();
    }

    /**
     * A score accumulator that receives marks when the field is scanned.
     *
     * <p>The field scan direction is determined by the external user of this class.
     * For this accumulator, the input looks like a continuous stream of marks placed in a single line.
     */
    private class HeuristicPointAccumulator {
        /** The total number of accumulated points. */
        private int totalPoints;
        /** The number of player's own marks counted. */
        private int hits;
        /** The number of empty cells counted between the player's own marks. */
        private int empty;
        /** The longest continuous sequence of player's own marks in the current line (i.e. without empty cells). */
        private int longestContinuousHits;
        /** The current continuous sequence of player's own marks in the current line. */
        private int continuousHits;

        /**
         * Completely resets all data.
         */
        public void reset() {
            totalPoints = 0;
            empty = 0;
            hits = 0;
            longestContinuousHits = 0;
        }

        /**
         * Returns the total number of accumulated points.
         */
        public int getTotalPoints() {
            return totalPoints;
        }

        /**
         * Adds a mark to the current line.
         *
         * <p>Automatically starts a new line if it detects an opponent's mark.
         *
         * @param mark The mark to add.
         */
        public void addMark(Mark mark) {
            if (mark == Mark.EMPTY) {
                ++empty;
                breakContinuousHits();
            } else if (mark == heuristicTargetMark) {
                ++hits;
                ++continuousHits;
            } else {
                breakContinuousHits();
                startNextLine();
            }
        }

        /**
         * Restarts the current continuous sequence of player's own marks.
         */
        private void breakContinuousHits() {
            longestContinuousHits = Math.max(longestContinuousHits, continuousHits);
            continuousHits = 0;
        }

        /**
         * Proceeds to scan the next line of marks.
         *
         * <p>When moving to the next line, it calculates the number of points received
         * in the current line and adds this value to the total number of points.
         */
        public void startNextLine() {
            if (hits + empty >= comboSize) {
                // Rewarding the AI for long sets of own marks and empty cells located between opponent's marks.
                totalPoints += HIT_POINTS[Math.min(hits, HIT_POINTS.length - 1)] + empty * EMPTY_POINTS;

                // Force the AI to block long continuous opponent's sequences. This value depends on the number of
                // empty cells in the line so that the AI will interrupt the sequence by filling the empty cells with
                // its marks.
                if (longestContinuousHits > MIN_REWARDED_SEQUENCE) {
                    totalPoints += empty * SEQUENCE_POINTS;
                }
            }
            empty = 0;
            hits = 0;
            longestContinuousHits = 0;
        }
    }

    /**
     * Nodes that will be used in the MTD(f) cache generation process.
     */
    private static class Node {
        public int lowerBound;
        public int upperBound;
    }

    /**
     * A pool of nodes that allows them to be reused.
     */
    private static class NodePool {
        private final List<Node> pool = new ArrayList<>();
        private int head;

        /**
         * Tries to retrieve a node for reuse. If the storage is empty, creates a new node.
         *
         * @return A new node.
         */
        public Node obtain() {
            Node node;
            if (head >= pool.size()) {
                node = new Node();
                pool.add(node);
            } else {
                node = pool.get(head);
            }
            ++head;
            node.lowerBound = Integer.MIN_VALUE;
            node.upperBound = Integer.MAX_VALUE;
            return node;
        }

        /**
         * Puts all created nodes back into the pool for reuse.
         */
        public void freeAll() {
            head = 0;
        }
    }
}
