package SkyNet;

import SkyNet.model.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * client
 * Created by maagaard on 10/05/15.
 * Copyright (c) maagaard 2015.
 */
public class MasterPlanner implements Planner {

    private Strategy strategy;
    private Level level;
    private Node currentState;

    private Node lastSolvedGoalState;
    private Box currentSolutionBox;

    private PartialPlanner partialPlanner;
    private ArrayList<PartialPlan> partialPlans = null;
    private ArrayList<Goal> sortedGoals = new ArrayList<>();
    private int solvedGoalCount = 0;

    private BufferedReader serverMessages;  //Used when executing live while planning
    private Plan plan;

    private int[][] activeCells;

    private ArrayList<Node> agentStates = new ArrayList<>();

    public MasterPlanner(BufferedReader serverMessages) {
        this.partialPlanner = new PartialPlanner();
        this.serverMessages = serverMessages;
        plan = new Plan();
    }


    /**
     * Create plan - create the full plan
     *
     * @param level Level
     * @return plan
     */
    @Override
    public Plan createPlan(Level level) {
        this.level = level;

        distributeGoals();
        activeCells = mergePartialPlans();
        level.activeCells = activeCells;

        ArrayList<Goal> sortedGoals = sortGoals();

        initAgents();   //Initialize agent states in order to do state-wide active cell checks


        for (Goal goal : sortedGoals) {
            LOG.d("Agent: " + goal.partialPlan.agent.number + " solving " + goal.name);
            createPlanForAgentAndGoal(goal.partialPlan.agent, goal);
        }


        // Extract plans from the agents' nodes
        for (Agent agent : level.agents) {
            Node endState = agent.state;
            plan.addPlan(endState.extractPlan());
        }

        return plan;

    }


    private void initAgents() {
        level.agents.stream().filter(agent -> agent.state == null).forEach(agent -> {
            agent.createInitialState(level);
            agentStates.add(agent.state);
        });
    }


    private void distributeGoals() {
        bidOnGoals();

        for (Goal goal : level.goals) {

            Stream<Bid> sortedBids = goal.getBids().stream().sorted((b1, b2) -> b1.heuristic + b1.agent.burden() - (b2.heuristic + b2.agent.burden()));

            for(Bid bid : sortedBids.collect(Collectors.toList())){

                // TODO: Maybe do a solution for each of the shortest bid for EACH box

                if (bid.box.hasWonBid) {
                    continue;
                } // Box already used to solve other goal


                PartialPlan plan = partialPlanner.createPartialPlan(level, bid.agent, goal, bid.box);
                if (plan == null) {
                    continue;
                } //INVALID SOLUTION BID - TAKE NEXT

                goal.addSolutionBox(plan.box, plan.size());
                plan.box.hasWonBid = true;
                bid.agent.assignGoal(goal);
                bid.agent.addPartialPlan(plan);
                goal.partialPlan = plan;
                break;
            }

        }
    }


    /**
     * Run through all goals, agent and boxes to bid on goals.
     * Agents only bid on boxes they are eligible to move (cf. box and agent colors)
     */
    private void bidOnGoals() {
        for (Goal goal : level.goals) {

            ArrayList<Box> matchingBoxes = level.getMatchingBoxesForGoal(goal);

            for (Agent agent : level.agents) {

                for (Box box : matchingBoxes) {

                    Bid bid = agent.bid(goal, box);
                    if (bid != null) {
                        goal.addBid(bid);
                    }
                }
            }
            goal.sortBids();
        }
    }


    private int[][] mergePartialPlans() {
        int[][] activeCells = new int[level.height][level.width];
//        int[][] staticCells = new int[level.height][level.width];
        for (int i = 0; i < level.height; i++) {
            for (int j = 0; j < level.width; j++) {
                activeCells[i][j] = level.walls[i][j] ? 1 : 0;
            }
        }

        for (Goal goal : level.goals) {
            activeCells[goal.y][goal.x] = 1;
        }

        for (Goal goal : level.goals) {
            PartialPlan partialPlan = goal.partialPlan;
            if (partialPlan.goal.isSolved()) {
                continue;
            }

            for (Node node : partialPlan.plan) {
                activeCells[node.agentRow][node.agentCol] = 2;
                activeCells[node.movingBoxY][node.movingBoxX] = 3;
            }
        }

        return activeCells;
    }

