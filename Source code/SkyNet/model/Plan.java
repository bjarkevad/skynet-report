package SkyNet.model;

import SkyNet.Command;
import SkyNet.Node;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;

public class Plan implements Comparator<PartialPlan> {
    private LinkedList<Node> plan;
    private ArrayList<LinkedList<Node>> multiPlans;
    private Node noOpNode = new Node(0,0);

    private LinkedList<PartialPlan> partialPlans;

    public LinkedList<ArrayList<PathFragment>> paths = new LinkedList<>();

    public LinkedList<Node> GetPlan() {
        return plan;
    }

    public ArrayList<LinkedList<Node>> GetPlans() {
        return multiPlans;
    }

    public Plan() {
        multiPlans = new ArrayList<>();
        noOpNode.action = new Command(Command.type.NoOp);
    }


    public Plan(LinkedList<Node> plan) {
        this.plan = plan;
        this.multiPlans = null;
    }


    public void addPlan(LinkedList<Node> plan) {

        int timeCount = 0;
        ArrayList<PathFragment> path = new ArrayList<>();
        for (Node node : plan) {
            int agentX = node.agentCol;
            int agentY = node.agentRow;

            int fromX = -1;
            int fromY = -1;

            if (!node.isInitialState()) {
                fromX = node.parent.agentCol;
                fromY = node.parent.agentRow;
            }

            int boxId = node.movingBoxId;
            int movingBoxX = -1;
            int movingBoxY = -1;
            if (boxId != 0) {
                movingBoxX = node.movingBoxX;
                movingBoxY = node.movingBoxY;
            }
            PathFragment pathFragment = new PathFragment(node.assignedAgent(), node.chosenBox, fromX, fromY, node.action, timeCount);
            path.add(pathFragment);
            timeCount++;
        }

        paths.add(path);

//        for (LinkedList<Node> currentPlan : multiPlans) {
//            for (int i = 0; i < plan.size(); i++) {
//                if (i >= currentPlan.size()) {
//                    break;
//                }
//                Node step = plan.get(i);
//                Node currentPlanStep = currentPlan.get(i);
//                 //Check if step interferes with current plans
//                if (currentPlanStep.conflictsWithNodeAction(step)) {
//                    System.err.println("Adding no op");
//                    plan.add(i, noOpNode);
//                }
//            }
//        }

        multiPlans.add(plan);
    }

    public int size() {
        if (multiPlans != null) {
            int size = 0;
            for (LinkedList<Node> singlePlan : multiPlans) {
                size += singlePlan.size();
            }
            return size;
        } else if (plan != null) {
            return plan.size();
        }
        return 0;
    }

    public int longestPlanSize() {
        if (multiPlans != null) {
            int longestPlan = 0;
            for (LinkedList<Node> singlePlan : multiPlans) {
                if (singlePlan.size() > longestPlan) {
                    longestPlan = singlePlan.size();
                }
            }
            return longestPlan;
        } else if (plan != null) {
            return plan.size();
        }
        return 0;
    }


//    public MultiPlan(ArrayList<LinkedList<Node>> plans) {this.multiPlans = plans; }

    //TODO: Also prioritize with regards to initial heuristics as to know which plan is best to solve first


    @Override
    public int compare(PartialPlan p1, PartialPlan p2) {
        return p1.priority - p2.priority;
//        return 0;
    }

}
