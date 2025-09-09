package cool.muyucloud.minesweeper.widget;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe text screen widget that can render text content to an output stream.
 * Supports dynamic resizing, writing text with automatic line wrapping, and filling areas with a specific character.
 * Can run in a separate thread to periodically flush updates to the output stream, see {@link #run(OutputStream)}
 */
@SuppressWarnings("UnusedReturnValue")
public class TextScreen extends Widget<TextScreen> {
    private int width = 1;
    private int height = 1;
    private final AtomicReference<char[][]> content = new AtomicReference<>();
    private boolean dirty = true;
    private int frameTime;
    private long lastRender;
    private Thread thread = null;

    public TextScreen(int width, int height) {
        this.setBound(width, height);
    }

    // noInspection unused
    public TextScreen setBound(int width, int height) {
        synchronized (content) {
            this.width = Math.max(width, 1);
            this.height = Math.max(height, 1);
            content.set(new char[height][width]);
            this.dirty = true;
            this.fill(' ', 0, 0, this.width, this.height);
        }
        return this;
    }

    public TextScreen setFrameTime(int frameTime) {
        this.frameTime = Math.max(frameTime, 0);
        return this;
    }

    /**
     * Write text to the screen at the specified position.
     * Automatically handles new lines, carriage returns, and tabs.
     * Text will wrap to the next line if it exceeds the <b>screen width</b>.
     *
     * @param s The string to write.
     * @param x The x-coordinate to start writing.
     * @param y The y-coordinate to start writing.
     * @return This TextScreen instance for method chaining.
     */
    public TextScreen write(String s, int x, int y) {
        return this.write(s, x, y, this.getWidth(), this.getHeight(), 0);
    }

    /**
     * Write text to the screen at the specified position within a defined border.
     * Automatically handles new lines, carriage returns, and tabs.
     * Text will wrap to the next line if it exceeds the <b>border width</b>.
     *
     * @param s       The string to write.
     * @param x       The x-coordinate to start writing.
     * @param y       The y-coordinate to start writing.
     * @param borderX The width of the writing border.
     * @param borderY The height of the writing border.
     * @param indent  The number of spaces to indent from the starting x-coordinate.
     * @return This TextScreen instance for method chaining.
     */
    @NotNull
    public TextScreen write(String s, int x, int y, int borderX, int borderY, int indent) {
        borderX = Math.min(Math.max(borderX, 1), this.getWidth());
        borderY = Math.min(Math.max(borderY, 1), this.getHeight());
        x = Math.min(Math.max(x, 0), borderX - 1);
        y = Math.min(Math.max(y, 0), borderY - 1);
        int dx = x + indent;
        synchronized (this.content) {
            char[][] content = this.content.get();
            char[] toWrite = s.toCharArray();
            for (int i = 0; i < toWrite.length; i++) {
                char c = toWrite[i];
                // auto new line
                while (dx >= borderX) {
                    dx -= borderX - x;
                    y++;
                }
                if (y >= borderY) return this;
                // handle char
                char old = content[y][dx];
                if (c == '\n') {
                    y++;
                    dx = x;
                } else if (c == '\r') {
                    y++;
                    dx = x;
                    // \r\n
                    if (i + 1 < toWrite.length && toWrite[i + 1] == '\n') {
                        dx++;
                    }
                } else if (c == '\t') {
                    this.write("    ", x, y, borderX, borderY, dx - x);
                } else if (isFullWidth(c)) {
                    if (x == borderX - 1) { // only 1-char space
                        content[y][dx] = '?';
                        if (old != content[y][dx]) this.dirty = true;
                        dx++;
                    } else if (dx < borderX - 1) {  // enough space
                        content[y][dx] = c;
                        if (old != content[y][dx]) this.dirty = true;
                        old = content[y][dx + 1];
                        content[y][dx + 1] = '\0';
                        if (old != content[y][dx + 1]) this.dirty = true;
                        dx += 2;
                    } else {    // not enough space for current line
                        y++;
                        dx = x;
                        if (y >= borderY) return this;
                        old = content[y][dx];
                        content[y][dx] = c;
                        if (old != content[y][dx]) this.dirty = true;
                        old = content[y][dx + 1];
                        content[y][dx + 1] = '\0';
                        if (old != content[y][dx + 1]) this.dirty = true;
                        dx += 2;
                    }
                } else if (c != '\0' && old != '\0') {
                    content[y][dx] = c;
                    if (old != content[y][dx]) this.dirty = true;
                    dx++;
                }
            }
            this.content.set(content);
        }
        return this;
    }

    /**
     * Set a character at the specified position on the screen.
     * If the character is full-width, the next position will be set to '\0' to indicate it's part of a full-width character.
     * Ignores newline, carriage return, tab, and null characters.
     *
     * @param c The character to set.
     * @param x The x-coordinate to set the character.
     * @param y The y-coordinate to set the character.
     * @return This TextScreen instance for method chaining.
     */
    public TextScreen set(char c, int x, int y) {
        x = Math.min(Math.max(x, 0), this.getWidth() - 1);
        y = Math.min(Math.max(y, 0), this.getHeight() - 1);
        if (c == '\n' || c == '\r' || c == '\t' || c == '\0') return this;
        synchronized (this.content) {
            char[][] content = this.content.get();
            char old = content[y][x];
            if (old == '\0') return this;
            content[y][x] = c;
            if (old != c) this.dirty = true;
            if (isFullWidth(c) && (x + 1) < this.getWidth()) {
                old = content[y][x + 1];
                content[y][x + 1] = '\0';
                if (old != content[y][x + 1]) this.dirty = true;
            }
            this.content.set(content);
        }
        return this;
    }

    /**
     * Fill a rectangular area on the screen with a specific character.
     * If the character is full-width, it will handle the next position accordingly.
     * Ignores newline, carriage return, tab, and null characters.
     *
     * @param c      The character to fill with.
     * @param x      The x-coordinate of the top-left corner of the area to fill.
     * @param y      The y-coordinate of the top-left corner of the area to fill.
     * @param width  The width of the area to fill.
     * @param height The height of the area to fill.
     * @return This TextScreen instance for method chaining.
     */
    public TextScreen fill(char c, int x, int y, int width, int height) {
        x = Math.min(Math.max(x, 0), this.getWidth() - 1);
        y = Math.min(Math.max(y, 0), this.getHeight() - 1);
        int borderX = Math.min(Math.max(width, 1) + x, this.getWidth());
        int borderY = Math.min(Math.max(height, 1) + y, this.getWidth());
        if (c == '\n' || c == '\r' || c == '\t' || c == '\0') return this;
        synchronized (this.content) {
            char[][] content = this.content.get();
            for (int dy = y; dy < borderY; dy++) {
                for (int dx = x; dx < borderX; dx++) {
                    if (isFullWidth(c)) {
                        char old = content[dy][dx];
                        if (borderX - dx == 1) {
                            content[dy][dx] = '?';
                            if (old != content[dy][dx]) this.dirty = true;
                        } else {
                            content[dy][dx] = c;
                            if (old != content[dy][dx]) this.dirty = true;
                            old = content[dy][dx + 1];
                            content[dy][dx + 1] = '\0';
                            if (old != content[dy][dx + 1]) this.dirty = true;
                            dx++;
                        }
                    } else {
                        char old = content[dy][dx];
                        content[dy][dx] = c;
                        if (old != content[dy][dx]) this.dirty = true;
                    }
                }
            }
            this.content.set(content);
        }
        return this;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getWidth() {
        return width;
    }

    /**
     * Flush the current content of the screen to the given output stream.
     * Only non-null characters will be written, and each line will be terminated with a newline character.
     *
     * @param stream The output stream to flush the content to.
     * @return This TextScreen instance for method chaining.
     * @throws IOException If an I/O error occurs.
     */
    public TextScreen flush(OutputStream stream) throws IOException {
        synchronized (this.content) {
            for (char[] chars : content.get()) {
                for (char c : chars) {
                    if (c != '\0') {
                        stream.write(String.valueOf(c).getBytes());
                    }
                }
                stream.write('\n');
            }
            stream.flush();
        }
        return this;
    }

    public boolean isRunning() {
        return thread != null;
    }

    /**
     * Run the screen in a separate thread, periodically flushing updates to the given output stream.
     * The screen will render its content and flush to the stream at intervals defined by the frame time.
     * If the screen is already running, an IllegalStateException will be thrown.
     *
     * @param stream The output stream to flush the content to.
     * @throws IllegalStateException If the screen is already running.
     * @throws RuntimeException      If an I/O error occurs during rendering or flushing.
     * @see #setFrameTime(int)
     * @see #shut()
     **/
    @SuppressWarnings("BusyWait")
    public void run(OutputStream stream) {
        if (this.isRunning()) throw new IllegalStateException("Screen is already running");
        this.thread = new Thread(() -> {
            this.lastRender = System.currentTimeMillis();
            try {
                while (this.isRunning()) {
                    try {
                        this.renderInternal(this);
                        if (this.dirty) {
                            this.dirty = false;
                            this.flush(stream);
                        }
                        long now = System.currentTimeMillis();
                        long lastRender = this.lastRender;
                        this.lastRender = now;
                        Thread.sleep(Math.max(0, lastRender + this.frameTime - now));
                    } catch (InterruptedException ignored) {
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        this.thread.start();
    }

    public void shut() {
        this.thread = null;
    }

    public static boolean isFullWidth(char c) {
        return (c >= 'ᄀ' && c <= 'ᅟ') || // Hangul Jamo init. consonants
            (c >= '⺀' && c <= '\uA4CF') || // CJK, Yi, radicals
            (c >= '가' && c <= '힣') || // Hangul syllables
            (c >= '豈' && c <= '\uFAFF') || // CJK Compatibility Ideographs
            (c >= '︐' && c <= '︙') || // Vertical forms
            (c >= '︰' && c <= '\uFE6F') || // CJK Compatibility Forms
            (c >= '\uFF00' && c <= '｠') || // Fullwidth Forms
            (c >= '￠' && c <= '￦');   // Fullwidth symbol variants
    }
}
