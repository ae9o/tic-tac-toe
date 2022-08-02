package ae9o.tictactoe.core;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.Future;

// TODO description
public class TicTacToeAiExecutor implements Closeable {
    private OnGuessNextMoveCompleteListener onGuessNextMoveCompleteListener;

    public TicTacToeAiExecutor(TicTacToeGame game, TicTacToeAi ai) {
        // TODO
    }

    public Future<TicTacToeAi.Cell> guessNextMove() {
        // TODO
        return null;
    }

    @Nullable
    public OnGuessNextMoveCompleteListener getOnGuessNextMoveCompleteListener() {
        return onGuessNextMoveCompleteListener;
    }

    public void setOnGuessNextMoveCompleteListener(
            @Nullable OnGuessNextMoveCompleteListener onGuessNextMoveCompleteListener) {
        this.onGuessNextMoveCompleteListener = onGuessNextMoveCompleteListener;
    }

    @Override
    public void close() throws IOException {
        // TODO
    }

    public interface OnGuessNextMoveCompleteListener {
        // TODO
    }
}
