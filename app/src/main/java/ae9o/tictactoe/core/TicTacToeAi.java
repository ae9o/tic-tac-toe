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

public class TicTacToeAi {
    private static final int[] HIT_POINTS = {0, 10, 100, 1000, 10000, 100000, 1000000};
    private static final int EMPTY_POINTS = 10;
    private static final int SEQUENCE_POINTS = 100000000;
    private static final int MIN_REWARDED_SEQUENCE = 3;
    private static final long MAX_SEARCH_TIME = 1000000000L;

    private final TicTacToeGame game;

    private final Long2ObjectMap<Node> nodes = new Long2ObjectOpenHashMap<>();
    private NodePool nodePool;
    private SoftReference<NodePool> nodePoolRef = new SoftReference<>(new NodePool());

    private final Cell guess = new Cell();
    private int maxDepth;
    private boolean maxDepthTouched;

    private int comboSize;
    private Mark maxPlayerMark;
    private Mark minPlayerMark;
    private Mark heuristicTargetMark;
    private final HeuristicPointAccumulator pa1 = new HeuristicPointAccumulator();
    private final HeuristicPointAccumulator pa2 = new HeuristicPointAccumulator();

    public TicTacToeAi(TicTacToeGame game) {
        this.game = game;
    }

    public Cell guessNextMove() {
        setupPool();

        comboSize = game.getComboSize();

        final long startTime = System.nanoTime();
        int firstGuess = 0;
        maxDepth = 1;
        do {
            maxDepthTouched = false;
            firstGuess = mtdf(firstGuess);
            ++maxDepth;
        } while (maxDepthTouched && (System.nanoTime() - startTime < MAX_SEARCH_TIME));

        disposePool();
        return guess;
    }

    private void setupPool() {
        nodePool = nodePoolRef.get();
        if (nodePool == null) {
            nodePool = new NodePool();
            nodePoolRef = new SoftReference<>(nodePool);
        }
    }

    private void disposePool() {
        nodes.clear();
        nodePool.freeAll();
        nodePool = null;
    }

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

    private int rootMinimax(final int alpha, final int beta) {
        maxPlayerMark = game.getCurrentTurn();
        game.switchTurn();
        minPlayerMark = game.getCurrentTurn();

        int g = Integer.MIN_VALUE;
        int a = alpha;
        maximize:
        for (int row = 0, size = game.getSize(); row < size; ++row) {
            for (int col = 0; col < size; ++col) {
                if (game.getMark(row, col) == Mark.EMPTY) {
                    game.setMarkInternal(row, col, maxPlayerMark);
                    final int tmp = nestedMinimax(true, row, col, 0, a, beta);
                    game.setMarkInternal(row, col, Mark.EMPTY);

                    if (tmp > g) {
                        g = tmp;
                        guess.row = row;
                        guess.col = col;
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

    private int nestedMinimax(final boolean minimize, final int prevRow, final int prevCol, final int depth, int alpha,
                              int beta) {
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

        // Heuristic.
        if (depth == maxDepth) {
            maxDepthTouched = true;
            return evaluateHeuristic(maxPlayerMark) - evaluateHeuristic(minPlayerMark);
        }

        game.switchTurn();

        final int d = depth + 1;
        int g;
        if (minimize) {
            g = Integer.MAX_VALUE;
            int b = beta;
            minimize:
            for (int row = 0, size = game.getSize(); row < size; ++row) {
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
            for (int row = 0, size = game.getSize(); row < size; ++row) {
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

    private int evaluateHeuristic(Mark target) {
        heuristicTargetMark = target;
        pa1.reset();
        pa2.reset();

        // top diagonals
        for (int i = 0, size = game.getSize(); i < size; ++i) {
            for (int j = 0, n = size - 1, m = n - i; j <= i; ++j) {
                //diagonal1 \ top
                // (0,9)
                // (0,8) -> (1,9)
                // (0,7) -> (1,8) -> (2,9)
                pa1.addMark(game.getMark(j, m + j));

                //diagonal2 / top
                //0 0
                //0 1  1 0
                //0 2  1 1  2 0
                pa2.addMark(game.getMark(j, i - j));

            }
            pa1.startNextRow();
            pa2.startNextRow();
        }

        // bottom diagonals
        for (int i = 0, n = game.getSize() - 1; i < n; ++i) {
            for (int j = 0, m = n - i; j <= i; ++j) {
                //diagonal1 \ bottom
                //9 0
                //8 0  9 1
                //7 0  8 1  9 2
                pa1.addMark(game.getMark(m + j, j));

                //diagonal2 / bottom
                //9 9
                //8 9  9 8
                //7 9  8 8  9 7
                pa2.addMark(game.getMark(n - (i - j), n - j));
            }
            pa1.startNextRow();
            pa2.startNextRow();
        }

        // rows and cols
        for (int i = 0, size = game.getSize(); i < size; ++i) {
            for (int j = 0; j < size; ++j) {
                pa1.addMark(game.getMark(i, j));
                pa2.addMark(game.getMark(j, i));
            }
            pa1.startNextRow();
            pa2.startNextRow();
        }

        return pa1.getTotalPoints() + pa2.getTotalPoints();
    }

    private class HeuristicPointAccumulator {
        private int totalPoints;
        private int hits;
        private int empty;
        private int longestHitSequence;
        private int hitSequence;

        public void reset() {
            totalPoints = 0;
            empty = 0;
            hits = 0;
            longestHitSequence = 0;
        }

        public int getTotalPoints() {
            return totalPoints;
        }

        public void addMark(Mark mark) {
            if (mark == Mark.EMPTY) {
                ++empty;
                restartSequence();
            } else if (mark == heuristicTargetMark) {
                ++hits;
                ++hitSequence;
            } else {
                restartSequence();
                startNextRow();
            }
        }

        private void restartSequence() {
            longestHitSequence = Math.max(longestHitSequence, hitSequence);
            hitSequence = 0;
        }

        public void startNextRow() {
            if (hits + empty >= comboSize) {
                // Rewarding the AI for long sets of own marks and empty cells located between enemy marks.
                totalPoints += HIT_POINTS[Math.min(hits, HIT_POINTS.length - 1)] + empty * EMPTY_POINTS;

                // Force the AI to block long continuous enemy sequences. This value depends on the number of empty
                // cells in the row so that the AI will interrupt the sequence by filling the empty cells with its
                // marks.
                if (longestHitSequence > MIN_REWARDED_SEQUENCE) {
                    totalPoints += empty * SEQUENCE_POINTS;
                }
            }
            empty = 0;
            hits = 0;
            longestHitSequence = 0;
        }
    }

    private static class Node {
        public int lowerBound;
        public int upperBound;
    }

    private static class NodePool {
        private final List<Node> pool = new ArrayList<>();
        private int head;

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

        public void freeAll() {
            head = 0;
        }
    }

    public static class Cell {
        private int row;
        private int col;

        public int getRow() {
            return row;
        }

        public int getCol() {
            return col;
        }
    }
}
