package cool.muyucloud.minesweeper.game;

import cool.muyucloud.minesweeper.widget.TextScreen;
import cool.muyucloud.minesweeper.widget.Widget;

/**
 * A record-like class representing a single slot on the Minesweeper board.
 * It holds information about whether the slot contains a mine, whether it has been revealed,
 * whether it has been flagged, and the number of adjacent mines.
 */
@SuppressWarnings("UnusedReturnValue")
public class Slot extends Widget<Slot> {
    private boolean isMine;
    private boolean isRevealed;
    private boolean isFlagged;
    private int adjacentMines;
    private int x, y;

    public Slot() {
        this.isMine = false;
        this.isRevealed = false;
        this.isFlagged = false;
        this.adjacentMines = 0;
    }

    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    /**
     * Set the rendering position of this slot.
     *
     * @param x The x-coordinate relative to the parent widget.
     * @param y The y-coordinate relative to the parent widget.
     * @return This Slot instance for method chaining.
     */
    public Slot setPos(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public boolean isMine() {
        return isMine;
    }

    public void setMine(boolean mine) {
        isMine = mine;
    }

    public boolean isRevealed() {
        return isRevealed;
    }

    public void setRevealed(boolean revealed) {
        isRevealed = revealed;
    }

    public boolean isFlagged() {
        return isFlagged;
    }

    public void setFlagged(boolean flagged) {
        isFlagged = flagged;
    }

    public int getAdjacentMines() {
        return adjacentMines;
    }

    public void increaseAdjacentMines() {
        this.adjacentMines++;
    }

    @Override
    public void render(Widget<?> parent, TextScreen screen, int offsetX, int offsetY) {
        if (this.isRevealed) {
            if (this.isMine) screen.set('*', offsetX, offsetY);
            else if (this.adjacentMines > 0) screen.set((char) ('0' + this.adjacentMines), offsetX, offsetY);
            else screen.set(' ', offsetX, offsetY);
        } else {
            if (this.isFlagged) screen.set('$', offsetX, offsetY);
            else screen.set('█', offsetX, offsetY);
        }
    }
}
