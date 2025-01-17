package rushhour.model;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;

import rushhour.view.RushHourObserver;

public class RushHour {
    public int BOARD_DIM = 5;
    public char RED_SYMBOL = 'R';
    public char EMPTY_SYMBOL = '-';
    public int MOVE_COUNT = 0;
    char[][] board; // create a 2d array for the game board
    public Position EXIT_POS = new Position(2, 5);

    private Collection<RushHourObserver> observers = new ArrayList<>();

    public void registerObserver(RushHourObserver observer) {
        observers.add(observer);
    }

    public void notifyObservers(Vehicle vehicle) {
        for (RushHourObserver observer : observers) {
            observer.updateMove(vehicle);
        }
    }

    public RushHour(String filename) {
        board = new char[BOARD_DIM][BOARD_DIM];
        fillboard(filename);
    }

    public char[][] getBoard() {
        return board.clone(); // return a copy to prevent modifications
    }

    public void fillboard(String filename) {
        String csvFile = "data/" + filename;
        String line;
        String splitByCommas = ","; // separate values by commas

        try {
            BufferedReader br = new BufferedReader(new FileReader(csvFile));

            int rows = 0;
            int cols = 0;
            // find the number of rows and columns in the csv file
            while ((line = br.readLine()) != null) {
                String[] values = line.split(splitByCommas);
                rows++;
                cols = Math.max(cols, values.length);
            }

            // reset the file reader to read from the beginning of the file
            br.close();
            br = new BufferedReader(new FileReader(csvFile));

            // create the 2d array with not dynamic dimensions
            board = new char[BOARD_DIM][BOARD_DIM];

            List<Vehicle> vehicles = new ArrayList<>();

            while ((line = br.readLine()) != null) {
                String[] values = line.split(splitByCommas);
                Integer[] coords = new Integer[4];
                for (int i = 1; i < values.length; i++) {
                    coords[i - 1] = Integer.valueOf(values[i]);
                }

                Vehicle car = new Vehicle(values[0].charAt(0), new Position(coords[0], coords[1]),
                        new Position(coords[2], coords[3]));
                vehicles.add(car);
            }

            for (int row = 0; row < BOARD_DIM; row++) {
                for (int col = 0; col < BOARD_DIM; col++) {
                    for (Vehicle car : vehicles) {

                        if (car.getFront().getCol() == col) {
                            board[row][col] = car.getSymbol();
                        }
                        if (car.getFront().getRow() == col) {
                            board[row][col] = car.getSymbol();
                        }
                    }
                }
            }

            br.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // find where the chosen car is
    public Position findVehiclePosition(char chosenVehicle) {
        int rows = board.length;
        int cols = board[0].length;

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (board[i][j] == chosenVehicle) {
                    return new Position(i, j);
                }
            }
        }
        return null;
    }

    public void printBoard() {
        for (char[] rowArray : this.board) {
            for (char value : rowArray) {
                System.out.print(value + " ");
            }
            System.out.println();
        }
    }

