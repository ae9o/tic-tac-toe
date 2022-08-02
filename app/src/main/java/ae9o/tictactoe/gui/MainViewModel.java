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

package ae9o.tictactoe.gui;

import ae9o.tictactoe.core.MtdfTicTacToeAi;
import ae9o.tictactoe.core.TicTacToeAiExecutor;
import ae9o.tictactoe.core.TicTacToeGame;
import ae9o.tictactoe.core.TicTacToeGame.*;
import ae9o.tictactoe.gui.utils.NonNullMutableLiveData;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import java.util.Random;

/**
 * Provides an app-wide non-persistent (is reset when the app is terminated) centralized storage of game settings and
 * makes them independent of the Activity life cycle.
 *
 * @see <a href="https://developer.android.com/guide/components/activities/activity-lifecycle">Activity Lifecycle</a>
 */
public class MainViewModel extends ViewModel {

    /** Note: these size limits only apply to the very simple user interface of this app.
     * The algorithm itself, implemented in the core, can work with any field size.
     *
     * The minimum allowed size of the game field. */
    public static final int MIN_GAME_FIELD_SIZE = 3;
    /** The maximum allowed size of the game field. */
    public static final int MAX_GAME_FIELD_SIZE = 15;
    /** The size of the field at app startup. */
    public static final int DEFAULT_GAME_FIELD_SIZE = 10;

    /** The actual size of the game field chosen. */
    private final NonNullMutableLiveData<Integer> fieldSize = new NonNullMutableLiveData<>(DEFAULT_GAME_FIELD_SIZE);
    /** Whether AI is enabled (if not, you can play with another person). */
    private final NonNullMutableLiveData<Boolean> aiEnabled = new NonNullMutableLiveData<>(true);
    /** Whether the AI should make the first move in a new game. */
    private final NonNullMutableLiveData<Boolean> aiStarts = new NonNullMutableLiveData<>(false);
    /** Allows the first player to use "O" instead of "X". */
    private final NonNullMutableLiveData<Boolean> swapMarks = new NonNullMutableLiveData<>(false);
    /** Points earned by "X" player. */
    private final NonNullMutableLiveData<Integer> xScore = new NonNullMutableLiveData<>(0);
    /** Points earned by "O" player. */
    private final NonNullMutableLiveData<Integer> oScore = new NonNullMutableLiveData<>(0);

    // TODO description
    private Random random;

    private final TicTacToeGame game = new TicTacToeGame();
    private final TicTacToeAiExecutor ai = new TicTacToeAiExecutor(game, new MtdfTicTacToeAi());

    private OnGameStartListener onGameStartListener;
    private OnMarkSetListener onMarkSetListener;
    private OnGameFinishListener onGameFinishListener;

    public MainViewModel() {
        addCloseable(ai);

        game.setOnGameStartListener(this::onGameStart);
        game.setOnMarkSetListener(this::onMarkSet);
        game.setOnGameFinishListener(this::onGameFinish);
    }

    public LiveData<Integer> getFieldSize() {
        return fieldSize;
    }

    public void setFieldSize(int fieldSize) {
        this.fieldSize.setValue(fieldSize);
    }

    public LiveData<Boolean> getAiEnabled() {
        return aiEnabled;
    }

    public void setAiEnabled(boolean aiEnabled) {
        this.aiEnabled.setValue(aiEnabled);
    }

    public LiveData<Boolean> getAiStarts() {
        return aiStarts;
    }

    public void setAiStarts(boolean aiStarts) {
        this.aiStarts.setValue(aiStarts);
    }

    public LiveData<Boolean> getSwapMarks() {
        return swapMarks;
    }

    public void setSwapMarks(boolean swapMarks) {
        this.swapMarks.setValue(swapMarks);
    }

    public LiveData<Integer> getXScore() {
        return xScore;
    }

    public void setXScore(int xScore) {
        this.xScore.setValue(xScore);
    }

    public LiveData<Integer> getOScore() {
        return oScore;
    }

    public void setOScore(int oScore) {
        this.oScore.setValue(oScore);
    }

    public boolean isAiStarts() {
        return aiEnabled.getValue() && aiStarts.getValue();
    }

    public void clearScore() {
        xScore.setValue(0);
        oScore.setValue(0);
    }

    @Nullable
    public OnGameStartListener getOnGameStartListener() {
        return onGameStartListener;
    }

