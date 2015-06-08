package SkyNet;

import SkyNet.model.*;

import java.util.*;

public abstract class Heuristic implements Comparator<Node> {

    public Node initialState;
    public Level level;
    //    private Agent actingAgent;
//    private Goal chosenGoal;
//    private Box chosenBox;
    private Map<Goal, Box> solvedGoals;

    public int goalRow, goalColumn;

    public int goalY, goalX, initialBoxY, initialBoxX;


    public Map<Character, int[]> goalMap = new HashMap<Character, int[]>();
    public Map<Character, int[]> boxMap = new HashMap<Character, int[]>();


    /**
     * Initializing heuristics - Maybe do more clever stuff here!
     * Performance boost can maybe be found here
     *
     * @param initialState Node
     */
    public Heuristic(Node initialState) {
        this.initialState = initialState;

        //TODO: Make some of this dynamic !!!!
        if (initialState.chosenGoal != null && initialState.chosenBox != null) {
            solvedGoals = new HashMap<>();
            if (initialState.level != null) {
                for (Goal goal : initialState.level.goals) {
                    if (goal.isSolved()) {
                        solvedGoals.put(goal, goal.getBox());
//                    Box box = initialState.boxes[goal.y][goal.x];
//                    for (Box box : initialState.level.boxes) {
//                    }
                    }
                }
            }
        }

        //TODO: Make dynamic maybe??

        //TODO: OLD STUFF - MAYBE RE-WRITE TO BE USED AGAIN
        for (int i = 0; i < initialState.goals.length; i++) {
            for (int j = 0; j < initialState.goals[i].length; j++) {
                if (initialState.goals[i][j] != 0) {
//                    Advanced
//                    goalMap.put(Character.toLowerCase(initialState.goals[i][j]), new int[]{i, j});
//                    goalRow = i;
//                    goalColumn = j;

                    goalY = i;
                    goalX = j;
                }
            }
        }
        for (int i = 0; i < initialState.boxes.length; i++) {
            for (int j = 0; j < initialState.boxes[i].length; j++) {
                if (initialState.boxes[i][j] != 0) {
//                    boxMap.put(Character.toLowerCase(initialState.level.getBox(initialState.boxes[i][j]).name), new int[]{i, j});
                    initialBoxY = i;
                    initialBoxX = j;
                }
            }
        }
    }

    public int compare(Node n1, Node n2) {
        return f(n1) - f(n2);
    }


    /**
     * h2 - Not used anymore
     *
     * @param n Node
     * @return heuristics
     */
    public int h2(Node n) {
        ArrayList<Integer> combinedDistances = new ArrayList<Integer>();

        Map<Character, Integer> boxDistances = new HashMap<Character, Integer>();
        for (Character c : boxMap.keySet()) {
            double a = Math.pow(n.agentRow - boxMap.get(c)[0], 2);
            double b = Math.pow(n.agentCol - boxMap.get(c)[1], 2);
            int agentBoxDistance = (int) Math.sqrt(a + b);

            a = Math.pow(boxMap.get(c)[0] - goalMap.get(c)[0], 2);
            b = Math.pow(boxMap.get(c)[1] - goalMap.get(c)[1], 2);
            int boxGoalDistance = (int) Math.sqrt(a + b);

            combinedDistances.add(agentBoxDistance + boxGoalDistance);
        }

//        System.err.println("Distance" + combinedDistances);
        Collections.sort(combinedDistances);
        return combinedDistances.get(0);
    }


    /**
     * Full heuristics - intended to get heuristics for all boxes and goals
     *
     * @param n Node
     * @return heuristics value
     */
    public int fullH(Node n) {

        return 0;
    }


    /**
     * Heuristics used for the initial POP solution that only considers walls, one agent, one box and one goals
     *
     * @param n Node
     * @return Heuristics
     */
    public int partialH(Node n) {

//        if (n.action.actType == Command.type.Move) {
//            int discourageMovingAround = 5;
//
//            if (n.boxes[initialBoxY][initialBoxX] > 0) {
//                int agentBoxDist = Math.abs(initialBoxY - n.agentRow) + Math.abs(initialBoxX - n.agentCol);
//                int boxGoalDist = Math.abs(goalY - initialBoxY) + Math.abs(goalX - initialBoxX);
//                return boxGoalDist + agentBoxDist + discourageMovingAround;
//            } else {
//                int agentBoxDist = Math.abs(n.movingBoxY - n.agentRow) + Math.abs(n.movingBoxX - n.agentCol);
//                int boxGoalDist = Math.abs(goalY - n.movingBoxY) + Math.abs(goalX - n.movingBoxX);
//                return boxGoalDist + agentBoxDist + discourageMovingAround;
//            }
//        } else {
//            return Math.abs(goalY - n.movingBoxY) + Math.abs(goalX - n.movingBoxX);
//        }

        int agentBoxDist = Math.abs(n.movingBoxY - n.agentRow) + Math.abs(n.movingBoxX - n.agentCol);
        int boxGoalDist = Math.abs(goalY - n.movingBoxY) + Math.abs(goalX - n.movingBoxX);
        return boxGoalDist + agentBoxDist;// + discourageMovingAround;
    }