    private void undoMove(Move move) {
        char symbol = move.getSymbol();
        Direction direction = move.getDirection();

        try {
            // Find where the car symbol from the move is on the board
            Position vehiclePos = findVehiclePosition(symbol);

            if (vehiclePos != null) {
                int row = vehiclePos.getRow();
                int col = vehiclePos.getCol();

                if (direction == Direction.LEFT) {
                    // Undo the move to the left
                    board[row][col] = EMPTY_SYMBOL;
                    board[row][col + 1] = symbol;
                } else if (direction == Direction.RIGHT) {
                    // Undo the move to the right
                    board[row][col] = symbol;
                    board[row][col + 2] = EMPTY_SYMBOL;
                } else if (direction == Direction.UP) {
                    // Undo the move upwards
                    board[row][col] = EMPTY_SYMBOL;
                    board[row + 1][col] = symbol;
                } else if (direction == Direction.DOWN) {
                    // Undo the move downwards
                    board[row + 2][col] = EMPTY_SYMBOL;
                    board[row][col] = symbol;
                }

                MOVE_COUNT--;
            } else {
                throw new RushHourException("Invalid move. The specified vehicle symbol was not found on the board.");
            }

            // Notify observers about the undo move
            Position frontPos;
            if (direction == Direction.LEFT || direction == Direction.RIGHT) {
                frontPos = new Position(vehiclePos.getRow(), vehiclePos.getCol() + 2);
            } else {
                frontPos = new Position(vehiclePos.getRow() + 2, vehiclePos.getCol());
            }
            notifyObservers(new Vehicle(symbol, vehiclePos, frontPos));
        } catch (RushHourException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public boolean solve() {
        // Create a copy of the current board
        char[][] originalBoard = getBoard();

        // Perform backtracking to find a solution
        boolean success = backtrack();

        // Restore the original board
        board = originalBoard;

        return success;
    }

    private boolean backtrack() {
        if (isGameOver()) {
            return true; // Solution found
        }

        // Iterate through possible moves
        Collection<Move> possibleMoves = getPossibleMoves();
        for (Move move : possibleMoves) {
            // Try making the move
            moveVehicle(move);

            // Recursively explore the next state
            if (backtrack()) {
                return true; // Solution found
            }

            // Undo the move if it doesn't lead to a solution
            undoMove(move);
        }

        return false; // No solution found
    }

    // Maybe make a helper function to find out the orientation and direction of the
    // move
    public void moveVehicle(Move move) {
        char symbol = move.getSymbol();
        Direction direction = move.getDirection();

        try {
            // 1) find where the car symbol from the move is on the board
            Position vehiclePos = findVehiclePosition(symbol);
            // 2) find out the orientation and direction of the move
            boolean isHorizontal = (direction == Direction.LEFT || direction == Direction.RIGHT);
            boolean isVertical = (direction == Direction.UP || direction == Direction.DOWN);
            // 3) check if the space next to the car is occupied
            if (vehiclePos != null) {
                int row = vehiclePos.getRow();
                int col = vehiclePos.getCol();

                if (isHorizontal) {
                    if (direction == Direction.LEFT) {
                        if (col - 1 >= 0 && board[row][col - 1] == EMPTY_SYMBOL) {
                            board[row][col - 1] = symbol;
                            board[row][col + 1] = EMPTY_SYMBOL;
                        } else {
                            throw new RushHourException("Invalid move. The space next to the vehicle is occupied.");
                        }
                    } else {
                        if (col + 2 < BOARD_DIM && board[row][col + 2] == EMPTY_SYMBOL) {
                            board[row][col + 2] = symbol;
                            board[row][col] = EMPTY_SYMBOL;
                        } else {
                            throw new RushHourException("Invalid move. The space next to the vehicle is occupied.");
                        }
                    }
                } else if (isVertical) {
                    if (direction == Direction.UP) {
                        if (row - 1 >= 0 && board[row - 1][col] == EMPTY_SYMBOL) {
                            board[row - 1][col] = symbol;
                            board[row + 1][col] = EMPTY_SYMBOL;
                        } else {
                            throw new RushHourException("Invalid move. The space next to the vehicle is occupied.");
                        }
                    } else {
                        if (row + 2 < BOARD_DIM && board[row + 2][col] == EMPTY_SYMBOL) {
                            // Move the vehicle down
                            board[row + 2][col] = symbol;
                            board[row][col] = EMPTY_SYMBOL;
                        } else {
                            throw new RushHourException("Invalid move. The space next to the vehicle is occupied.");
                        }
                    }
                }
            } else {
                throw new RushHourException("Invalid move. The specified vehicle symbol was not found on the board.");
            }

            // Notify observers about the move
            Position frontPos;
            if (isHorizontal) {
                frontPos = new Position(vehiclePos.getRow(), vehiclePos.getCol() + 2);
            } else {
                frontPos = new Position(vehiclePos.getRow() + 2, vehiclePos.getCol());
            }
            notifyObservers(new Vehicle(symbol, vehiclePos, frontPos));

            MOVE_COUNT++;
        } catch (RushHourException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public boolean isGameOver() {
        // check if the board has at least 3 rows and 6 columns
        if (board.length >= 3 && board[0].length >= 6) {
            if (board[2][5] == RED_SYMBOL) {
                notifyObservers(null);
                return true;
            }
        }
        return false;
    }

    public Collection<Move> getPossibleMoves() {
        Collection<Move> possibleMoves = new HashSet<>();

        // for (Vehicle vehicle : ) {
        // for (Direction direction : Direction.values()) {
        // Move move = new Move(vehicle.getSymbol(), direction);

        // try {
        // vehicle.move(direction);
        // possibleMoves.add(move);
        // }
        // catch (RushHourException ignored) {
        // // Move is not valid, so ignore and continue checking other directions.
        // }
        // }
        // }

        return possibleMoves;
    }

    public int getMoveCount() {
        return MOVE_COUNT;
    }

    public void resetBoard() {
        int rows = board.length;
        int cols = board[0].length;

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                board[i][j] = EMPTY_SYMBOL;
            }
        }
    }

    public void parseCommand(RushHour rushHour, String command) throws RushHourException {
        // check for help
        if (command.equals("help") || command.equals("Help")) {
            System.out.println("Help Menu:\n" + //
                    "        help - this menu\n" + //
                    "        quit - quit\n" + //
                    "        hint - display a valid move\n" + //
                    "        reset - reset the game\n" + //
                    "        <symbol> <UP|DOWN|LEFT|RIGHT> - move the vehicle one space in the given direction");
            // check for move
        } else if (command.equals("move") || command.equals("Move")) {
            try (Scanner scanner = new Scanner(System.in)) {
                System.out.print("Enter symbol and direction (e.g., A UP): ");
                String input = scanner.nextLine();

                String[] parts = input.split(" ");
                if (parts.length == 2) {
                    char symbol = parts[0].charAt(0);
                    Direction direction = Direction.valueOf(parts[1].toUpperCase());

                    Move move = new Move(symbol, direction);
                    rushHour.moveVehicle(move);
                    // System.out.println("Symbol: " + move.getSymbol());
                    // System.out.println("Direction: " + move.getDirection());
                } else {
                    System.out.println("Invalid input format. Please use the format 'Symbol Direction'!");
                }
            }
            // check for solve
        } else if (command.equals("solve") || command.equals("Solve")) {
            if (rushHour.solve()) {
                System.out.println("Solution found!");
                rushHour.printBoard();
            } else {
                System.out.println("No solution found.");
            }
            // check for reset
        } else if (command.equals("reset") || command.equals("Reset")) {
            System.out.println("Clearing board...");
            System.out.println("New Game");
            rushHour.resetBoard();
        }
    }

    public static void main(String[] args) throws RushHourException {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Enter a Rush Hour filename: ");
            String filename = scanner.nextLine();
            RushHour rushHour = new RushHour(filename);
            System.out.println("Type 'help' for the help menu.");
            rushHour.printBoard();

            while (!rushHour.isGameOver()) {
                System.out.print("> ");
                String command = scanner.nextLine();
                String resultString = command.replace(">", "");
                System.out.println(resultString);

                if (resultString.equals("quit") || resultString.equals("Quit")) {
                    System.out.println("Quitting. Have a nice day!");
                    return;
                }

                rushHour.parseCommand(rushHour, resultString);
            }
        }
    }
}