    /**
     * Create a plan to solve a goal for an agent.
     * Saves the last node of the solution as the current state on the agent.
     * Next goal solved by this agent will be continued from the last state.
     *
     * @param agent Agent
     * @param goal  Goal
     */
    private void createPlanForAgentAndGoal(Agent agent, Goal goal) {

        currentState = agent.state;

//        for (Goal g : goal.conflictingPlans) {
//            // Do NoOp until goal is solved by other agent at some point
//            while (!g.isSolved() || g.solvedAtTime > currentState.g()) {
//                currentState = currentState.addNoOpNode();
//            }
//        }

        //TODO: FIX update of state - maybe correlate with a time step or something?
        agent.updateStateBoxesWithLevel(level);


        this.strategy = new Strategy.StrategyBestFirst(new Heuristic.AStar(currentState));

        /** Find boxes conflicting with plan */
        updateConflictingBoxes(goal);


        LinkedList<Node> solution = solveGoalWithBox(null, goal, goal.partialPlan.box);
        int subSolutionLength = solution.size() - currentState.g();


        if (solution == null) {
            LOG.v("NO SOLUTION FOUND - RE-PLAN");
            System.exit(0);
        }

        LOG.d("Found solution: " + subSolutionLength);
        LOG.d("Optimal solution: " + goal.optimalSolutionLength);

        // Check if solution is close to the admissible result - if yes just go with it?
        if (subSolutionLength <= (goal.optimalSolutionLength + 200)) {
//                break;
        } else {
            LOG.d("Should maybe try another box.");
//            System.exit(0);
        }


        //State and level updating and marking goal as solved etc.
        currentState = solution.getLast();

        currentState.level.solveGoalWithBox(currentState.chosenGoal, currentState.chosenBox, currentState.g()); // goal.solveGoal(currentState.chosenBox);
        sortedGoals.remove(currentState.chosenGoal);
        currentState.chosenBox.x = goal.x;
        currentState.chosenBox.y = goal.y;
        currentState.updateAgent();
        agent.state = currentState;



        solvedGoalCount++;
        updateLevelBoxes();

        if (!goal.isSolved()) {
            createPlanForAgentAndGoal(agent, goal);
        }
//        updateConflictingBoxes(agent.assignedGoals);
    }


    /**
     * Find a solution for a specific goal
     *
     * @param strategy Supplied strategy - included here for use when backtracking
     * @param goal     Chosen goal
     * @param box      Chosen box to solve goal with
     * @return List of nodes composing plan for solving goal
     */
    private LinkedList<Node> solveGoalWithBox(Strategy strategy, Goal goal, Box box) {
//        LOG.d("______________________________");
        LOG.d("Agent: " + currentState.assignedAgent().number + ", goal: " + goal.name + ", box: " + box.name + " at " + box.x + "," + box.y);

        LinkedList<Node> partialSolution = extractPlan(strategy, goal, box);

        /** Back track from last successful node and solve goal again */
        if (partialSolution == null || partialSolution.size() == 0) {
            LOG.d("Back tracking");
            //TODO: Something is wrong - back-track

            Node node = currentState.parent;
            node.stupidMoveHeuristics = 300;

            if (solvedGoalCount >= 1) {
                currentState = node;
                solvedGoalCount = sortedGoals.indexOf(currentState.chosenGoal) - 1;
                return solveGoalWithBox(this.strategy, currentState.chosenGoal, currentState.chosenBox);
            } else {
                // Backtracked to first goal - skip back-tracking and try another box
                LOG.v("########### I SHOULDN'T BE IN HERE !!!!!!");
                currentState = lastSolvedGoalState;
//                return solveGoalWithBox(this.strategy, agent, currentState.chosenGoal, currentState.chosenBox);
                // Switch strategy and try again
            }
        }

        return partialSolution;
    }


