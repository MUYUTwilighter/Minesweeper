package cool.muyucloud.minesweeper.render.widget;

import org.jetbrains.annotations.NotNull;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicReference;

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

    public TextScreen write(String s, int x, int y) {
        return this.write(s, x, y, this.getWidth(), this.getHeight(), 0);
    }

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
