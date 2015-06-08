package SkyNet;

import SkyNet.model.*;
import SkyNet.Strategy.*;
//import SkyNet.PartialStrategy.*;
//import SkyNet.PartialPlanNode;
//import SkyNet.PartialPlanHeuristic.*;
import SkyNet.Heuristic.*;

import java.io.IOException;
import java.util.*;


public class PartialPlanner {

    public PartialPlanner() {
    }


    /**
     * Create a single relaxed partial plan based on the parameters given
     * @param level The level
     * @param agent The agent chosen to solve the goal
     * @param goal The goal
     * @param box The box chosen to solve the goal
     * @return A partial plan for solving the goal with the given parameters
     */
    public PartialPlan createPartialPlan(Level level, Agent agent, Goal goal, Box box) {
        LinkedList<Node> solution = extractSubgoalSolution(level, agent, goal, box);
        if (solution == null) { return null; }
        if (solution.size() == 0) { return null; }

        PartialPlan plan = new PartialPlan(agent, goal, box, solution);
        goal.optimalSolutionLength = plan.size();
        goal.suggestedBox = box;

        return plan;
    }


    /**
     * Create relaxed partial plans for all goals from an initial state
     * @param level The level
     * @param agent The assigned agent - change somehow
     * @param goals The Goals to be solved by the agent
     * @return A list of partial plans solving each goal from the initial state
     */
    public ArrayList<PartialPlan> createPartialPlans(Level level, Agent agent, ArrayList<Goal> goals) {
        ArrayList<PartialPlan> partialPlans = new ArrayList<>();

        //TODO: Update to handle multiple agents
//        Agent agent = level.agents.get(0);
//        LOG.d("Number of agents: " + level.agents.size());


//        for (Goal goal : level.unsolvedGoals) {//.goals) {
        for (Goal goal : goals) {//.goals) {
            LinkedList<PartialPlan> solutionList = new LinkedList<>();

            for (Box box : level.boxes) {
                if (Character.toLowerCase(goal.name) == Character.toLowerCase(box.name)) {
                    LOG.d("Agent: " + agent.number + ", goal: " + goal.name + ", box: " + box.name);

                    LinkedList<Node> solution = extractSubgoalSolution(level, agent, goal, box);
                    solutionList.add(new PartialPlan(agent, goal, box, solution));

                    if (solution == null) {
                        LOG.d("No solution found\n");
                    } else if (solution.size() == 0) {
                        LOG.d("Solution of length 0 is wrong");
                    }
                }
            }

            PartialPlan shortestPlan = solutionList.pop();
            goal.addSolutionBox(shortestPlan.box, shortestPlan.size());

            for (PartialPlan plan : solutionList) {

//                int prioritizedSize = plan.size();
                plan.priority = plan.size();

                if (plan.size() < shortestPlan.size()) {
                    shortestPlan = plan;
                } else if (plan.size() == shortestPlan.size()) {
                    if (plan.lastNode().boxMoves() < shortestPlan.lastNode().boxMoves()) {
                        plan.priority = shortestPlan.priority-1;
                        shortestPlan = plan;
                    }
                }


                // TODO: MAYBE SOME GOALS CAN'T BE SOLVED WITH CERTAIN BOXES !!
                // TODO: CHECK FOR THAT

                goal.addSolutionBox(plan.box, plan.priority);
            }

            goal.suggestedBox = shortestPlan.box;
            goal.optimalSolutionLength = shortestPlan.size();
            goal.partialPlan = shortestPlan;

            //TODO: See if any goals uses the same box for shortest solution - and solve problem?

            partialPlans.add(shortestPlan);
        }
        return partialPlans;
    }

    /**
     * Extract relaxed solution for a goal
     * @param level Level
     * @param agent Agent
     * @param goal Goal
     * @param box Box to solve goal with
     * @return List of nodes composing plan for solving goal
     */
    private LinkedList<Node> extractSubgoalSolution(Level level, Agent agent, Goal goal, Box box) {

        Node state = new Node(null, level.height, level.width);
        state.level = level;

        state.walls = level.walls;
        state.assignAgent(agent);
        state.goals[goal.y][goal.x] = goal.id;
        state.boxes[box.y][box.x] = box.id;

        Strategy strategy = new StrategyBestFirst(new AStar(state));

        try {
            LinkedList<Node> partialPlan = PartialSearch(strategy, state);
            if (partialPlan == null) return null;
            LOG.d("Search starting with strategy " + strategy);
            return partialPlan;
        } catch (IOException e) {
            e.printStackTrace();
            LOG.d("Error");
            return null;
        }
    }


    /**
     * Relaxed partial search
     * @param strategy Strategy to perform search with
     * @param state Initial state
     * @return List of nodes composing plan for solving goal
     * @throws IOException
     */
    public LinkedList<Node> PartialSearch(Strategy strategy, Node state) throws IOException {
        LOG.d("Search starting with strategy " + strategy);

        strategy.addToFrontier(state);

        int iterations = 0;
        while (true) {
            if (iterations % 1000 == 0) { LOG.d(strategy.searchStatus()); }
            if (Memory.shouldEnd()) {
                LOG.d("Memory limit almost reached, terminating search " + Memory.stringRep());
                return null;
            }
            if (strategy.timeSpent() > 600) { // Minutes timeout
                LOG.d("Time limit reached, terminating search " + Memory.stringRep());
                return null;
            }
            if (strategy.frontierIsEmpty()) {
                LOG.d("Frontier is empty\n");
                return null;
            }

            Node leafNode = strategy.getAndRemoveLeaf();

            if (leafNode.isGoalState()) {
                LOG.d("Goal state reached\n");
                return leafNode.extractPlan();
            }

            strategy.addToExplored(leafNode);
            for (Node n : leafNode.getExpandedNodes()) {
                if (!strategy.isExplored(n) && !strategy.inFrontier(n)) {
                    strategy.addToFrontier(n);
                }
            }

            iterations++;
        }
    }

}
