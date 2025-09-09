package cool.muyucloud.minesweeper.game;

import cool.muyucloud.minesweeper.widget.TextScreen;
import cool.muyucloud.minesweeper.widget.Widget;

import java.security.SecureRandom;

/**
 * The Board class represents the Minesweeper game board.
 * It manages the grid of slots, handles user interactions such as digging and flagging,
 * and keeps track of the game state including the number of mines, revealed slots, and flags.
 */
public class Board extends Widget<Board> {
    public static final SecureRandom RANDOM = new SecureRandom();

    private final int width;
    private final int height;
    private final Slot[][] slots;
    private int mine;
    private int revealed;
    private int flagged;
    private boolean gameOver;

    public Board(int width, int height) {
        this.width = width;
        this.height = height;
        slots = new Slot[height][width];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                Slot slot = new Slot();
                slot.setPos(j, i);
                slots[i][j] = slot;
                this.add(slots[i][j]);
            }
        }
    }

    /**
     * Dig at the specified coordinates.
     * If the slot contains a mine, the game is over.
     * If the slot is empty (0 adjacent mines), recursively dig adjacent slots.
     *
     * @param x The x-coordinate of the slot to dig.
     * @param y The y-coordinate of the slot to dig.
     * @return true if the dig was successful (no mine hit), false if a mine was hit.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean dig(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            throw new IndexOutOfBoundsException("Invalid slot coordinates");
        }
        Slot slot = slots[y][x];
        slot.setRevealed(true);
        revealed++;
        if (slot.isMine()) {
            return false; // Hit a mine
        }
        for (int i = x - 1; i <= x + 1; i++) {
            for (int j = y - 1; j <= y + 1; j++) {
                if (i >= 0 && i < width && j >= 0 && j < height) {
                    Slot neighbor = slots[j][i];
                    if (!neighbor.isRevealed() && !neighbor.isMine() && !neighbor.isFlagged() && slot.getAdjacentMines() == 0) {
                        if (!this.dig(i, j)) return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Make a guess at a revealed slot.
     * If the number of flagged adjacent slots equals the number of adjacent mines,
     * automatically dig all unflagged adjacent slots.
     * If any of these slots contain a mine, the game is over.
     *
     * @param x The x-coordinate of the slot to guess.
     * @param y The y-coordinate of the slot to guess.
     * @return true if the guess was successful (no mine hit), false if a mine was hit.
     **/
    public boolean guess(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            throw new IndexOutOfBoundsException("Invalid slot coordinates");
        }
        Slot slot = slots[y][x];
        if (slot.isRevealed()) {
            int i = slot.getAdjacentMines();
            int flaggedCount = 0;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0) continue;
                    int nx = x + dy;
                    int ny = y + dx;
                    if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                        if (slots[ny][nx].isFlagged()) {
                            flaggedCount++;
                        }
                    }
                }
            }
            if (flaggedCount == i) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        if (dx == 0 && dy == 0) continue;
                        int nx = x + dy;
                        int ny = y + dx;
                        if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                            Slot neighbor = slots[ny][nx];
                            if (!neighbor.isRevealed() && !neighbor.isFlagged()) {
                                if (!this.dig(nx, ny)) {
                                    return false;
                                }
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    /**
     * Toggle the flagged state of the slot at the specified coordinates.
     * If the slot is already revealed, do nothing.<br/>
     * Flag is used for marking suspected mines and making guesses, see also {@link #guess(int, int)}.
     *
     * @param x The x-coordinate of the slot to toggle.
     * @param y The y-coordinate of the slot to toggle.
     **/
    public void toggleFlag(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            throw new IndexOutOfBoundsException("Invalid slot coordinates");
        }
        Slot slot = slots[y][x];
        if (!slot.isRevealed()) {
            if (slot.isFlagged()) {
                slot.setFlagged(false);
                flagged--;
            } else {
                slot.setFlagged(true);
                flagged++;
            }
        }
    }

    /**
     * Add a random mine to the board and update adjacent mine counts.
     *
     * @return true if a mine was added, false if the board is full of mines.
     */
    @SuppressWarnings("UnusedReturnValue")
    public boolean addRandomMine() {
        if (mine >= width * height) {
            return false; // No more space for mines
        }
        // Find a random empty slot
        int x, y;
        do {
            x = RANDOM.nextInt(width);
            y = RANDOM.nextInt(height);
        } while (slots[y][x].isMine());
        slots[y][x].setMine(true);
        // Update adjacent mine counts
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                int nx = x + dy;
                int ny = y + dx;
                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                    slots[ny][nx].increaseAdjacentMines();
                }
            }
        }
        mine++;
        return true;
    }

    /**
     * Add multiple random mines to the board.
     *
     * @param count The number of mines to add.
     * @return The Board instance for method chaining.
     */
    public Board addRandomMines(int count) {
        for (int i = 0; i < count; i++) {
            this.addRandomMine();
        }
        return this;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height + 1; // Extra line for mine count display
    }

    @Override
    public boolean onKeyPressed(int x, int y, int keyCode) {
        if (this.gameOver) {
            return false;
        }
        if (keyCode == 'f') {
            this.toggleFlag(x, y);
            return true;
        } else if (keyCode == 'g') {
            this.gameOver = !this.guess(x, y);
            return true;
        } else if (keyCode == 'd') {
            this.gameOver = !this.dig(x, y);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void render(Widget<?> parent, TextScreen screen, int offsetX, int offsetY) {
        int remaining = width * height - mine - revealed;
        if (remaining <= 0) {
            this.gameOver = true;
        }
        if (this.gameOver) {
            screen.write(remaining == 0 ? "You Win!" : "Game Over!", offsetX, offsetY + this.getHeight());
        } else {
            screen.write("Flags: %d/%d".formatted(this.flagged, this.mine), offsetX, offsetY + this.getHeight());
        }
    }
}
