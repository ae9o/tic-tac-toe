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

package ae9o.tictactoe.game;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Random;

/**
 * The core of the tic-tac-toe game with the main logic.
 *
 * <p>Supports game fields of custom size.
 *
 * <p>Tracks the sequence of players' moves and monitors finish conditions.
 *
 * <p>Contains a set of listeners for all major game events:
 * <ol>
 * <li>Game start {@link TicTacToeGame#setOnGameStartListener(OnGameStartListener)}.
 * <li>Game finish {@link TicTacToeGame#setOnGameFinishListener(OnGameFinishListener)}.
 * <li>Setting a mark {@link TicTacToeGame#setOnMarkSetListener(OnMarkSetListener)}.
 * </ol>
 *
 * <p>Written in pure Java and can be ported to any platform.
 */
public class TicTacToeGame {
    /** Default turn order. */
    private static final Mark[] TURN_ORDER = {Mark.X, Mark.O};
    private static final int X_TURN_INDEX = 0;
    private static final int O_TURN_INDEX = 1;

    /** Limitation on the maximum size of the winning combo for large fields to make the game interesting. */
    private static final int MAX_COMBO_SIZE = 5;

    /** Characteristics of the game field. */
    private Mark[][] field;
    private int fieldSize;
    private int emptyCellCount;

    /** Indicates that the game is in progress. */
    private boolean active;

    /** The index of the mark in {@code TURN_ORDER} that will be placed next. */
    private int currentTurnIndex;

    /** Result at the end of the game. */
    private GameResult result = GameResult.UNDEFINED;

    /** The coordinates of the winning combo collected by a player. */
    private final Combo combo = new Combo();
    private int comboSize;

    /** Stuff for building transposition table used for Zobrist hashing.
     *
     * @see <a href="https://en.wikipedia.org/wiki/Zobrist_hashing">Zobrist hashing</a> */
    private final Random random = new Random();
    private final long[][][] transpositionTable = new long[Mark.values().length][][];
    private long transpositionHash;

    /** A copy of the current state of the game that can be safely passed to another thread. */
    private TicTacToeGameSnapshot snapshot;

    /** This event fires when a new game is started. */
    private OnGameStartListener onGameStartListener;
    /** This event fires when the game finishes. */
    private OnGameFinishListener onGameFinishListener;
    /** This event fires when a player places a new mark on the field. */
    private OnMarkSetListener onMarkSetListener;

    /**
     * Returns the current listener for game start event.
     */
    @Nullable
    public OnGameStartListener getOnGameStartListener() {
        return onGameStartListener;
    }

    /**
     * Sets a new game start listener.
     *
     * @param onGameStartListener New listener.
     */
    public void setOnGameStartListener(@Nullable OnGameStartListener onGameStartListener) {
        this.onGameStartListener = onGameStartListener;
    }

    /**
     * Returns the current listener for game finish event.
     */
    @Nullable
    public OnGameFinishListener getOnGameFinishListener() {
        return onGameFinishListener;
    }

    /**
     * Sets a new game finish listener.
     *
     * @param onGameFinishListener New listener.
     */
    public void setOnGameFinishListener(@Nullable OnGameFinishListener onGameFinishListener) {
        this.onGameFinishListener = onGameFinishListener;
    }

    /**
     * Returns the current listener for mark set event.
     */
    @Nullable
    public OnMarkSetListener getOnMarkSetListener() {
        return onMarkSetListener;
    }

    /**
     * Sets a new mark set listener.
     *
     * @param onMarkSetListener New listener.
     */
    public void setOnMarkSetListener(@Nullable OnMarkSetListener onMarkSetListener) {
        this.onMarkSetListener = onMarkSetListener;
    }

    /**
     * Returns a copy of the current state of the game that can be safely passed to another thread.
     *
     * <p>All calls return the same reference. The method should not be called again until the processing of the
     * previous snapshot is completely completed.
     */
    public TicTacToeGameSnapshot getSnapshot() {
        if (snapshot == null) {
            snapshot = new TicTacToeGameSnapshot();
        }
        snapshot.assign(this);
        return snapshot;
    }

