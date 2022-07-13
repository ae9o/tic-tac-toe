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

import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.Random;

public class TicTacToeGame {
    private static final Mark[] TURN_ORDER = {Mark.X, Mark.O};
    private static final int X_TURN_INDEX = 0;
    private static final int O_TURN_INDEX = 1;

    private static final int MAX_COMBO_SIZE = 5;

    public enum Mark {
        EMPTY,
        X,
        O;

        public static final int EMPTY_ORDINAL = 0;
        public static final int X_ORDINAL = 1;
        public static final int O_ORDINAL = 2;
    }

    public enum GameResult {
        UNDEFINED,
        CANCELED,
        DRAW,
        COMBO
    }

    private Mark[][] field;
    private int size;
    private int emptyCellCount;
    private int currentTurnIndex;
    private boolean active;
    private GameResult result = GameResult.UNDEFINED;

    private final Combo combo = new Combo();
    private int comboSize;

    private final Random random = new Random();
    private final long[][][] transpositionTable = new long[Mark.values().length][][];
    private long transpositionHash;

    private OnGameStartListener onGameStartListener;
    private OnGameFinishListener onGameFinishListener;
    private OnMarkSetListener onMarkSetListener;

    @Nullable
    public OnGameStartListener getOnGameStartListener() {
        return onGameStartListener;
    }

    public void setOnGameStartListener(@Nullable OnGameStartListener onGameStartListener) {
        this.onGameStartListener = onGameStartListener;
    }

    @Nullable
    public OnGameFinishListener getOnGameFinishListener() {
        return onGameFinishListener;
    }

    public void setOnGameFinishListener(@Nullable OnGameFinishListener onGameFinishListener) {
        this.onGameFinishListener = onGameFinishListener;
    }

    @Nullable
    public OnMarkSetListener getOnMarkSetListener() {
        return onMarkSetListener;
    }

    public void setOnMarkSetListener(@Nullable OnMarkSetListener onMarkSetListener) {
        this.onMarkSetListener = onMarkSetListener;
    }

    public void start(int size, boolean swapMarks) {
        if (active) {
            throw new IllegalStateException("Attempt to start an active game.");
        }
        active = true;

        setupEmptyField(size);
        setupTranspositionTable();
        setupTurns(swapMarks);
        result = GameResult.UNDEFINED;

        notifyGameStarted();
    }

    private void setupEmptyField(int size) {
        setSize(size);
        for (int i = 0; i < size; ++i) {
            Arrays.fill(field[i], Mark.EMPTY);
        }
        emptyCellCount = size * size;
    }

    private void setupTranspositionTable() {
        transpositionHash = 0L;

        if ((transpositionTable[0] != null) && (transpositionTable[0].length >= size)) {
            // When the field size is reduced, keep the table size to avoid unnecessary garbage collection.
            return;
        }

        for (int i = 0; i < transpositionTable.length; ++i) {
            transpositionTable[i] = new long[size][];
            for (int j = 0; j < size; ++j) {
                transpositionTable[i][j] = new long[size];
                if (i > 0) {
                    // The transposition table for EMPTY mark is left filled with zeros so that the hash of the start
                    // transposition is zero.
                    for (int k = 0; k < size; ++k) {
                        transpositionTable[i][j][k] = random.nextLong();
                    }
                }
            }
        }
    }

    private void setupTurns(boolean swapMarks) {
        currentTurnIndex = swapMarks ? O_TURN_INDEX : X_TURN_INDEX;
    }

    private void notifyGameStarted() {
        if (onGameStartListener != null) {
            onGameStartListener.onGameStart(size);
        }
    }

    public boolean isActive() {
        return active;
    }

    public int getSize() {
        return size;
    }

    private void setSize(int size) {
        if (size == this.size) {
            return;
        }
        this.size = size;
        comboSize = Math.min(size, MAX_COMBO_SIZE);

        if ((field != null) && (field.length >= size)) {
            return;
        }
        field = new Mark[size][];
        for (int i = 0; i < size; ++i) {
            field[i] = new Mark[size];
        }
    }

    private void finish(GameResult result) {
        if (!active) {
            return;
        }
        active = false;

        this.result = result;
        notifyGameFinished();
    }

    public void finish() {
        finish(GameResult.CANCELED);
    }

    private void notifyGameFinished() {
        if (onGameFinishListener != null) {
            onGameFinishListener.onGameFinish(result, combo);
        }
    }

    public boolean findCombo(int row, int col) {
        return findComboRow(row, col) || findComboCol(row, col) || findComboDiagonal1(row, col)
                || findComboDiagonal2(row, col);
    }

