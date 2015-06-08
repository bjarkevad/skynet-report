package SkyNet;

import SkyNet.model.Plan;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

public class LevelWriter {
    public static void ExecutePlan(Plan plan, BufferedReader serverMessages) throws IOException {

        /** MULTI AGENT STYLE */
        if (plan.GetPlans() != null) {

            for (int i = 0; i < plan.longestPlanSize(); i++) {

                String jointAction = "[";

//                for ()
                for (LinkedList<Node> singlePlan : plan.GetPlans()) {
                    if (singlePlan.size() <= i) {
                        //SET NO-OP
                        jointAction += "NoOp,";
                    } else {
                        singlePlan.get(i).isExecuted = true;
                        jointAction += singlePlan.get(i).action.toString() + ",";
                    }


                }

                jointAction = jointAction.substring(0, jointAction.length()-1);
                jointAction += "]";

//                LOG.v(jointAction);
                System.out.println(jointAction);

                String response = serverMessages.readLine();

                //TODO: Run through array and see if any moves couldn't be done - go back and insert NoOp and do again?
                String[] responses = response.split(",");

                ArrayList<String> responseList = new ArrayList<>(Arrays.asList(responses));
                for (String message : responseList) {
//                    System.err.println(response);
//                    System.err.println(message);

                    if (message.contains("false")) {
                        int planIndex = responseList.indexOf(message);
//                        System.err.println("Index: "+planIndex);
//                        System.err.println("Command: "+i);
                        plan.GetPlans().get(planIndex).addFirst(null);
                    }
                }
                if (!response.contains("true") && response.contains("false")) {
                    //Add extra NoOp to some
//                    plan.GetPlans().get(0).addFirst(null);
//                    System.err.println("WHAT: " + response);
                }

                if (response.contains("false")) {
                    System.err.format("Server responsed with %s to the inapplicable action: %s\n", response, jointAction);
//                    System.err.format("%s was attempted in \n%s%s \n", jointAction, plan.GetPlans().get(0).get(i), plan.GetPlans().get(1).get(i));
                    //                break;
                    continue;
                }
                else if (response.contains("success")) {
                    System.err.format("Finished");
                } else if (i == plan.longestPlanSize()) {
                    //TODO: NOT FINISHED WITH LEVEL YET
                    //TODO: FORCE IT TO DO MORE SOMEHOW
                }
            }
        }

        /** SINGLE AGENT STYLE */
        else if (plan.GetPlan() != null) {

            for (Node n : plan.GetPlan()) {
                n.isExecuted = true;
                String act = n.action.toActionString();
                System.out.println(act);
                String response = serverMessages.readLine();
                if (response.contains("false")) {
                    System.err.format("Server responsed with %s to the inapplicable action: %s\n", response, act);
                    System.err.format("%s was attempted in \n%s\n", act, n);
                    //                break;
                    continue;
                }
            }
        }

    }
}
