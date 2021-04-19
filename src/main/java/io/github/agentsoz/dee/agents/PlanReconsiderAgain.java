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

import java.util.Map;


public class PlanReconsiderAgain extends Plan {

    TrafficAgent agent=null;
    Blockage reconsiderBlockage = null; //#FIXME for multiple blockages
    double reconsider_time;

    public PlanReconsiderAgain(Agent agent, Goal goal, String name) {
        super(agent, goal, name);
        this.agent=(TrafficAgent) agent;
        body = steps;
    }

    public boolean context() {
        boolean applicable = false;

        for (Blockage blockage: agent.getBlockageList()) {

            reconsiderBlockage = blockage;

            if (blockage.getDistToBlockage() <= agent.getDistanceFromTheBlockageThreshold() &&  blockage.isBlockageInCurrentDirection() == true && // reconsider soon
                   blockage.isCongestionNearBlockage() ==false && blockage.getRecencyOfBlockage() == Blockage.recency.OLD) {
               reconsider_time = agent.getReconsiderSoonerTime();
                applicable = true;

            }
           else if( agent.getDistanceFromTheBlockageThreshold() < blockage.getDistToBlockage() && blockage.getDistToBlockage() <= 3*agent.getDistanceFromTheBlockageThreshold() ) { // reconsider in a while
               reconsider_time = agent.getReconsiderRegularTime();
                applicable = true;

            }
           else if (blockage.getDistToBlockage() > 3*agent.getDistanceFromTheBlockageThreshold()){ // reconsider later
               reconsider_time = agent.getReconsiderLaterTime();
                applicable = true;

            }

        }

        if(applicable){
            ((TrafficAgent) getAgent()).memorise(TrafficAgent.MemoryEventType.DECIDED.name(), TrafficAgent.MemoryEventValue.IS_PLAN_APPLICABLE.name()
                    + ":" + this.getClass().getSimpleName() + "=" + applicable);
        }

        return applicable;
    }

    @Override
    public void setPlanVariables(Map<String, Object> vars) {
    }


    PlanStep[] steps = {
            () -> {
                ((TrafficAgent) getAgent()).memorise(TrafficAgent.MemoryEventType.DECIDED.name(), TrafficAgent.MemoryEventValue.RECONSIDER_AGAIN.name() +  ":" + getGoal() + "| reconsider in " + reconsider_time + "seconds");
                 reconsiderBlockage.setReconsiderTime(reconsider_time);

            },


    };
}
