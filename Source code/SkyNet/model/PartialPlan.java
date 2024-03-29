package SkyNet.model;

import SkyNet.Node;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * client
 * Created by maagaard on 14/04/15.
 * Copyright (c) maagaard 2015.
 */

public class PartialPlan {

    public LinkedList<Node> plan;


    public Set<Position> path = new HashSet<>();

    public Goal goal;
    public Box box;
    public Agent agent;

    public int priority = 0;

    public PartialPlan(Agent agent, Goal goal, Box box, LinkedList<Node> partialSolution) {
        this.agent = agent;
        this.goal = goal;
        this.box = box;
        this.plan = partialSolution;
    }

    public int size() {
        return plan.size();
    }

    public Node lastNode() {
        return plan.getLast();
    }

}
