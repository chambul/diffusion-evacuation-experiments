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
import io.github.agentsoz.jill.lang.Agent;
import io.github.agentsoz.jill.lang.Goal;
import io.github.agentsoz.jill.lang.Plan;
import io.github.agentsoz.jill.lang.PlanStep;
import io.github.agentsoz.util.Location;
import io.github.agentsoz.util.PerceptList;

import java.util.Map;


public class PlanEvaluateCurrentContext extends Plan {

    TrafficAgent agent = null;
    PlanStep[] steps = {
            () -> {
//                agent.memorise(TrafficAgent.MemoryEventType.DECIDED.name(), TrafficAgent.MemoryEventValue.EVALUATE.name() +  ":" + getGoal() + "|" + this.getClass().getSimpleName() + "=" + true);

                // get current location (from node of the link): x1,y1
                Location currentLoc = ((Location[]) agent.getQueryPerceptInterface().queryPercept(
                        String.valueOf(agent.getId()), PerceptList.REQUEST_LOCATION, null))[0];


                // get destination (next activity) coords: x2,y2
                double[] destCords =  (double[]) agent.getQueryPerceptInterface().queryPercept(
                        String.valueOf(agent.getId()), PerceptList.REQUEST_DESTINATION_COORDINATES, null);

                if( (destCords[0]==-1 && destCords[1]==-1) ) { //agent is in last activity
                    agent.setTravelPlanCompleted(true);
                    agent.memorise(TrafficAgent.MemoryEventType.BELIEVED.name(),TrafficAgent.MemoryEventValue.MATSIM_PLAN_COMPLETED.name()+ "|" + this.getClass().getSimpleName()+ "=" + true);
                }
                else{
                    Location destination  = new Location("Dest",destCords[0],destCords[1]);

                    for (Blockage blockage: agent.getBlockageList()) { // list cannot be empty if agent selects this plan

                        if(  agent.getTime() - blockage.getLatestInfoReceivedTime() <= agent.getBlockageRecencyThresholdInSeconds() ){ // set to OLD at initialisation
                            blockage.setRecencyOfBlockage(Blockage.recency.RECENT);
                        }

                        double smallestAngleDif = agent.getSmallestAngleBetweenTwoLines(currentLoc,blockage,destination); // test blockage direction wrt to current destination
                        if (smallestAngleDif <= agent.getBlockageAngleThreshold()) { // consider absolute value without - or + direction
                            blockage.setBlockageInCurrentDirection(true); // blockage  in same direction
                            blockage.setNoBlockageImpact(false); // evaluated to find blockage impact
                        }

                        //calculate distance to  blockage
                        agent.calculateAndGetCurrentDistanceToBlockage(blockage);


                    }
                }

            }


    };

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


}