    /**
     * Update the box locations in the level variable
     * Should be done after a goal is solved
     */
    private void updateLevelBoxes() {
        for (int row = 0; row < currentState.boxes.length; row++) {
            for (int col = 0; col < currentState.boxes[0].length; col++) {
                if (currentState.boxes[row][col] != 0) {
                    Box box = currentState.level.getBox(currentState.boxes[row][col]);
                    box.x = col;
                    box.y = row;
                }
            }
        }
    }


    /**
     * Sort all goals
     *
     * @return List of sorted goals
     */
    private ArrayList<Goal> sortGoals() {
        ArrayList<Goal> sortedGoals = new ArrayList<>();
        int longestPlan = 0;
        for (Goal goal : level.goals) {
            PartialPlan p = goal.partialPlan;
            LOG.d("Plan " + p.goal.name + " length: " + p.size());
            if (p.size() > longestPlan) {
                longestPlan = p.size();
            }
        }

        discoverConflicts(level.goals);

        for (Goal goal : level.goals) {   // could also be level.unsolvedGoals if modified

            goal.sizePriority = normalizedSizePriority(longestPlan, goal.optimalSolutionLength, level.goals.size());
            int magicNumber = 100;
            goal.conflictPriority = (goal.conflictingPlans.size()) * magicNumber * level.goals.size();

            LOG.d("Plan " + goal.name + " size priority: " + goal.sizePriority + " conflict size: " + goal.conflictingPlans.size() + " total priority: " + (goal.sizePriority + goal.conflictPriority));

            sortedGoals.add(goal);
        }

        Collections.sort(sortedGoals);

        //Print goal order
        LOG.d("Goal order: ");
        for (Goal g : sortedGoals) {
            LOG.d("" + g.name);
        }

        return sortedGoals;
    }


    /**
     * Normalize size priority with regards to longest plan and number of goals - REWRITE THIS TO INCLUDE ALL GOALS
     *
     * @param longestPlanSize Longest plan size for normalizing - SHOULD BE FROM ALL PLANS FOR ALL GOALS !!!!!!!!!!!!! (NOT ONLY ASSIGNED)
     * @param planSize        Size of plan
     * @param goalCount       Number of goals, should be refactored and included in longestPlanSize
     * @return normalized size priority
     */
    private int normalizedSizePriority(int longestPlanSize, int planSize, int goalCount) {
        int magicNumber = 10;
        return (int) (((float) longestPlanSize / (float) planSize) * magicNumber) / (goalCount) * magicNumber;
    }



    /**
     * Discover conflicts between goals and plans - RE-WRITE TO INCLUDE ALL GOALS AND MAKE A GLOBAL LIST
     *
     * @param goals Goals assigned to agent ?? OR IS IT ?
     */
    public void discoverConflicts(ArrayList<Goal> goals) {

        for (Goal currentGoal : goals) {

            PartialPlan partialPlan = currentGoal.partialPlan;

            ArrayList<Goal> otherGoals = new ArrayList<>(goals);     //new ArrayList<>(level.goals);
            otherGoals.remove(partialPlan.goal);

            for (Node node : partialPlan.plan) {
                for (Goal goal : otherGoals) {
                    // Maybe also check for agent location
                    int x = goal.x;
                    int y = goal.y;

                    if (node.boxes[y][x] != 0) {
                        if (goalConflictUnavoidable(node, x, y)) {
                            goal.conflictingPlans.add(partialPlan.goal);
                        }
                    }
                }
            }
        }
    }


