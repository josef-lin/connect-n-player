package com.thg.accelerator25.connectn.ai.onleave;

import com.thehutgroup.accelerator.connectn.player.Board;
import com.thehutgroup.accelerator.connectn.player.Counter;
import com.thehutgroup.accelerator.connectn.player.InvalidMoveException;
import com.thehutgroup.accelerator.connectn.player.Player;

import java.util.*;

public class OnLeaveSlowResponse extends Player {

    // timeout value, gives 500ms buffer
    private static final long TIMEOUT_SECONDS = 9500;
    private long startTime;

    private final Map<Long, Integer> boardHashMap = new HashMap<>();

    public OnLeaveSlowResponse(Counter counter) {
        //TODO: fill in your name here
        super(counter, OnLeaveSlowResponse.class.getName());
    }

    @Override
    public int makeMove(Board board) {
        //TODO: some crazy analysis
        //TODO: make sure said analysis uses less than 2G of heap and returns within 10 seconds on whichever machine is running it
        startTime = System.currentTimeMillis();
        List<Integer> legalMoves = getLegalMoves(board);
        int bestMoveFound = legalMoves.get(0);
        int bestScoreFound = Integer.MIN_VALUE;
        int bestDepthFound = 0;
        // for a given depth, first value is the best move, second value is the best score
        List<Integer> depthBest;

        for (int depth = 1; !isTimeUp() && depth < 80; depth++) {
            try {
                System.out.println("depth: " + depth);
                depthBest = searchMovesAtDepth(board, depth, legalMoves);
                System.out.println("depthBest: " + depthBest);
//                if (depthBest.get(1) > bestScoreFound) {
//                    bestMoveFound = depthBest.get(0);
//                    bestScoreFound = depthBest.get(1);
//                    bestDepthFound = depth;
//                }
                bestMoveFound = depthBest.get(0);
                bestScoreFound = depthBest.get(1);
            } catch (OnLeaveSlowResponse.TimeoutException e) {
                System.out.println("Timeout, use previous best result");
                break;
            }
        }
        System.out.println("Best move found: " + bestMoveFound);
        System.out.println("Best score found: " + bestScoreFound);
        System.out.println("Best depth found: " + bestDepthFound);
        System.out.println("Cache Size: " + boardHashMap.size());
        return bestMoveFound;
    }

    private long hashCode(Board board) {
        Counter[][] counterPlacement = board.getCounterPlacements();
        long result = 1;
        for (Counter[] counters : counterPlacement) {
            result = 31 * result + Arrays.hashCode(counters);
        }
        return result;
    }

    private void clearCache() {
        boardHashMap.clear();
    }

    private List<Integer> searchMovesAtDepth(Board board, int depth, List<Integer> legalMoves) throws OnLeaveSlowResponse.TimeoutException {
        int depthBestScore = Integer.MIN_VALUE;
        int depthBestMove = legalMoves.get(0);
//        System.out.println("Searching for moves at depth " + depth);
        for (int move : legalMoves) {
            int score;
            if (isTimeUp()) {
                throw new OnLeaveSlowResponse.TimeoutException();
            }
            try {
                Board newBoard = new Board(board, move, getCounter());
                score = minimax(newBoard, depth - 1, false, Integer.MIN_VALUE, Integer.MAX_VALUE, getCounter().getOther(), depth - 1);
            } catch (InvalidMoveException e) {
                break;
            }

            if (score > depthBestScore) {
                depthBestScore = score;
                depthBestMove = move;
            }
        }
        clearCache();
        return Arrays.asList(depthBestMove, depthBestScore);
    }

    private boolean isTimeUp() {
        return System.currentTimeMillis() - startTime > TIMEOUT_SECONDS;
    }


    private static class TimeoutException extends Exception {
    }

    public List<Integer> getLegalMoves(Board board) {
        List<Integer> legalMoves = new ArrayList<>();
        Counter[][] counterPlacements = board.getCounterPlacements();
        for (int col = 0; col < board.getConfig().getWidth(); col++) {
            if (counterPlacements[col][board.getConfig().getHeight() - 1] == null) {
                legalMoves.add(col);
            }
        }
        return legalMoves;
    }

    private int minimax(Board board, int depth, boolean isMaximizing,
                        int alpha, int beta, Counter currentCounter, int initialDepth) throws OnLeaveSlowResponse.TimeoutException, InvalidMoveException {
        if (isTimeUp()) {
            throw new OnLeaveSlowResponse.TimeoutException();
        }
        if (depth == 0 || isGameOver(board)) {
            return evaluatePosition(board, initialDepth);
        }

        long boardHash = hashCode(board);
        if (boardHashMap.containsKey(boardHash)) {
            return boardHashMap.get(boardHash);
        }

        List<Integer> moves = getLegalMoves(board);
        int bestScore = isMaximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;

        for (int move : moves) {
            Board newBoard = new Board(board, move, currentCounter);
            int score = minimax(newBoard, depth - 1, !isMaximizing, alpha, beta, currentCounter.getOther(), initialDepth);

            if (isMaximizing) {
                bestScore = Math.max(score, bestScore);
                alpha = Math.max(alpha, score);
            } else {
                bestScore = Math.min(score, bestScore);
                beta = Math.min(beta, score);
            }
            if (beta <= alpha) {
                break;
            }
        }

        // Store the result in hash map
        boardHashMap.put(boardHash, bestScore);

        return bestScore;
    }