    /**
     * Copies all properties from the specified game instance.
     *
     * @param other The instance from which the properties will be copied.
     */
    protected void assign(TicTacToeGame other) {
        setFieldSize(other.fieldSize);
        for (int i = 0; i < other.fieldSize; ++i) {
            System.arraycopy(other.field[i], 0, field[i], 0, other.fieldSize);
        }

        setupTranspositionTable(false);
        for (int i = 1 /* skip 0 */; i < other.transpositionTable.length; ++i) {
            for (int j = 0; j < other.fieldSize; ++j) {
                System.arraycopy(other.transpositionTable[i][j], 0, transpositionTable[i][j], 0, other.fieldSize);
            }
        }

        combo.assign(other.combo);
        active = other.active;
        result = other.result;
        emptyCellCount = other.emptyCellCount;
        currentTurnIndex = other.currentTurnIndex;
        transpositionHash = other.transpositionHash;
    }

    /**
     * Starts a new game on a field of the given size.
     *
     * <p>When starting a new game, there should be no other one active. It must be explicitly finished before
     * this point, otherwise an exception will be thrown.
     *
     * @param fieldSize The size of the new game field.
     * @param swapMarks Should the first player use the "O" mark.
     */
    public void start(int fieldSize, boolean swapMarks) throws IllegalStateException {
        if (active) {
            throw new IllegalStateException("Attempt to start an active game.");
        }
        active = true;

        setupEmptyField(fieldSize);
        setupTranspositionTable(true);
        setupTurns(swapMarks);
        result = GameResult.UNDEFINED;

        notifyGameStarted();
    }

    /**
     * Sets up an empty game field of the given size.
     *
     * @param fieldSize The size of the new game field.
     */
    private void setupEmptyField(int fieldSize) {
        setFieldSize(fieldSize);
        for (int i = 0; i < fieldSize; ++i) {
            Arrays.fill(field[i], Mark.EMPTY);
        }
        emptyCellCount = fieldSize * fieldSize;
    }

    /**
     * Sets a transposition table for the current game field, used for Zobrist hashing.
     *
     * <p>If the game field size is reduced, the previous version of the transposition table is kept to avoid
     * unnecessary garbage collection.
     *
     * @param fill True if the table is to be filled with numbers,
     *             False will only allocate the table without filling.
     */
    private void setupTranspositionTable(boolean fill) {
        transpositionHash = 0L;

        if ((transpositionTable[0] != null) && (transpositionTable[0].length >= fieldSize)) {
            return;
        }

        for (int i = 0; i < transpositionTable.length; ++i) {
            transpositionTable[i] = new long[fieldSize][];
            for (int j = 0; j < fieldSize; ++j) {
                transpositionTable[i][j] = new long[fieldSize];
                if (fill && (i > 0)) {
                    // The transposition table for EMPTY mark is left filled with zeros so that the hash of the start
                    // transposition is zero.
                    for (int k = 0; k < fieldSize; ++k) {
                        transpositionTable[i][j][k] = random.nextLong();
                    }
                }
            }
        }
    }

    /**
     * Sets the turn order at the start of the game so that the first player can use the "O" mark.
     *
     * @param swapMarks Should the first player use the "O" mark.
     */
    private void setupTurns(boolean swapMarks) {
        currentTurnIndex = swapMarks ? O_TURN_INDEX : X_TURN_INDEX;
    }

    /**
     * Fires a game start event.
     */
    protected void notifyGameStarted() {
        if (onGameStartListener != null) {
            onGameStartListener.onGameStart(fieldSize);
        }
    }

    /**
     * Indicates that the game is in progress.
     *
     * @return True if the game is in progress.
     *         False otherwise.
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Returns the size of the game field.
     */
    public int getFieldSize() {
        return fieldSize;
    }

    /**
     * Prepares an array of sufficient capacity to accommodate the specified game field.
     *
     * @param fieldSize The size of the new game field.
     */
    private void setFieldSize(int fieldSize) {
        if (fieldSize == this.fieldSize) {
            return;
        }
        this.fieldSize = fieldSize;

        comboSize = Math.min(fieldSize, MAX_COMBO_SIZE);

        if ((field != null) && (field.length >= fieldSize)) {
            return;
        }
        field = new Mark[fieldSize][];
        for (int i = 0; i < fieldSize; ++i) {
            field[i] = new Mark[fieldSize];
        }
    }