    public void setOnGameStartListener(@Nullable OnGameStartListener onGameStartListener) {
        this.onGameStartListener = onGameStartListener;
    }

    @Nullable
    public OnMarkSetListener getOnMarkSetListener() {
        return onMarkSetListener;
    }

    public void setOnMarkSetListener(@Nullable OnMarkSetListener onMarkSetListener) {
        this.onMarkSetListener = onMarkSetListener;
    }

    @Nullable
    public OnGameFinishListener getOnGameFinishListener() {
        return onGameFinishListener;
    }

    public void setOnGameFinishListener(@Nullable OnGameFinishListener onGameFinishListener) {
        this.onGameFinishListener = onGameFinishListener;
    }

    private void notifyGameStarted(int fieldSize) {
        if (onGameStartListener != null) {
            onGameStartListener.onGameStart(fieldSize);
        }
    }

    private void notifyGameFinished(GameResult result, Combo combo) {
        if (onGameFinishListener != null) {
            onGameFinishListener.onGameFinish(result, combo);
        }
    }

    private void notifyMarkSet(TicTacToeGame.Mark mark, int row, int col) {
        if (onMarkSetListener != null) {
            onMarkSetListener.onMarkSet(mark, row, col);
        }
    }

    private void onGameStart(int fieldSize) {
        notifyGameStarted(fieldSize);
        if (isAiStarts()) {
            makeRandomFirstMove();
        }
    }

    private void onGameFinish(GameResult result, Combo combo) {
        updateScore(result, combo);
        notifyGameFinished(result, combo);
    }

    private void onMarkSet(TicTacToeGame.Mark mark, int row, int col) {
        notifyMarkSet(mark, row, col);
    }

    private void replayOnGameStart() {
        if (game.isActive() || (game.getResult() != GameResult.UNDEFINED)) {
            notifyGameStarted(game.getFieldSize());
        }
    }

    private void replayOnGameFinish() {
        if (game.getResult() != GameResult.UNDEFINED) {
            notifyGameFinished(game.getResult(), game.getCombo());
        }
    }

    private void replayOnMarkSet() {
        for (int row = 0, size = game.getFieldSize(); row < size; ++row) {
            for (int col = 0; col < size; ++col) {
                final Mark mark = game.getMark(row, col);
                if (mark != Mark.EMPTY) {
                    notifyMarkSet(mark, row, col);
                }
            }
        }
    }

    public void replay() {
        replayOnGameStart();
        replayOnMarkSet();
        replayOnGameFinish();
    }

    /**
     * Starts a new game.
     *
     * <p>If the previous game is still active, it will automatically finish.
     */
    public void startGame() {
        finishGame();
        game.start(fieldSize.getValue(), swapMarks.getValue());
    }

    /**
     * Finishes an active game, if any.
     */
    public void finishGame() {
        if (game.isActive()) {
            game.finish();
        }
    }

    /**
     * Places a mark in a random cell on an empty field.
     */
    private void makeRandomFirstMove() {
        if (random == null) {
            random = new Random();
        }
        final int size = game.getFieldSize();
        game.setMark(random.nextInt(size), random.nextInt(size));
    }

    /**
     * Sets a mark in the cell suggested by the AI.
     */
    private void makeAiMove() {
        if (game.isActive() && aiEnabled.getValue()) {
            // TODO
        }
    }

    /**
     * Adds points to the winner.
     *
     * @param result The result of the game.
     * @param combo The coordinates of the combo collected by the player.
     *              Defined when the {@code result} is {@code TicTacToeGame.GameResult.COMBO}.
     */
    private void updateScore(GameResult result, Combo combo) {
        if (result != GameResult.COMBO) {
            return;
        }
        switch (game.getMark(combo.getStartRow(), combo.getStartCol())) {
            case X:
                xScore.setValue(xScore.getValue() + 1);
                break;

            case O:
                oScore.setValue(oScore.getValue() + 1);
                break;

            default:
                // Do nothing.
                break;
        }
    }

    public void setMark(int row, int col) {
        if (!game.isActive()) {
            return;
        }

        // TODO check previous 'Future<TicTacToeAi.Cell> guessNextMove()' state

        if (!game.setMark(row, col)) {
            return;
        }
        makeAiMove();
    }

    public Mark getCurrentTurn() {
        return game.getCurrentTurn();
    }

    public Mark getNextTurn() {
        return game.getNextTurn();
    }
}
