package SkyNet.model;

import SkyNet.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Level {

    public class Position {
        public final int x, y;
        public Position(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    public boolean walls[][];
    public ArrayList<Goal> goals;
    public ArrayList<Box> boxes;
    public ArrayList<Agent> agents;

    public HashMap<Integer, Box> boxMap = new HashMap<>();
    public HashMap<Integer, Goal> goalMap = new HashMap<>();
    public HashMap<Character, ArrayList<Box>> matchingBoxes = new HashMap<>();

    public HashMap<Integer, Goal> solvedGoals = new HashMap<>();
    public ArrayList<Goal> unsolvedGoals = new ArrayList<>();

    public Map<Position, Boolean> freeCellMap = new HashMap<>();
    public int[][] activeCells;

//    public HashMap<Integer, Goal> unsolvedGoals = new HashMap<>();

    public int width;
    public int height;

    public Level() {
    }

    //Used for mapping "unused" cells
    public void createFreeCellMap() {
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                freeCellMap.put(new Position(i, j), Boolean.TRUE);
            }
        }
    }

    public void updateFreeCellMap(PartialPlan plan) {
        for (Node n : plan.plan) {

        }
    }

    //Used for mapping box ids to boxes
    public void createBoxMap() {
        for (Box box : boxes) {
            boxMap.put(box.id, box);
        }
    }

    //Used for mapping goal ids to boxes
    public void createGoalMap() {
        for (Goal goal : goals) {
            goalMap.put(goal.id, goal);
        }
    }

    //Create list of unsolved goals
    public void createUnsolvedList() {
        for (Goal goal: goals) {
            unsolvedGoals.add(goal);
        }
    }

    //Add box to list of matching boxes
    public void createMatchingBoxesForGoal() {
        for (Box box : boxes) {
            ArrayList<Box> list = matchingBoxes.get(box.lowerCaseName);
            if (list == null) {
                list = new ArrayList<>();
                matchingBoxes.put(box.lowerCaseName, list);
            }
            list.add(box);
        }
    }

    // Get matching boxes for a goal
    public ArrayList<Box> getMatchingBoxesForGoal(Goal goal) {
//        ArrayList<Box> freeBoxes = new ArrayList<>(matchingBoxes.get(goal.name));
//        for (Box b : matchingBoxes.get(goal.name)) {
//            if (hasSolvedGoal(b) != null) {
//                freeBoxes.remove(b);
//            }
//        }


        ArrayList<Box> freeBoxes = new ArrayList<>();
        ArrayList<ProposedSolution> proposedSolutions = goal.getProposedSolutions();

        for (ProposedSolution solution : proposedSolutions) {
            if (hasSolvedGoal(solution.box) == null) {
                freeBoxes.add(solution.box);
            }
        }


        if (freeBoxes.size() > 0) {
//            if (freeBoxes.contains(goal.suggestedBox)) {
//                Box optimalBox = freeBoxes.remove(freeBoxes.indexOf(goal.suggestedBox));
//                freeBoxes.add(0, optimalBox);
//            }
            return freeBoxes;
        } else {
            return matchingBoxes.get(goal.name);
        }

    }


    public boolean celIsFree(int row, int col) {
        return !(this.walls[row][col]);
    }


    public Box getBox(Integer id) {
        return boxMap.get(id);
    }

    public Goal getGoal(Integer id) {
        return goalMap.get(id);
    }


    public void unsolveGoal(Goal goal) {
        goal.solveGoal(null, Integer.MAX_VALUE);
        solvedGoals.remove(goal);
        unsolvedGoals.add(goal);
    }


    public void solveGoalWithBox(Goal goal, Box box, int atTime) {
        System.err.println("Solving goal: " + goal.name + " at: " + goal.x + "," + goal.y);
        goal.solveGoal(box, atTime);
        solvedGoals.put(box.id, goal);
        unsolvedGoals.remove(goal);
    }

    public Goal hasSolvedGoal(Box box) {
        return solvedGoals.get(box.id);
    }
    public Goal hasSolvedGoal(Integer boxId) {
        return solvedGoals.get(boxId);
    }


    public boolean isParkingCell(int movingBoxX, int movingBoxY) {
//        Position p = new Position(movingBoxX, movingBoxY);
//        return freeCellMap.get(p);
        if (activeCells[movingBoxY][movingBoxX] == 0) {
            return true;
        }
        return false;


    }
}
