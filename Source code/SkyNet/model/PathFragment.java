package SkyNet.model;

import SkyNet.Command;

import java.util.Arrays;

/**
 * client
 * Created by maagaard on 31/03/15.
 * Copyright (c) maagaard 2015.
 */
public class PathFragment {

    public Command action;
    public Position boxLocation;
    public Position agentLocation;
    public Position newLocation;
    public Box movingBox;
    public int time;

    public int pathLength;


    public PathFragment(Agent agent, Box box, int fromX, int fromY, Command action, int time) {
        this.newLocation = new Position(fromX, fromY);
        this.agentLocation = new Position(agent.x, agent.y);
        this.boxLocation = new Position(box.x, box.y);
        this.action = action;
        this.time = time;
    }


//    public PathFragment(Agent agent, Box box, int newLocationX, int newLocationY, Command action, int length) {
//        this.newLocation = new Position(newLocationX, newLocationY);
//        this.agentLocation = new Position(agent.x, agent.y);
//        this.boxLocation = new Position(box.x, box.y);
//        this.action = action;
//        this.pathLength = length;
//    }


    @Override
    public int hashCode() {
        final int prime = 37;
        int result = 1;
        if (action == null) {
            return result;
        }
        result = prime * result + action.dir1.ordinal();

        if (action.dir2 != null) {
            result = prime * result + action.dir2.ordinal();
        }

        result = prime * result + action.actType.ordinal();
        result = prime * result + agentLocation.x;
        result = prime * result + agentLocation.y;
        result = prime * result + boxLocation.x;
        result = prime * result + boxLocation.y;
        result = prime * result + newLocation.x + newLocation.y;
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        //TODO: Modify below if statement to include where box or goal
        if ( getClass() != obj.getClass() )
            return false;

        if (this.hashCode() == obj.hashCode())
            return true;

        return false;
    }

//    @Override
//    public boolean equals( Object obj ) {
//        if ( this == obj )
//            return true;
//        if ( obj == null )
//            return false;
//        if ( getClass() != obj.getClass() )
//            return false;
//        Node other = (Node) obj;
//        if ( agentCol != other.agentCol )
//            return false;
//        if ( agentRow != other.agentRow )
//            return false;
//        if ( !Arrays.deepEquals( boxes, other.boxes ) ) {
//            return false;
//        }
//        if ( !Arrays.deepEquals( goals, other.goals ) )
//            return false;
//        if ( !Arrays.deepEquals( walls, other.walls ) )
//            return false;
//        return true;
//    }
}
