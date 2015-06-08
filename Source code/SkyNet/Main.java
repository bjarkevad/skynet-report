package SkyNet;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;

//import SkyNet.PartialStrategy.*;
//import SkyNet.PartialPlanHeuristic.*;
import SkyNet.model.Level;
import SkyNet.model.Plan;

public class Main {

    public static void main(String[] args) throws Exception {

        // Use stderr to print to console
        System.err.println("SearchClient initializing.");

        BufferedReader serverMessages = new BufferedReader(new InputStreamReader(System.in));

        //Read level
        Level level = LevelReader.ReadLevel(serverMessages);

        //Instantiate planner
        Planner planner = new MasterPlanner(serverMessages);

        //Create plan
        Plan plan = planner.createPlan(level);


        //Check and output plan
        if (plan.size() == 0) {
            System.err.println("Unable to solve level");
        } else {
            //TODO: Maybe remove this stuff?

            System.err.println("Found solution of length " + plan.size());

            LevelWriter.ExecutePlan(plan, serverMessages);
        }

        System.exit(0);
    }
}
