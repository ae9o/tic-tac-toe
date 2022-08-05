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

import ae9o.tictactoe.core.TicTacToeAi.Cell;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * TODO description
 */
public class TicTacToeAiExecutor implements Closeable {
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final TicTacToeAi ai;
    private GuessNextMoveTask task;
    private OnAiGuessNextMoveCompleteListener onAiGuessNextMoveCompleteListener;

    public TicTacToeAiExecutor(TicTacToeAi ai) {
        this.ai = ai;
    }

    @Override
    public void close() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(TicTacToeAi.MAX_SEARCH_TIME, TimeUnit.NANOSECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Nullable
    public OnAiGuessNextMoveCompleteListener getOnAiGuessNextMoveCompleteListener() {
        return onAiGuessNextMoveCompleteListener;
    }

    public void setOnAiGuessNextMoveCompleteListener(
            @Nullable OnAiGuessNextMoveCompleteListener onAiGuessNextMoveCompleteListener) {
        this.onAiGuessNextMoveCompleteListener = onAiGuessNextMoveCompleteListener;
    }

    @Async
    private void notifyAiGuessNextMoveComplete(GuessNextMoveResult result) {
        if (onAiGuessNextMoveCompleteListener != null) {
            onAiGuessNextMoveCompleteListener.onAiGuessNextMoveComplete(result);
        }
    }

    public void guessNextMoveAsync(TicTacToeGame snapshot) {
        if ((task != null) && !task.getFuture().isDone()) {
            throw new IllegalStateException("Only one task must be active at a time.");
        }
        task = new GuessNextMoveTask(snapshot);
        task.setFuture(executorService.submit(task));
    }

    public void cancelTask() {
        if (task != null) {
            task.cancel();
        }
    }

    public class GuessNextMoveTask implements Runnable {
        private final GuessNextMoveResult result = new GuessNextMoveResult();
        private final TicTacToeGame snapshot;
        private Future<?> future;

        public GuessNextMoveTask(TicTacToeGame snapshot) {
            this.snapshot = snapshot;
        }

        public Future<?> getFuture() {
            return future;
        }

        public void setFuture(Future<?> future) {
            this.future = future;
        }

        @Override
        @Async
        public void run() {
            result.getCell().set(ai.guessNextMove(snapshot));
            notifyAiGuessNextMoveComplete(result);
        }

        public void cancel() {
            future.cancel(true);
            result.cancel();
        }
    }

    public static class GuessNextMoveResult {
        private final Cell cell = new Cell();
        private boolean canceled;

        public Cell getCell() {
            return cell;
        }

        public void cancel() {
            canceled = true;
        }

        public boolean isCanceled() {
            return canceled;
        }
    }

    public interface OnAiGuessNextMoveCompleteListener {
        @Async
        void onAiGuessNextMoveComplete(GuessNextMoveResult result);
    }
}
