package cool.muyucloud.minesweeper.widget;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Optional;
import java.util.TreeSet;

/**
 * The base class for all widgets.
 * A widget can contain other widgets as its children.
 * It provides methods for adding, removing, and rendering child widgets,
 * as well as handling input events such as key presses.
 *
 * @param <W> The type of the widget itself, used for method chaining.
 */
@SuppressWarnings("unused")
public abstract class Widget<W extends Widget<W>> implements Comparable<Widget<?>> {
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

    /**
     * Handle key pressed event at (x, y) with the given key code.
     * This method will first propagate the event to its children,
     * and if none of the children handle the event, it will call {@link #handleKeyPressed(int, int, int)}.
     */
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

    /**
     * Add child widgets to this widget.
     *
     * @param children The child widgets to add.
     * @return This widget instance for method chaining.
     */
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

    /**
     * Remove child widgets from this widget.
     *
     * @param children The child widgets to remove.
     * @return This widget instance for method chaining.
     */
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

    /**
     * Remove all child widgets from this widget.
     *
     * @return This widget instance for method chaining.
     */
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

    /**
     * Refresh this widget in all its parents.
     * This method will remove and re-add this widget in all its parents' children set,
     * which will update the rendering order based on the z-index.
     */
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

    /**
     * Compare this widget with another widget based on their z-index.
     * A widget with a higher z-index will be rendered on top of a widget with a lower z-index.
     * If two widgets have the same z-index, their order is undefined (Based on the order-on-add).
     *
     * @param o The other widget to compare with.
     * @return A negative integer, zero, or a positive integer as this widget is less than, equal to, or greater than the specified widget.
     */
    @Override
    public int compareTo(Widget<?> o) {
        return Integer.compare(this.getZ(), o.getZ());
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
