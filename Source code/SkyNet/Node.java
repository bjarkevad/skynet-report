package SkyNet;

import SkyNet.model.*;
import SkyNet.Command.*;

import java.util.*;

/**
 * client
 * Created by maagaard on 31/03/15.
 * Copyright (c) maagaard 2015.
 */
public class Node {

    public Level level;
//    public ArrayList<Box> boxes;
//    public ArrayList<Goal> goals;

    public boolean[][] walls; // = new boolean[MAX_ROW][MAX_COLUMN];
    public int[][] boxes;// = new char[MAX_ROW][MAX_COLUMN];
    public int[][] goals;//; = new char[MAX_ROW][MAX_COLUMN];

    public Goal chosenGoal = null;
    public Box chosenBox;
    public int movingBoxId = 0;
    public int movingBoxX, movingBoxY = 0;
    public int chosenBoxX, chosenBoxY = 0;

    private Agent actingAgent;
    public Node parent;
    public Command action;
    public boolean isExecuted = false;

    private static Random rnd = new Random(1);
    public static int MAX_ROW = 50;     //Default setting
    public static int MAX_COLUMN = 50;  //Default setting

    public int agentRow;
    public int agentCol;

    public int destroyingGoal = 0;
    public int stupidMoveHeuristics = 0;

    private int g;
    private int boxMoves;


    public Node(int rows, int columns) {
        MAX_ROW = rows;
        MAX_COLUMN = columns;

        boxes = new int[rows][columns];

        g = 0;
        boxMoves = 0;
        goals = new int[rows][columns];
        walls = new boolean[rows][columns];
    }

    public Node(Node parent, int rows, int columns) {

        MAX_ROW = rows;
        MAX_COLUMN = columns;

        boxes = new int[rows][columns];

        this.parent = parent;
        if (parent == null) {
            g = 0;
            boxMoves = 0;
            goals = new int[rows][columns];
            walls = new boolean[rows][columns];
        } else {
            actingAgent = parent.actingAgent;
            g = parent.g() + 1;
            boxMoves = parent.boxMoves;
            level = parent.level;
            chosenBox = parent.chosenBox;
            chosenGoal = parent.chosenGoal;
            walls = parent.walls;
            goals = parent.goals;
//            movingBoxId = parent.movingBoxId;

        }
    }

    public Agent assignedAgent() {
        return actingAgent;
    }

    public void assignAgent(Agent agent) {
        this.actingAgent = agent;
        agentRow = agent.y;
        agentCol = agent.x;
    }

    public void updateAgent() {
        this.actingAgent.x = agentCol;
        this.actingAgent.y = agentRow;
    }


    public void resetNodeCount() {
        g = 0;
    }

    public int g() {
        return g;
    }

    public int boxMoves() {
        return boxMoves;
    }

    public boolean isInitialState() {
        return this.parent == null;
    }