    private int evaluatePosition(Board board, int initialDepth) {
        Counter[][] counterPlacements = board.getCounterPlacements();
        Counter counter = getCounter();
        int score = 0;
        for (int col = 0; col < counterPlacements.length; col++) {
            for (int row = 0; row < counterPlacements[col].length; row++) {

                // Immediate wins/losses, should choose option that wins in fastest way
                if (hasFourInARow(counterPlacements, row, col, counter)) {
                    return Integer.MAX_VALUE - initialDepth;
                } else if (hasFourInARow(counterPlacements, row, col, counter.getOther())) {
                    return Integer.MIN_VALUE + initialDepth;
                }

                // some weight based on depth
                score -= 5 * initialDepth;

                List<Integer> playerInARow = inARow(counterPlacements, row, col, counter);
                List<Integer> oppInARow = inARow(counterPlacements, row, col, counter.getOther());

                score += 100 * playerInARow.get(0);
                score += 10 * playerInARow.get(1);
                score -= 100 * oppInARow.get(0);
                score -= 10 * oppInARow.get(1);

            }
        }
        return score;
    }

    private boolean isGameOver(Board board) {
        Counter[][] counterPlacements = board.getCounterPlacements();
        if (isBoardFull(counterPlacements)) {
            return true;
        }
        if (hasFourInARow(counterPlacements, Counter.O) || (hasFourInARow(counterPlacements, Counter.X))) {
            return true;
        }

        return false;
    }

    private boolean isBoardFull(Counter[][] counterPlacements) {
        // returns false if any space is empty, otherwise return true
        for (Counter[] counterPlacement : counterPlacements) {
            for (Counter counter : counterPlacement) {
                if (counter == null) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean hasFourInARow(Counter[][] counterPlacements, Counter counter) {
        for (int col = 0; col < counterPlacements.length; col++) {
            for (int row = 0; row < counterPlacements[col].length; row++) {
                if (hasFourInARow(counterPlacements, row, col, counter)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasFourInARow(Counter[][] counterPlacements, int row, int col, Counter counter) {
        if (counter == counterPlacements[col][row]) {
            // check horizontal right
            if (col + 3 < counterPlacements.length &&
                    counter == counterPlacements[col + 1][row] &&
                    counter == counterPlacements[col + 2][row] &&
                    counter == counterPlacements[col + 3][row]) {
                return true;
            }
            // check vertical up
            if (row + 3 < counterPlacements[col].length &&
                    counter == counterPlacements[col][row + 1] &&
                    counter == counterPlacements[col][row + 2] &&
                    counter == counterPlacements[col][row + 3]) {
                return true;
            }
            // check diagonal (up, right)
            if (col + 3 < counterPlacements.length && row + 3 < counterPlacements[col].length &&
                    counter == counterPlacements[col + 1][row + 1] &&
                    counter == counterPlacements[col + 2][row + 2] &&
                    counter == counterPlacements[col + 3][row + 3]) {
                return true;
            }
            // check diagonal (down, right)
            if (col + 3 < counterPlacements.length && row - 3 >= 0 &&
                    counter == counterPlacements[col + 1][row - 1] &&
                    counter == counterPlacements[col + 2][row - 2] &&
                    counter == counterPlacements[col + 3][row - 3]) {
                return true;
            }
        }
        return false;
    }

    private List<Integer> inARow(Counter[][] counterPlacements, int row, int col, Counter counter) {
        int countThrees = 0;
        int countTwos = 0;

        // check horizontal
        if (col + 3 < counterPlacements.length) {
            int filled = 0;
            int empty = 0;
            for (int i = 0; i < 4; i++) {
                if (counterPlacements[col+i][row] == counter) {
                    filled += 1;
                } else if (counterPlacements[col+i][row] == null) {
                    empty += 1;
                } else {
                    break;
                }
            }
            if (filled + empty == 4 && empty == 1) {
                countThrees += 1;
            } else if (filled + empty == 4 && empty == 2) {
                countTwos += 1;
            }
        }

        // check vertical
        if (row + 3 < counterPlacements[col].length) {
            int filled = 0;
            for (int i = 0; i < 3; i++) {
                if (counterPlacements[col][row+i] == counter) {
                    filled += 1;
                } else {
                    break;
                }
            }
            if (filled == 3) {
                countThrees += 1;
            } else if (filled == 2) {
                countTwos += 1;
            }
        }

        // check diagonal up right
        if (col + 3 < counterPlacements.length && row + 3 < counterPlacements[col].length) {
            int filled = 0;
            int empty = 0;
            for (int i = 0; i < 4; i++){
                if (counterPlacements[col+i][row+i] == counter) {
                    filled += 1;
                } else if (counterPlacements[col+i][row+i] == null) {
                    empty += 1;
                } else {
                    break;
                }
                if (filled + empty == 4 && empty == 1) {
                    countThrees += 1;
                } else if (filled + empty == 4 && empty == 2) {
                    countTwos += 1;
                }
            }
        }

        // check diagonal up left
        if (col + 3 < counterPlacements.length && row - 3 > 0) {
            int filled = 0;
            int empty = 0;
            for (int i = 0; i < 4; i++) {
                if (counterPlacements[col+i][row-i] == counter) {
                    filled += 1;
                } else if (counterPlacements[col+i][row-i] == null) {
                    empty += 1;
                } else {
                    break;
                }
                if (filled + empty == 4 && empty == 1) {
                    countThrees += 1;
                } else if (filled + empty == 4 && empty == 2) {
                    countTwos += 1;
                }
            }
        }

        return Arrays.asList(countThrees, countTwos);
    }
}