    /**
     * Finishes the current game with the specified result.
     *
     * <p>If the game has already been finished before, nothing will happen.
     *
     * @param result The result of the game.
     */
    private void finish(GameResult result) {
        if (!active) {
            return;
        }
        active = false;

        this.result = result;
        notifyGameFinished();
    }

    /**
     * Finishes the current game with {@code GameResult.CANCELED} result.
     */
    public void finish() {
        finish(GameResult.CANCELED);
    }

    /**
     * Fires a game finished event.
     */
    protected void notifyGameFinished() {
        if (onGameFinishListener != null) {
            onGameFinishListener.onGameFinish(result, combo);
        }
    }

    /**
     * Scans all lines of the field passing through the given cell, looking for a winning combo.
     *
     * <p>Checks vertical and horizontal lines, as well as both diagonals.
     *
     * <p>The winning combo is considered to be a continuous set of identical marks, the size
     * of which is {@code comboSize}.
     *
     * @param row The target row.
     * @param col The target col.
     * @return True if a winning combo is found;
     *         False otherwise.
     */
    public boolean findCombo(int row, int col) {
        return findComboRow(row, col) || findComboCol(row, col) || findComboBackslashDiagonal(row, col)
                || findComboSlashDiagonal(row, col);
    }

    /**
     * Scans the row around the given cell looking for a winning combo.
     *
     * <p>The winning combo is considered to be a continuous set of identical marks, the size
     * of which is {@code comboSize}.
     *
     * @param row The target row.
     * @param col The target col.
     * @return True if a winning combo is found;
     *         False otherwise.
     */
    private boolean findComboRow(int row, int col) {
        final Mark sample = field[row][col];
        int left = 0;
        for (int i = 1; i <= col; ++i, ++left) {
            if (field[row][col - i] != sample) {
                break;
            }
        }
        int right = 0;
        for (int i = 1, n = fieldSize - col; i < n; ++i, ++right) {
            if (field[row][col + i] != sample) {
                break;
            }
        }
        if (left + right + 1 >= comboSize) {
            combo.setStartRow(row);
            combo.setStopRow(row);
            combo.setStartCol(col - left);
            combo.setStopCol(col + right);
            return true;
        }
        return false;
    }

    /**
     * Scans the column around the given cell looking for a winning combo.
     *
     * <p>The winning combo is considered to be a continuous set of identical marks, the size
     * of which is {@code comboSize}.
     *
     * @param row The target row.
     * @param col The target col.
     * @return True if a winning combo is found;
     *         False otherwise.
     */
    private boolean findComboCol(int row, int col) {
        final Mark sample = field[row][col];
        int bottom = 0;
        for (int i = 1, n = fieldSize - row; i < n; ++i, ++bottom) {
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
            combo.setStartCol(col);
            combo.setStopCol(col);
            combo.setStartRow(row - top);
            combo.setStopRow(row + bottom);
            return true;
        }
        return false;
    }

    /**
     * Scans the backslash direction diagonal (\) around the given cell looking for a winning combo.
     *
     * <p>The winning combo is considered to be a continuous set of identical marks, the size
     * of which is {@code comboSize}.
     *
     * @param row The target row.
     * @param col The target col.
     * @return True if a winning combo is found;
     *         False otherwise.
     */
    private boolean findComboBackslashDiagonal(int row, int col) {
        final Mark sample = field[row][col];
        int bottom = 0;
        for (int i = 1, n = fieldSize - row, m = fieldSize - col; (i < n) && (i < m); ++i, ++bottom) {
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
            combo.setStartRow(row - top);
            combo.setStartCol(col - top);
            combo.setStopRow(row + bottom);
            combo.setStopCol(col + bottom);
            return true;
        }
        return false;
    }

    /**
     * Scans the slash direction diagonal (/) around the given cell looking for a winning combo.
     *
     * <p>The winning combo is considered to be a continuous set of identical marks, the size
     * of which is {@code comboSize}.
     *
     * @param row The target row.
     * @param col The target col.
     * @return True if a winning combo is found;
     *         False otherwise.
     */
    private boolean findComboSlashDiagonal(int row, int col) {
        final Mark sample = field[row][col];
        int bottom = 0;
        for (int i = 1, n = fieldSize - row; (i < n) && (i <= col); ++i, ++bottom) {
            if (field[row + i][col - i] != sample) {
                break;
            }
        }
        int top = 0;
        for (int i = 1, n = fieldSize - col; (i <= row) && (i < n); ++i, ++top) {
            if (field[row - i][col + i] != sample) {
                break;
            }
        }
        if (bottom + top + 1 >= comboSize) {
            combo.setStartRow(row - top);
            combo.setStartCol(col + top);
            combo.setStopRow(row + bottom);
            combo.setStopCol(col - bottom);
            return true;
        }
        return false;
    }

