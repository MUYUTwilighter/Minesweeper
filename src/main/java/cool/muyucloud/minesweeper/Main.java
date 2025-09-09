package cool.muyucloud.minesweeper;

import cool.muyucloud.minesweeper.game.Board;
import cool.muyucloud.minesweeper.widget.TextScreen;

import java.util.Scanner;

public class Main {
    @SuppressWarnings("BusyWait")
    public static void main(String[] args) throws InterruptedException {
        Board board = new Board(10, 10);
        TextScreen screen = new TextScreen(20, 11).add(board.addRandomMines(10)).setFrameTime(100);
        screen.run(System.out);
        Scanner scanner = new Scanner(System.in);
        while (!board.isGameOver()) {
            Thread.sleep(200);
            System.out.print(">> ");
            String s = scanner.nextLine();
            String[] parts = s.trim().split(" ");
            try {
                String command = parts[0];
                int x = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);
                board.onKeyPressed(x, y, command.charAt(0));
            } catch (NumberFormatException e) {
                System.out.println("Invalid pos");
            } catch (ArrayIndexOutOfBoundsException e) {
                System.out.println("Invalid command");
            } catch (Throwable ignored) {
            }
        }
        Thread.sleep(500);
        screen.shut();
    }
}