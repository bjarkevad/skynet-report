package SkyNet;

import java.util.*;

import SkyNet.Memory;

public abstract class Strategy {

    public HashSet<Node> explored;
    public long startTime = System.currentTimeMillis();

    public Strategy() {
        explored = new HashSet<Node>();
    }

    public void addToExplored(Node n) {
        explored.add(n);
    }

    public boolean isExplored(Node n) {
        return explored.contains(n);
    }

    public int countExplored() {
        return explored.size();
    }

    public String searchStatus() {
        return String.format("#Explored: %4d, #Frontier: %3d, Time: %3.2f s \t%s", countExplored(), countFrontier(), timeSpent(), Memory.stringRep());
    }

    public float timeSpent() {
        return (System.currentTimeMillis() - startTime) / 1000f;
    }

    public abstract Node getAndRemoveLeaf();

    public abstract void addToFrontier(Node n);

    public abstract boolean inFrontier(Node n);

    public abstract int countFrontier();

    public abstract boolean frontierIsEmpty();

    public abstract String toString();

    public boolean doubleFrontier;

    public static class StrategyBFS extends Strategy {

        private ArrayDeque<Node> frontier;

        public StrategyBFS() {
            super();
            frontier = new ArrayDeque<Node>();
        }

        public Node getAndRemoveLeaf() {
            return frontier.pollFirst();
        }

        public void addToFrontier(Node n) {
            frontier.addLast(n);
        }

        public int countFrontier() {
            return frontier.size();
        }

        public boolean frontierIsEmpty() {
            return frontier.isEmpty();
        }

        public boolean inFrontier(Node n) {
            return frontier.contains(n);
        }

        public String toString() {
            return "Breadth-first Search";
        }
    }

    public static class StrategyDFS extends Strategy {

        private Stack<Node> frontier;

        public StrategyDFS() {
            super();
            frontier = new Stack<Node>();
        }

        public Node getAndRemoveLeaf() {
            return frontier.pop();
        }

        public void addToFrontier(Node n) {
            frontier.push(n);

        }

        public int countFrontier() {
            return frontier.size();
        }

        public boolean frontierIsEmpty() {
            return frontier.isEmpty();
        }

        public boolean inFrontier(Node n) {
            return frontier.contains(n);
        }

        public String toString() {
            return "Depth-first Search";
        }
    }


    public static class StrategyBestFirst extends Strategy {
        private Heuristic heuristic;

        boolean doubleFrontier = false;

        private PriorityQueue<Node> frontier;
        private PriorityQueue<Node> oldFrontier;
        private HashSet<Node> frontierSet;
        private HashSet<Node> oldFrontierSet;

        public StrategyBestFirst(Heuristic h) {
            super();
            heuristic = h;
            frontier = new PriorityQueue<Node>(11, heuristic); //11 is default initial capacity
            oldFrontier = new PriorityQueue<Node>(11, heuristic); //11 is default initial capacity
            frontierSet = new HashSet<>();
            oldFrontierSet = new HashSet<>();
        }

        public Node getAndRemoveLeaf() {

            Node node;

            if (doubleFrontier) {
                if (frontier.size() == 0) {
                    node = oldFrontier.poll();
                    oldFrontierSet.remove(node);
                } else {
                    node = frontier.poll();
                    frontierSet.remove(node);

                    oldFrontier.addAll(frontier);
                    frontier.clear();
                    oldFrontierSet.addAll(frontierSet);
                    frontierSet.clear();
                }

            } else {
                node = frontier.poll();
                frontierSet.remove(node);
            }

            if (heuristic != null && !node.isInitialState()) {
//				LOG.d("H: " + heuristic.f(node));
            }

            return node;
        }

        public void addToFrontier(Node n) {
            frontier.add(n);
            frontierSet.add(n);
        }

        public int countFrontier() {
            return (frontier.size() + oldFrontier.size());
        }

        public boolean frontierIsEmpty() {
            return frontier.isEmpty() && oldFrontier.isEmpty();
        }

        public boolean inFrontier(Node n) {
            return frontierSet.contains(n) || oldFrontierSet.contains(n);
//            return frontier.contains(n);
        }

        public String toString() {
            return "Best-first Search (PriorityQueue) using " + heuristic.toString();
        }
    }
}