    private boolean findComboRow(int row, int col) {
        final Mark sample = field[row][col];
        int left = 0;
        for (int i = 1; i <= col; ++i, ++left) {
            if (field[row][col - i] != sample) {
                break;
            }
        }
        int right = 0;
        for (int i = 1, n = size - col; i < n; ++i, ++right) {
            if (field[row][col + i] != sample) {
                break;
            }
        }
        if (left + right + 1 >= comboSize) {
            combo.startRow = row;
            combo.stopRow = row;
            combo.startCol = col - left;
            combo.stopCol = col + right;
            return true;
        }
        return false;
    }

    private boolean findComboCol(int row, int col) {
        final Mark sample = field[row][col];
        int bottom = 0;
        for (int i = 1, n = size - row; i < n; ++i, ++bottom) {
            if (field[row + i][col] != sample) {
                break;
            }
        }
        int top = 0;
        for (int i = 1; i <= row; ++i, ++top) {
            if (field[row - i][col] != sample) {
                break;
            }
        }
        if (bottom + top + 1 >= comboSize) {
            combo.startCol = col;
            combo.stopCol = col;
            combo.startRow = row - top;
            combo.stopRow = row + bottom;
            return true;
        }
        return false;
    }

    private boolean findComboDiagonal1(int row, int col) {
        final Mark sample = field[row][col];
        int bottom = 0;
        for (int i = 1, n = size - row, m = size - col; (i < n) && (i < m); ++i, ++bottom) {
            if (field[row + i][col + i] != sample) {
                break;
            }
        }
        int top = 0;
        for (int i = 1; (i <= row) && (i <= col); ++i, ++top) {
            if (field[row - i][col - i] != sample) {
                break;
            }
        }
        if (bottom + top + 1 >= comboSize) {
            combo.startRow = row - top;
            combo.startCol = col - top;
            combo.stopRow = row + bottom;
            combo.stopCol = col + bottom;
            return true;
        }
        return false;
    }

    private boolean findComboDiagonal2(int row, int col) {
        final Mark sample = field[row][col];
        int bottom = 0;
        for (int i = 1, n = size - row; (i < n) && (i <= col); ++i, ++bottom) {
            if (field[row + i][col - i] != sample) {
                break;
            }
        }
        int top = 0;
        for (int i = 1, n = size - col; (i <= row) && (i < n); ++i, ++top) {
            if (field[row - i][col + i] != sample) {
                break;
            }
        }
        if (bottom + top + 1 >= comboSize) {
            combo.startRow = row - top;
            combo.startCol = col + top;
            combo.stopRow = row + bottom;
            combo.stopCol = col - bottom;
            return true;
        }
        return false;
    }

    public Mark getMark(int row, int col) {
        return field[row][col];
    }

    public boolean setMark(int row, int col) {
        if (!active) {
            throw new IllegalStateException("Attempt to play an inactive game.");
        }
        if (field[row][col] != Mark.EMPTY) {
            return false;
        }
        setMarkInternal(row, col, getCurrentTurn());
        notifyMarkSet(row, col);
        onMarkSet(row, col);
        return true;
    }

    void setMarkInternal(int row, int col, Mark mark) {
        transpositionHash ^= transpositionTable[field[row][col].ordinal()][row][col];
        field[row][col] = mark;
        emptyCellCount += (mark == Mark.EMPTY) ? 1 : -1;
        transpositionHash ^= transpositionTable[mark.ordinal()][row][col];
    }

    public long getTranspositionHash() {
        return transpositionHash;
    }

    public int getEmptyCellCount() {
        return emptyCellCount;
    }

    private void notifyMarkSet(int row, int col) {
        if (onMarkSetListener != null) {
            onMarkSetListener.onMarkSet(field[row][col], row, col);
        }
    }

    private void onMarkSet(int row, int col) {
        if (findCombo(row, col)) {
            finish(GameResult.COMBO);
        } else if (isFinalTurn()) {
            finish(GameResult.DRAW);
        } else {
            switchTurn();
        }
    }

    public GameResult getResult() {
        return result;
    }

    public Combo getCombo() {
        return combo;
    }

    public int getComboSize() {
        return comboSize;
    }

    public int getCurrentTurnIndex() {
        return currentTurnIndex;
    }

    public Mark getCurrentTurn() {
        return TURN_ORDER[currentTurnIndex];
    }

    void switchTurn() {
        currentTurnIndex ^= 1;
    }

    public boolean isFinalTurn() {
        return (emptyCellCount == 0);
    }

    public static class Combo {
        private int startRow;
        private int startCol;
        private int stopRow;
        private int stopCol;

        public int getStartRow() {
            return startRow;
        }

        public int getStartCol() {
            return startCol;
        }

        public int getStopRow() {
            return stopRow;
        }

        public int getStopCol() {
            return stopCol;
        }
    }

    public interface OnGameStartListener {
        void onGameStart(int size);
    }

    public interface OnGameFinishListener {
        void onGameFinish(GameResult result, Combo combo);
    }

    public interface OnMarkSetListener {
        void onMarkSet(Mark mark, int row, int col);
    }
}
