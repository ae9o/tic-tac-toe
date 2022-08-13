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

package ae9o.tictactoe.ai;

/**
 * Contains results of {@link TicTacToeAiExecutor} work.
 *
 * <p>An instance of this class is formed by the executor in a separate thread, and then transferred to the UI thread
 * for final processing.
 *
 * <p>Before final processing, the value of the cancel flag should be checked. The AI task completion on a separate
 * thread and the user canceling the task on the main thread can happen at the same time. If these moments coincide, the
 * task result will be transferred to the main thread with the cancel flag set. Such results should simply be discarded.
 */
public class TicTacToeAiExecutorResult {
    private final TicTacToeAiResult aiResult = new TicTacToeAiResult();
    private boolean canceled;

    public TicTacToeAiResult getAiResult() {
        return aiResult;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void cancel() {
        canceled = true;
    }
}
