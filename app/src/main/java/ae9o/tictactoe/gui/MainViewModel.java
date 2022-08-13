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

import ae9o.tictactoe.game.Combo;
import ae9o.tictactoe.game.GameResult;
import ae9o.tictactoe.game.Mark;
import ae9o.tictactoe.utils.Async;
import ae9o.tictactoe.ai.MtdfTicTacToeAi;
import ae9o.tictactoe.ai.TicTacToeAiResult;
import ae9o.tictactoe.ai.TicTacToeAiExecutor;
import ae9o.tictactoe.ai.TicTacToeAiExecutorResult;
import ae9o.tictactoe.ai.TicTacToeAiExecutor.OnTicTacToeAiExecutorTaskCompleteListener;
import ae9o.tictactoe.game.TicTacToeGame;
import ae9o.tictactoe.game.TicTacToeGame.*;
import ae9o.tictactoe.gui.utils.NonNullMutableLiveData;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import java.util.Random;

/**
 * ViewModel for the MainActivity.
 *
 * <p>Encapsulates direct interaction with the game core and AI.
 *
 * <p>Provides an app-wide non-persistent (is reset when the app is terminated) centralized storage of game settings and
 * makes them independent of the Activity life cycle.
 *
 * @see <a href="https://developer.android.com/guide/components/activities/activity-lifecycle">Activity Lifecycle</a>
 * @see <a href="https://en.wikipedia.org/wiki/Model%E2%80%93view%E2%80%93viewmodel">Model–view–viewmodel</a>
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

    /** The core of the game. */
    private final TicTacToeGame game = new TicTacToeGame();

    /** AI running on a separate thread. */
    private final TicTacToeAiExecutor aiExecutor = new TicTacToeAiExecutor(new MtdfTicTacToeAi());
    /** Handler to post AI guesses to the main thread. */
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    /** Flag to block user input. */
    private boolean aiTurn;
    /** To select random coordinates on the first move. */
    private Random random;

    /** Wrappers over various game events that need to be pre-processed before being passed to the presentation layer. */
    private OnGameStartListener onGameStartListener;
    private OnMarkSetListener onMarkSetListener;
    private OnGameFinishListener onGameFinishListener;

    /**
     * Creates a MainViewModel containing the game automatically started with default settings.
     */
    public MainViewModel() {
        game.setOnGameStartListener(this::onGameStart);
        game.setOnMarkSetListener(this::onMarkSet);
        game.setOnGameFinishListener(this::onGameFinish);

        aiExecutor.setOnTicTacToeAiExecutorTaskCompleteListener(this::onTicTacToeAiExecutorTaskComplete);
        addCloseable(aiExecutor);

        startGame();
    }

    /**
     * Replays all events that have occurred since the beginning of the game. Used to restore the state of the Activity
     * after it has been recreated.
     */
    public void replay() {
        replayOnGameStart();
        replayOnMarkSet();
        replayOnGameFinish();
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
            aiExecutor.cancelTask();
            game.finish();
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

    /**
     * Places a mark in a random cell on an empty field if the AI is activated.
     */
    private void makeRandomFirstMove() {
        aiTurn = isAiStarts();
        if (!aiTurn) {
            return;
        }
        if (random == null) {
            random = new Random();
        }
        final int size = game.getFieldSize();
        setMark(random.nextInt(size), random.nextInt(size), false);
    }

    /**
     * Sets a mark of the current player in the specified cell of the field.
     *
     * <p>After the user's turn, automatically starts the work of the AI, if it is enabled in the settings.
     *
     * @param row The row coordinate of the cell.
     * @param col The col coordinate of the cell.
     * @param fromUser True if the action was initiated by the user and not by the AI;
     *                 False if the action was initiated by the AI.
     */
    public void setMark(int row, int col, boolean fromUser) {
        if (!game.isActive()) {
            return;
        }
        if (aiTurn == fromUser) {
            return;
        }
        if (!game.setMark(row, col)) {
            return;
        }
        if (game.isActive() && aiEnabled.getValue()) {
            aiTurn = !aiTurn;
            if (aiTurn) {
                aiExecutor.guessNextMoveAsync(game.getSnapshot());
            }
        }
    }

    /**
     * Intercepts to the {@link TicTacToeGame#setOnGameStartListener(OnGameStartListener)} event.
     *
     * <p>Makes a random first move if AI is enabled in settings.
     *
     * @param fieldSize The size of the field in the started game.
     */
    private void onGameStart(int fieldSize) {
        notifyGameStarted(fieldSize);
        makeRandomFirstMove();
    }

    /**
     * Intercepts to the {@link TicTacToeGame#setOnGameFinishListener(OnGameFinishListener)} event.
     *
     * @param result The result of the game.
     * @param combo The coordinates of the combo collected by the player.
     *              Defined when the {@code result} is {@code TicTacToeGame.GameResult.COMBO}.
     */
    private void onGameFinish(GameResult result, Combo combo) {
        updateScore(result, combo);
        notifyGameFinished(result, combo);
    }

    /**
     * Intercepts to the {@link TicTacToeGame#setOnMarkSetListener(TicTacToeGame.OnMarkSetListener)} event.
     *
     * <p>Renders the set mark.
     *
     * @param mark The mark set.
     * @param row The row coordinate.
     * @param col The col coordinate.
     */
    private void onMarkSet(Mark mark, int row, int col) {
        notifyMarkSet(mark, row, col);
    }

    /**
     * Listens to the
     * {@link TicTacToeAiExecutor#setOnTicTacToeAiExecutorTaskCompleteListener(OnTicTacToeAiExecutorTaskCompleteListener)}
     * event.
     *
     * <p>Posts AI guesses to the main thread.
     *
     * @param executorResult The results of AI work.
     */
    @Async
    private void onTicTacToeAiExecutorTaskComplete(TicTacToeAiExecutorResult executorResult) {
        mainHandler.post(() -> {
            if (!executorResult.isCanceled()) {
                TicTacToeAiResult aiResult = executorResult.getAiResult();
                setMark(aiResult.getRow(), aiResult.getCol(), false);
            }
        });
    }

    public Mark getCurrentTurn() {
        return game.getCurrentTurn();
    }

    public Mark getNextTurn() {
        return game.getNextTurn();
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

    private void notifyMarkSet(Mark mark, int row, int col) {
        if (onMarkSetListener != null) {
            onMarkSetListener.onMarkSet(mark, row, col);
        }
    }
}