    /**
     * Conflict avoidable or not
     * @param n Node
     * @param x Cell x-coordinate
     * @param y Cell y-coordinate
     * @return Whether or not a conflict is avoidable in order to determine goal interdependency
     */
    private boolean goalConflictUnavoidable(Node n, int x, int y) {
        // Horizontal issue
        if (n.walls[y - 1][x] || n.walls[y + 1][x] && (n.action.dir1 == Command.dir.E || n.action.dir1 == Command.dir.W)) {
            return true;
        } else if (n.walls[y][x + 1] || n.walls[y][x + 1] && (n.action.dir1 == Command.dir.S || n.action.dir1 == Command.dir.N)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Conflict avoidable with box or not
     * @param n Node
     * @param x Cell x-coordinate
     * @param y Cell y-coordinate
     * @return Whether or not a conflict is avoidable in order to determine goal interdependency
     */
    private boolean boxConflictUnavoidable(Node n, int x, int y) {
        // Horizontal issue
        if (n.walls[y - 1][x] && n.walls[y + 1][x] && (n.action.dir1 == Command.dir.E || n.action.dir1 == Command.dir.W)) {
            return true;
        } else if (n.walls[y][x + 1] && n.walls[y][x + 1] && (n.action.dir1 == Command.dir.S || n.action.dir1 == Command.dir.N)) {
            return true;
        } else {
            return false;
        }
    }



    /**
     * DEPRECATED Conflict discovery - discover conflicts between plans and goals
     */
    public void conflictDiscovery2() {
        for (PartialPlan partialPlan : partialPlans) {

            ArrayList<Goal> goals = new ArrayList<>(level.goals);
            goals.remove(partialPlan.goal);

            Set<Goal> conflictGoalBox = new HashSet<>();
            Set<Goal> conflictGoalAgent = new HashSet<>();

            for (Node node : partialPlan.plan) {
                for (Goal goal : goals) {
                    //Goal box movement conflict
                    if (node.boxes[goal.y][goal.x] != 0) {
                        conflictGoalBox.add(goal);
                        goal.conflictingPlans.add(partialPlan.goal);
                        LOG.d("Goal: " + goal.name + " conflicting with plan for " + partialPlan.goal.name);
                    }
                    // Goal agent conflict
                    if (goal.x == node.agentCol && goal.y == node.agentRow) {
                        conflictGoalAgent.add(goal);
//                        goal.conflictingPlans.add(partialPlan.goal);

                        LOG.d("Goal: " + goal.name + " conflicting with plan for " + partialPlan.goal.name);
                    }
                }
            }
//            float planSizePriority = (((float) longestPlan / (float) partialPlan.size()) * 10) / (level.goals.size()) * 10;
            int goalConflictPriority = (conflictGoalBox.size()) * 100 * level.goals.size();
//            partialPlan.goal.priority = goalConflictPriority + (int) planSizePriority;
//            LOG.d("Plan " + partialPlan.goal.name + " size priority: " + planSizePriority + " conflict size: " + conflictGoalBox.size() + " total priority: " + partialPlan.goal.priority);
        }
    }


    /**
     * Update list of boxes that conflict with goal plan
     *
     * @param goal
     */
    private void updateConflictingBoxes(Goal goal) {
        ArrayList<Goal> goals = new ArrayList<>();
        goals.add(goal);
        updateConflictingBoxes(goals);
    }

    /**
     * Update boxes that conflicts
     * Update the partial plans
     *
     * @param goals list of goals
     */
    private void updateConflictingBoxes(ArrayList<Goal> goals) {
//        for (PartialPlan partialPlan : partialPlans) {
        for (Goal goal : goals) {
            PartialPlan partialPlan = goal.partialPlan;
            if (partialPlan.goal.isSolved()) {
                continue;
            }

            ArrayList<Box> boxes = new ArrayList<>(level.boxes);
            boxes.remove(partialPlan.box); // Chosen box shouldn't be regarded as a conflict

            LinkedHashSet<Box> conflictingBoxes = new LinkedHashSet<>();

            for (Node node : partialPlan.plan) {
                partialPlan.path.add(new Position(node.agentCol, node.agentRow));
                if (node.movingBoxId != 0) {
                    partialPlan.path.add(new Position(node.movingBoxX, node.movingBoxY));
                }

                for (Box box : boxes) {
                    if (box.x == node.agentCol && box.y == node.agentRow) {
//                        LOG.d("Box: " + box.name + " interfering with plan for " + partialPlan.box.name);
                        if (boxConflictUnavoidable(node, box.x, box.y)) {
                            conflictingBoxes.add(box);
                        }
                    }
                }
            }

            LOG.d("Plan: " + partialPlan.box.name + " has " + conflictingBoxes.size() + " conflicting boxes");
            partialPlan.goal.conflictingBoxes = new HashSet<>(conflictingBoxes);

            for (Box box : conflictingBoxes) {
                partialPlan.goal.subgoals.add(createSubGoalParking(box));
            }
        }
    }


    private SubGoal createSubGoalParking(Box box) {
        int parkingDistance = Integer.MAX_VALUE;
        int x = 0;
        int y = 0;

        for (int i = 0; i < activeCells.length; i++) {
            for (int j = 0; j < activeCells[i].length; j++) {
                if (activeCells[i][j] == 0) {
                    int dist = Math.abs(i - box.y) + Math.abs(j - box.x);
                    if (dist < parkingDistance) {
                        parkingDistance = dist;
                        y = i;
                        x = j;
                    }
                }
            }
        }


        SubGoal g = new SubGoal('#', x, y);
        activeCells[y][x] = 3;
        g.suggestedBox = box;

        LOG.d("Subgoal with box: " + box.name + " from " + box.x + "," + box.y + " to be parked at: " + g.x + "," + g.y);
        return g;
    }


    /**
     * Extract a plan to solve a goal
     * @param strategy passed in here when doing backtracking, else null
     * @param goal     Goal to solve
     * @param box      Box chosen to solve goal with
     * @return List of nodes composing plan for solving goal
     */
    private LinkedList<Node> extractPlan(Strategy strategy, Goal goal, Box box) {

        currentState.chosenGoal = goal;
        currentState.chosenBox = box;

        LOG.d("Goal: " + goal.x + "," + goal.y + " box: " + box.x + "," + box.y);

        if (strategy == null) {
            this.strategy = new Strategy.StrategyBestFirst(new Heuristic.AStar(currentState));
        }

        try {
            LinkedList<Node> partialPlan = Search(this.strategy, currentState);
            if (partialPlan == null) return null;
//            LOG.d("Search starting with strategy %s\n", this.strategy);
            return partialPlan;
        } catch (IOException e) {
            e.printStackTrace();
            LOG.d("Error");
            return null;
        }
    }


    /**
     * Search
     * @param strategy Search strategy
     * @param state    State from which search starts
     * @return List of nodes composing plan for solving goal
     * @throws IOException
     */
    public LinkedList<Node> Search(Strategy strategy, Node state) throws IOException {
        LOG.d("Search starting with strategy " + strategy);
//        Heuristic.AStar h = new Heuristic.AStar(currentState);
        Node lastNode = null;

        strategy.addToFrontier(state);

        int iterations = 0;
        while (true) {
            if (iterations % 1000 == 0) {
                LOG.d(strategy.searchStatus());
            }
            if (Memory.shouldEnd()) {
                LOG.d("Memory limit almost reached, terminating search " + Memory.stringRep());
                return null;
            }
            if (strategy.timeSpent() > 180) { // 20 Minutes timeout
                LOG.d("Time limit reached, terminating search " + Memory.stringRep());
                return null;
            }
            if (strategy.frontierIsEmpty()) {
                LOG.d("Frontier is empty\n");

                //TODO: Back Track

                return null;
//                return lastNode.extractPlan();
            }

            Node leafNode = strategy.getAndRemoveLeaf();
            lastNode = leafNode;

            if (leafNode.isGoalState()) {
                return leafNode.extractPlan();
            }

            if (leafNode.chosenGoal.subgoals.size() > 0) {
                if (leafNode.isSubGoalState()) {
                    LOG.d("Sub goal reached");
                } else if (iterations > 100 && iterations % 40000 == 0) {
                    Iterator<SubGoal> iterator = leafNode.chosenGoal.subgoals.iterator();
                    SubGoal g = iterator.next();
                    g = createSubGoalParking(g.suggestedBox);
                }

            }

            strategy.addToExplored(leafNode);
            if (leafNode.destroyingGoal != 0) {

            }

            for (Node n : leafNode.getExpandedNodes()) {
                if (!strategy.isExplored(n) && !strategy.inFrontier(n)) {
                    strategy.addToFrontier(n);
//                    LOG.d("H: " + h.f(n));
                }
            }

            iterations++;
        }
    }

}
