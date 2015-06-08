package SkyNet.model;

import SkyNet.Node;

import java.util.ArrayList;

/**
 * client
 * Created by maagaard on 31/03/15.
 * Copyright (c) maagaard 2015.
 */
public class Agent implements Comparable<Agent> {

    public Agent(char number, int x, int y) {
        this.number = number;
        this.x = x;
        this.y = y;
    }

    public char number;
    public int x;
    public int y;
    public String color;
    public Node state;

    public ArrayList<Goal> assignedGoals = new ArrayList<>();
    public ArrayList<PartialPlan> partialPlans = new ArrayList<>();

    private int burden = 0;


    public void updateStateBoxesWithLevel(Level level) {

        state.boxes = new int[level.height][level.width];

        for (int i = 0; i < level.boxes.size(); i++) {
            Box b = level.boxes.get(i);
//            currentState.boxes[b.y][b.x] = b.name;
            state.boxes[b.y][b.x] = b.id;
        }
    }

    public void createInitialState(Level level) {

        state = new Node(level.height, level.width);
        state.assignAgent(this);
        state.level = level;
        state.walls = level.walls;

        /** Add all goals and boxes to initial state node */
        for (int i = 0; i < level.goals.size(); i++) {
            Goal g = level.goals.get(i);
            state.goals[g.y][g.x] = g.id;
        }
        updateStateBoxesWithLevel(level);
    }

    public void assignGoal(Goal goal) {
        this.assignedGoals.add(goal);
    }

    /**
     * Get the agent's work-burden
     * @return number of optimal moves as int
     */
    public int burden() {
        int burden = 0;
        for (Goal goal : assignedGoals) {
            burden += goal.optimalSolutionLength;
        }
        return burden;
    }


    @Override
    public int compareTo(Agent agent) {
        return this.number - agent.number;
    }


    /**
     * Bid on a goal with solution based on naive heuristics
     * @param goal Goal
     * @param box Box to solve goal with
     * @return A bid
     */
    public Bid bid(Goal goal, Box box) {

        if (color != box.color) {
            //This agent is not eligible for moving the box
            return null;
        }

        // return distance to box and distance from box to goal
        int agentBoxDist = Math.abs(this.y - box.y) + Math.abs(this.x - box.x);
        int boxGoalDist = Math.abs(box.y - goal.y) + Math.abs(box.x - goal.x);

        // Favour solutions with boxes close to goal
        boxGoalDist = (int) (boxGoalDist * 1.5);

//        return boxGoalDist + agentBoxDist;
        return new Bid(this, goal, box, boxGoalDist + agentBoxDist);
    }

    public void addPartialPlan(PartialPlan plan) {
        partialPlans.add(plan);
    }
}