    /**
     * Returns the mark set in the given field cell.
     *
     * @param row The target row.
     * @param col The target col.
     * @return The current mark.
     */
    public Mark getMark(int row, int col) {
        return field[row][col];
    }

    /**
     * Sets the mark of the active player to the given cell.
     *
     * <p>The selected cell must be empty.
     *
     * <p>After the mark is successfully set, it generates the corresponding event.
     *
     * <p>Calling this method in an inactive game will throw an exception.
     *
     * @param row The target row.
     * @param col The target col.
     * @return True if the mark was successfully set;
     *         False otherwise.
     */
    public boolean setMark(int row, int col) throws IllegalStateException {
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

    /**
     * For internal use.
     *
     * <p>This method is required for AI to work.
     *
     * <p>Sets the given mark to the given cell without any restrictions.
     *
     * <p>Does not generate an event.
     *
     * @param row The target row.
     * @param col The target col.
     * @param mark The target mark.
     */
    protected void setMarkInternal(int row, int col, Mark mark) {
        transpositionHash ^= transpositionTable[field[row][col].ordinal()][row][col];
        field[row][col] = mark;
        emptyCellCount += (mark == Mark.EMPTY) ? 1 : -1;
        transpositionHash ^= transpositionTable[mark.ordinal()][row][col];
    }

    /**
     * Returns the current transposition hash.
     */
    public long getTranspositionHash() {
        return transpositionHash;
    }

    /**
     * Returns the remaining number of empty cells.
     */
    public int getEmptyCellCount() {
        return emptyCellCount;
    }

    /**
     * Fires a mark set event.
     *
     * @param row The target row.
     * @param col The target col.
     */
    protected void notifyMarkSet(int row, int col) {
        if (onMarkSetListener != null) {
            onMarkSetListener.onMarkSet(field[row][col], row, col);
        }
    }

    /**
     * Internal handler for the mark set event.
     *
     * <p>Checks the conditions for finishing the game after setting a new mark.
     *
     * <p>Switches turns.
     *
     * @param row The target row.
     * @param col The target col.
     */
    private void onMarkSet(int row, int col) {
        if (findCombo(row, col)) {
            finish(GameResult.COMBO);
        } else if (isFinalTurn()) {
            finish(GameResult.DRAW);
        } else {
            switchTurn();
        }
    }

    /**
     * Returns the result of the game.
     */
    public GameResult getResult() {
        return result;
    }

    /**
     * Returns the winning combo. Defined when the game result is {@code GameResult.COMBO}.
     */
    public Combo getCombo() {
        return combo;
    }

    /**
     * Returns the size of the combo.
     */
    public int getComboSize() {
        return comboSize;
    }

    /**
     * Returns the index of the mark in {@code TURN_ORDER} that will be placed next.
     */
    public int getCurrentTurnIndex() {
        return currentTurnIndex;
    }

    /**
     * Returns the mark of the current player.
     */
    public Mark getCurrentTurn() {
        return TURN_ORDER[currentTurnIndex];
    }

    /**
     * Returns the mark of the next player.
     */
    public Mark getNextTurn() {
        return TURN_ORDER[currentTurnIndex ^ 1];
    }

    /**
     * Activates next player.
     *
     * <p>This method is required for AI to work.
     */
    protected void switchTurn() {
        currentTurnIndex ^= 1;
    }

    /**
     * Checks if there are empty cells in the field.
     *
     * @return True if there are empty cells left;
     *         False otherwise.
     */
    public boolean isFinalTurn() {
        return (emptyCellCount == 0);
    }

    /** This event fires when a new game is started. */
    public interface OnGameStartListener {
        void onGameStart(int size);
    }

    /** This event fires when the game finishes. */
    public interface OnGameFinishListener {
        void onGameFinish(GameResult result, Combo combo);
    }

    /** This event fires when a player places a new mark on the field. */
    public interface OnMarkSetListener {
        void onMarkSet(Mark mark, int row, int col);
    }
}
