package SkyNet.model;

/**
 * client
 * Created by maagaard on 23/05/15.
 * Copyright (c) maagaard 2015.
 */
public class Bid implements Comparable<Bid> {

    public Goal goal;
    public Box box;
    public Agent agent;

    public int heuristic = 0;

    public Bid(Agent agent, Goal goal, Box box, int heuristic) {
        this.agent = agent;
        this.goal = goal;
        this.box = box;
        this.heuristic = heuristic;
    }

    @Override
    public int compareTo(Bid b) {
        return this.heuristic-b.heuristic;
    }
}
