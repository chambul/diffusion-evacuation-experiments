package io.github.agentsoz.dee.agents;

/*-
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2018 by its authors. See AUTHORS file.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import io.github.agentsoz.dee.blockage.Blockage;
import io.github.agentsoz.ees.PerceptList;
import io.github.agentsoz.jill.lang.Agent;
import io.github.agentsoz.jill.lang.Goal;
import io.github.agentsoz.jill.lang.Plan;
import io.github.agentsoz.jill.lang.PlanStep;
import io.github.agentsoz.util.Location;

import java.util.Map;


public class PlanEvaluateCurrentContext extends Plan {

    TrafficAgent agent = null;

    public PlanEvaluateCurrentContext(Agent agent, Goal goal, String name) {
        super(agent, goal, name);
        this.agent = (TrafficAgent) agent;
        body = steps;
    }

    public boolean context() {

        agent.memorise(TrafficAgent.MemoryEventType.DECIDED.name(), TrafficAgent.MemoryEventValue.IS_PLAN_APPLICABLE.name()
                + ":" + getGoal() + "|" + this.getClass().getSimpleName() + "=" + true);

        return true;
    }

    @Override
    public void setPlanVariables(Map<String, Object> vars) {
    }


    PlanStep[] steps = {
            () -> {
                agent.memorise(TrafficAgent.MemoryEventType.DECIDED.name(), TrafficAgent.MemoryEventValue.EVALUATE.name() +  ":" + getGoal() + "|" + this.getClass().getSimpleName() + "=" + true);

                Location currentLoc = ((Location[]) agent.getQueryPerceptInterface().queryPercept( // get current location: x1,y1
                        String.valueOf(agent.getId()), PerceptList.REQUEST_LOCATION, null))[0];

                //#FIXME need to get current destination cordinates from MATSim side
                Location destLoc = ((Location[]) agent.getQueryPerceptInterface().queryPercept( // get current location: x2,y2
                        String.valueOf(agent.getId()), PerceptList.REQUEST_LOCATION, null))[0];

                // calculate angle of blockage, based on the line connecting current location and destination
                double angle1 = Math.atan2(currentLoc.getY() - destLoc.getY(),  currentLoc.getX() - destLoc.getX());

                for (Blockage blockage: agent.getBlockageList()) {

                    if(  agent.getTime() - blockage.getLatestBlockageInfoTime() <= agent.getBlockageRecencyThreshold() ){ // evaluate recency
                        blockage.setRecencyOfBlockage(Blockage.recency.RECENT);
                    }

                    //evaluate direction of line connecting current location and blockage
                    double angle2 = Math.atan2(currentLoc.getY() - blockage.getY(), currentLoc.getX() - blockage.getX());
                    if(Math.abs(angle1 - angle2) <= agent.getBlockageSameDirectionAnlgeThreshold()) { // abs to consider - or + directoin values
                        blockage.setBlockageInCurrentDirection(true); // blockage  in same direction
                        blockage.setNoBlockageImpact(false); // evaluated to find blockage impact
                    }
                }
            },


    };
// calculate anlge between two lines
// It is declared as double atan2(double y, double x) and converts rectangular coordinates (x,y) to the angle theta from the polar coordinates (r,theta)
//    public static double angleBetween2Lines(Line2D line1, Line2D line2)
//    {
//        double angle1 = Math.atan2(line1.getY1() - line1.getY2(),
//                line1.getX1() - line1.getX2());
//        double angle2 = Math.atan2(line2.getY1() - line2.getY2(),
//                line2.getX1() - line2.getX2());
//        return angle1-angle2;
//    }
}