    /**
     * Solved goal distance used to add a large value to heuristics to discourage moving solved goals - deprecated
     *
     * @param n node
     * @return heuristics value
     */
    public int solvedGoalDistance(Node n) {
        int solvedGoalDistance = 0;
        if (solvedGoals != null && solvedGoals.size() > 0) {
            for (Goal goal : solvedGoals.keySet()) {
//                Box box = solvedGoals.get(goal);
//                System.err.println("Checking solved goal: " + goal.x + "," +goal.y);
                if (n.boxes[goal.y][goal.x] == 0) {
                    solvedGoalDistance += 101;
                }
            }
        }

        return solvedGoalDistance;
    }

    public int subgoalH(Node n) {
        Iterator iterator = n.chosenGoal.subgoals.iterator();
        SubGoal subgoal = (SubGoal) iterator.next();
        Box subgoalBox = subgoal.suggestedBox;

        if (n.action.actType == Command.type.Move) {
            // return distance to box and distance from box to goal
            int agentBoxDist = agentBoxDist(n.agentRow, n.agentCol, subgoalBox);

            if (agentBoxDist == 1) {
                int boxGoalDist = boxGoalDist(subgoalBox, subgoal);
                return boxGoalDist * 3;
            }

            return agentBoxDist + 5;// + boxGoalDist;

        } else if (n.movingBoxId != subgoalBox.id) {

            int agentBoxDist = agentBoxDist(n.agentRow, n.agentCol, subgoalBox);
            int boxGoalDist = boxGoalDist(subgoalBox, subgoal);

            return (boxGoalDist + agentBoxDist) * 5; //Discourage this as long subgoals exists

        } else {

            int agentBoxDist = agentBoxDist(n.agentRow, n.agentCol, subgoalBox);
            int boxGoalDist = boxGoalDist(subgoalBox, subgoal);

            return boxGoalDist + agentBoxDist; // Encourage this even more
        }
    }


    /**
     * h - main heuristics function
     *
     * @param n Node
     * @return heuristics value
     */
    public int h(Node n) {

        if (n.chosenGoal != null && n.chosenBox != null) {

            if (n.chosenGoal.subgoals.size() > 0) {

                return subgoalH(n);

            }
            else {

                if (n.action.actType == Command.type.Move) {
                    // return distance to box and distance from box to goal
                    int agentBoxDist = Math.abs(n.chosenBoxY - n.agentRow) + Math.abs(n.chosenBoxX - n.agentCol);

                    if (agentBoxDist == 1) {
                        int boxGoalDist = Math.abs(n.chosenGoal.y - n.chosenBoxY) + Math.abs(n.chosenGoal.x - n.chosenBoxX); //boxGoalDist(n.chosenBox, n.chosenGoal);
                        return boxGoalDist * 2;
                    }

                    return agentBoxDist;// + boxGoalDist + 21;

                } else {

                    if (n.movingBoxId != n.chosenBox.id) {

                        boolean isBoxInConflict = n.chosenGoal.conflictingBoxes.contains(n.level.getBox(n.movingBoxId));
                        int agentBoxDist = Math.abs(n.chosenBoxY - n.agentRow) + Math.abs(n.chosenBoxX - n.agentCol);
                        int boxGoalDist = Math.abs(n.chosenGoal.y - n.chosenBoxY) + Math.abs(n.chosenGoal.x - n.chosenBoxX);

                        if (isBoxInConflict) {
                            if (n.level.isParkingCell(n.movingBoxX, n.movingBoxY)) {
                                return boxGoalDist + agentBoxDist;
                            } else {
                                return (boxGoalDist + agentBoxDist) * 2;
                            }
                        }

                        return (boxGoalDist + agentBoxDist) * 3;

                    } else {
                        return Math.abs(n.chosenGoal.y - n.movingBoxY) + Math.abs(n.chosenGoal.x - n.movingBoxX);
                    }

                    //TODO: Encourage moving of correct box towards goal
                    //TODO: Correlate with setting of chosen box in Node

                }
            }

        } else {
            return partialH(n);
        }
    }


    private int boxGoalDist(Box box, Goal goal) {
        return (Math.abs(goal.y - box.y) + Math.abs(goal.x - box.x));
    }

    private int agentBoxDist(int agentRow, int agentCol, Box box) {
        return (Math.abs(box.y - agentRow) + Math.abs(box.x - agentCol));
    }


    public abstract int f(Node n);

    public static class AStar extends Heuristic {
        public AStar(Node initialState) {
            super(initialState);
        }

        public int f(Node n) {
//            System.err.println("g: "+n.g()+ " h: "+h(n));
            return n.g() + h(n);
        }

        public String toString() {
            return "A* evaluation";
        }
    }

    public static class WeightedAStar extends Heuristic {
        private int W;

        public WeightedAStar(Node initialState) {
            super(initialState);
            W = 5; // You're welcome to test this out with different values, but for the reporting part you must at least indicate benchmarks for W = 5
        }

        public int f(Node n) {
            return n.g() + W * h(n);
        }

        public String toString() {
            return String.format("WA*(%d) evaluation", W);
        }
    }

    public static class Greedy extends Heuristic {

        public Greedy(Node initialState) {
            super(initialState);
        }

        public int f(Node n) {
            return h(n);
        }

        public String toString() {
            return "Greedy evaluation";
        }
    }
}