    public boolean isGoalState() {

        if (chosenGoal != null && chosenBox != null) {

            Box box = level.getBox(boxes[chosenGoal.y][chosenGoal.x]);
            if (box != null && chosenBox.id == box.id) {// && box.lowerCaseName == chosenGoal.name) {
                return true;
            } else {
                return false;
            }

        } else {
            for (int row = 1; row < MAX_ROW - 1; row++) {
                for (int col = 1; col < MAX_COLUMN - 1; col++) {
//                    char g = goals[row][col];
                    Goal goal = level.getGoal(goals[row][col]);
                    if (goal == null) {
                        continue;
                    }
                    Box box = level.getBox(boxes[row][col]);
                    if (box == null) {
                        continue;
                    }
                    if (Character.toLowerCase(box.name) == goal.name) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    public boolean isSubGoalState() {
        if (chosenGoal != null) {
            if (chosenGoal.subgoals.size() > 0) {
                Iterator iterator = chosenGoal.subgoals.iterator();
                SubGoal sg = (SubGoal) iterator.next();

                Box box = level.getBox(boxes[sg.y][sg.x]);

                if (box != null && sg.suggestedBox.id == box.id) {
                    chosenGoal.subgoals.remove(sg);
                    LOG.d("" + sg.suggestedBox.name);
                    LOG.d("Subgoal size:"+ chosenGoal.subgoals.size());
                    return true;
                } else {
                    return false;
                }
            } else {
                return true;
            }
        }

        return false;
    }

    public LinkedList<Node> extractPlan() {
        LinkedList<Node> plan = new LinkedList<Node>();
        Node n = this;
        while (!n.isInitialState() && !n.isExecuted) {
            plan.addFirst(n);
            n = n.parent;
        }
        return plan;
    }


    public ArrayList<Node> getExpandedNodes() {
        ArrayList<Node> expandedNodes = new ArrayList<Node>(Command.every.length);
        for (Command c : Command.every) {
            // Determine applicability of action
            int newAgentRow = this.agentRow + dirToRowChange(c.dir1);
            int newAgentCol = this.agentCol + dirToColChange(c.dir1);

            if (c.actType == type.Move) {
                // Check if there's a wall or box on the cell to which the agent is moving
                if (cellIsFree(newAgentRow, newAgentCol)) {
                    Node n = this.ChildNode();
                    n.action = c;
                    n.agentRow = newAgentRow;
                    n.agentCol = newAgentCol;
                    expandedNodes.add(n);
                }

            } else if (c.actType == type.Push) {
                // Make sure that there's actually a box to move
                if (boxAt(newAgentRow, newAgentCol)) {
                    int newBoxRow = newAgentRow + dirToRowChange(c.dir2);
                    int newBoxCol = newAgentCol + dirToColChange(c.dir2);
                    // .. and that new cell of box is free
                    if (cellIsFree(newBoxRow, newBoxCol)) {
                        Node n = this.ChildNode();
                        n.action = c;
                        n.boxMoves++;
                        n.agentRow = newAgentRow;
                        n.agentCol = newAgentCol;
                        n.boxes[newBoxRow][newBoxCol] = this.boxes[newAgentRow][newAgentCol];
                        n.boxes[newAgentRow][newAgentCol] = 0;

                        n.movingBoxId = n.boxes[newBoxRow][newBoxCol];
                        n.movingBoxY = newBoxRow;
                        n.movingBoxX = newBoxCol;


                        if (n.chosenBox != null && n.chosenBox.id == n.movingBoxId) {
                            n.chosenBoxX = newBoxCol;
                            n.chosenBoxY = newBoxRow;
                        } else if (n.chosenBox != null && n.chosenBox.id != n.movingBoxId) {
                            if (level.hasSolvedGoal(n.movingBoxId) != null) {
//                                n.destroyingGoal = 1000;//n.movingBoxId;
                                continue;
                            }
                        }
                        expandedNodes.add(n);
                    }
                }

            } else if (c.actType == type.Pull) {
                // Cell is free where agent is going
                if (cellIsFree(newAgentRow, newAgentCol)) {
                    int boxRow = this.agentRow + dirToRowChange(c.dir2);
                    int boxCol = this.agentCol + dirToColChange(c.dir2);
                    // .. and there's a box in "dir2" of the agent
                    if (boxAt(boxRow, boxCol)) {
                        Node n = this.ChildNode();
                        n.action = c;
                        n.boxMoves++;
                        n.agentRow = newAgentRow;
                        n.agentCol = newAgentCol;
                        n.boxes[this.agentRow][this.agentCol] = this.boxes[boxRow][boxCol];
                        n.boxes[boxRow][boxCol] = 0;

                        n.movingBoxId = n.boxes[this.agentRow][this.agentCol];
                        n.movingBoxY = this.agentRow;
                        n.movingBoxX = this.agentCol;


                        if (n.chosenBox != null && n.chosenBox.id == n.movingBoxId) {
                            n.chosenBoxX = this.agentCol;
                            n.chosenBoxY = this.agentRow;
                        } else if (n.chosenBox != null && n.chosenBox.id != n.movingBoxId) {

                            if (level.hasSolvedGoal(n.movingBoxId) != null) {
//                                n.destroyingGoal = 1000;//n.movingBoxId;
                                continue;
                            }

                        }

                        expandedNodes.add(n);
                    }
                }
            }
        }
//        Collections.shuffle(expandedNodes, rnd);
        return expandedNodes;
    }

    public Node addNoOpNode() {
        Node n = this.ChildNode();
        n.action = new Command(type.NoOp);
        n.movingBoxX = this.movingBoxX;
        n.movingBoxY = this.movingBoxY;
        n.chosenBoxY = this.chosenBoxY;
        n.chosenBoxX = this.chosenBoxX;
        n.agentRow = this.agentRow;
        n.agentCol = this.agentCol;
        return n;
    }

    private boolean cellIsFree(int row, int col) {
        return (!this.walls[row][col] && this.boxes[row][col] == 0);
    }

    private boolean boxAt(int row, int col) {
        return this.boxes[row][col] > 0;
    }


    private int dirToRowChange(dir d) {
        return (d == dir.S ? 1 : (d == dir.N ? -1 : 0)); // South is down one row (1), north is up one row (-1)
    }

    private int dirToColChange(dir d) {
        return (d == dir.E ? 1 : (d == dir.W ? -1 : 0)); // East is left one column (1), west is right one column (-1)
    }

    private Node ChildNode() {
        Node copy = new Node(this, MAX_ROW, MAX_COLUMN);
        for (int row = 0; row < MAX_ROW; row++) {
            System.arraycopy(this.boxes[row], 0, copy.boxes[row], 0, MAX_COLUMN);
        }
//        copy.chosenBox = new
        return copy;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + agentCol;
        result = prime * result + agentRow;
        result = prime * result + Arrays.deepHashCode(boxes);
        result = prime * result + Arrays.deepHashCode(goals);
        result = prime * result + Arrays.deepHashCode(walls);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Node other = (Node) obj;
        if (agentCol != other.agentCol)
            return false;
        if (agentRow != other.agentRow)
            return false;
        if (!Arrays.deepEquals(boxes, other.boxes)) {
            return false;
        }
        if (!Arrays.deepEquals(goals, other.goals))
            return false;
        if (!Arrays.deepEquals(walls, other.walls))
            return false;
        return true;
    }

    public String toString() {
        StringBuilder s = new StringBuilder();
        for (int row = 0; row < MAX_ROW; row++) {
            if (!this.walls[row][0]) {
                break;
            }
            for (int col = 0; col < MAX_COLUMN; col++) {
                if (this.boxes[row][col] > 0) {
//                    s.append(this.boxes[row][col]);
                    Box box = level.getBox(this.boxes[row][col]);
                    if (box != null) {
                        s.append(box.name);
                    } else {
                        s.append(this.boxes[row][col]);
                    }
                } else if (this.goals[row][col] > 0) {
//                    s.append(this.goals[row][col]);
                    Goal goal = level.getGoal(this.goals[row][col]);
                    if (goal != null) {
                        s.append(goal.name);
                    } else {
                        s.append(this.boxes[row][col]);
                    }
                } else if (this.walls[row][col]) {
                    s.append("+");
                } else if (row == this.agentRow && col == this.agentCol) {
                    s.append(this.actingAgent.number);
                } else {
                    s.append(" ");
                }
            }

            s.append("\n");
        }
        return s.toString();
    }


    public boolean conflictsWithNodeAction(Node otherNode) {
        System.err.println("Agent: " + this.actingAgent.number + " - " + agentCol + "," + agentRow + "  Other agent: " + otherNode.agentCol + "," + otherNode.agentRow);
        if ((otherNode.agentCol == agentCol && otherNode.agentRow == agentRow) ||
                (parent.agentCol == otherNode.agentCol && parent.agentRow == otherNode.agentRow) ||
                (agentCol == otherNode.parent.agentCol && agentRow == otherNode.parent.agentRow) ||
                (parent.agentCol == otherNode.parent.agentCol && parent.agentRow == otherNode.parent.agentRow)) {

            return true;
        }
        if (otherNode.movingBoxX == this.movingBoxX && otherNode.movingBoxY == this.movingBoxY && this.movingBoxId != 0 && otherNode.movingBoxId != 0) {
            return true;
        }
        return false;
    }


}
