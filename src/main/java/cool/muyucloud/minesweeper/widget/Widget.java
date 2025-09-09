package cool.muyucloud.minesweeper.render.widget;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Optional;
import java.util.TreeSet;

public abstract class Widget<W extends Widget<W>> implements Comparable<Widget<W>> {
    @NotNull
    private final HashSet<Widget<?>> parents = new HashSet<>();
    @NotNull
    private final TreeSet<Widget<?>> children = new TreeSet<>();

    @SuppressWarnings("unchecked")
    public <T extends Widget<T>> T adapt() {
        return (T) this;
    }

    public int getX() {
        return 0;
    }

    public int getY() {
        return 0;
    }

    public int getZ() {
        return 0;
    }

    public int getWidth() {
        return 0;
    }

    public int getHeight() {
        return 0;
    }

    public Optional<Widget<?>> getChildAt(int x, int y) {
        synchronized (this.children) {
            for (Widget<?> child : this.children.reversed()) {
                if (child.getX() <= x && x <= child.getX() + child.getWidth() && child.getY() <= y && y <= child.getY() + child.getHeight()) {
                    return Optional.of(child);
                }
            }
        }
        return Optional.empty();
    }

    @ApiStatus.NonExtendable
    public boolean onKeyPressed(int x, int y, int keyCode) {
        synchronized (this.children) {
            for (Widget<?> child : this.children.reversed()) {
                if (child.getX() <= x && x <= child.getX() + child.getWidth() && child.getY() <= y && y <= child.getY() + child.getHeight()) {
                    if (child.onKeyPressed(x, y, keyCode)) {
                        return true;
                    }
                }
            }
        }
        return this.handleKeyPressed(x, y, keyCode);
    }

    /**
     * Handle key pressed event at (x, y) with the given key code.
     *
     * @param x       The x pos relative to this widget.
     * @param y       The y pos relative to this widget.
     * @param keyCode The key code of the pressed key.
     * @return true if the event is handled, and other sibling widgets contains the same location will be ignored.
     *
     */
    public boolean handleKeyPressed(int x, int y, int keyCode) {
        return false;
    }

    @ApiStatus.NonExtendable
    public boolean onKeyReleased(int x, int y, int keyCode) {
        synchronized (this.children) {
            for (Widget<?> child : this.children.reversed()) {
                if (child.getX() <= x && x <= child.getX() + child.getWidth() && child.getY() <= y && y <= child.getY() + child.getHeight()) {
                    if (child.onKeyReleased(x, y, keyCode)) {
                        return true;
                    }
                }
            }
        }
        return this.handleKeyPressed(x, y, keyCode);
    }

    public boolean handleKeyReleased(int x, int y, int keyCode) {
        return false;
    }

    public W add(@NotNull Widget<?>... children) {
        synchronized (this.children) {
            for (Widget<?> child : children) {
                this.children.add(child);
                synchronized (child.parents) {
                    child.parents.add(this);
                }
            }
        }
        return this.adapt();
    }

    public W remove(@NotNull Widget<?>... children) {
        synchronized (this.children) {
            for (Widget<?> child : children) {
                this.children.remove(child);
                synchronized (child.parents) {
                    child.parents.remove(this);
                }
            }
        }
        return this.adapt();
    }

    public W clear() {
        synchronized (this.children) {
            for (Widget<?> child : this.children) {
                synchronized (child.parents) {
                    child.parents.remove(this);
                }
            }
            this.children.clear();
        }
        return this.adapt();
    }

    public void refresh() {
        synchronized (this.parents) {
            this.parents.forEach(parent -> {
                synchronized (parent.children) {
                    parent.children.remove(this);
                    parent.children.add(this);
                }
            });
        }
    }

    @Override
    public int compareTo(Widget o) {
        int z = Integer.compare(this.getZ(), o.getZ());
        int y = Integer.compare(this.getY(), o.getY());
        int x = Integer.compare(this.getX(), o.getX());
        return z == 0 ? (y == 0 ? x : y) : z;
    }

    /**
     * Render this widget and its children to the given screen.
     *
     * @param screen The screen to render to.
     *
     */
    @ApiStatus.NonExtendable
    protected void renderInternal(TextScreen screen) {
        this.renderInternal(screen, screen, 0, 0);
    }

    /**
     * Render this widget and its children to the given screen with the given offset.
     *
     * @param parent  The parent widget of this widget.
     * @param screen  The screen to render to.
     * @param offsetX The x offset from the parent widget to the screen origin.
     * @param offsetY The y offset from the parent widget to the screen origin.
     *
     */
    private void renderInternal(Widget<?> parent, TextScreen screen, int offsetX, int offsetY) {
        this.render(parent, screen, offsetX + this.getX(), offsetY + this.getY());
        this.children.forEach(widget -> widget.renderInternal(this, screen, offsetX + this.getX(), offsetY + this.getY()));
    }

    /**
     * Render current widget to the given screen with the given offset.
     *
     * @param parent  The parent widget of this widget.
     * @param screen  The screen to render to.
     * @param offsetX The x offset from this widget to the screen origin.
     * @param offsetY The y offset from this widget to the screen origin.
     *
     */
    public void render(Widget<?> parent, TextScreen screen, int offsetX, int offsetY) {
    }
}
