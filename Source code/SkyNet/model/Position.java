package SkyNet.model;

/**
 * client
 * Created by maagaard on 27/05/15.
 * Copyright (c) maagaard 2015.
 */
public class Position {
    public final int x, y, time;

    public Position(int x, int y, int time) {
        this.time = time;
        this.x = x;
        this.y = y;
    }

    public Position(int x, int y) {
        this.time = 0;
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Position p = (Position)obj;
        return (this.x == p.x && this.y == p.y);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + x;
        result = prime * result + y;
        return result;
    }
}
