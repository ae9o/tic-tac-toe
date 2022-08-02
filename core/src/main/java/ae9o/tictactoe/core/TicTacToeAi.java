package ae9o.tictactoe.core;

// TODO description
public interface TicTacToeAi {
    // TODO description
    Cell guessNextMove(TicTacToeGame snapshot);

    /**
     * Stores coordinates of a cell.
     */
    class Cell {
        private int row;
        private int col;

        public int getRow() {
            return row;
        }

        public void setRow(int row) {
            this.row = row;
        }

        public int getCol() {
            return col;
        }

        public void setCol(int col) {
            this.col = col;
        }
    }
}
