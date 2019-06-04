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

    BushfireAgentV1 agent=null;
    Blockage reconsiderBlockage = null; //#FIXME for multiple blockages
    double reconsider_time;

    public PlanReconsiderAgain(Agent agent, Goal goal, String name) {
        super(agent, goal, name);
        this.agent=(BushfireAgentV1) agent;
        body = steps;
    }

    public boolean context() {
        boolean applicable = false;

        for (Blockage blockage: agent.getBlockageList()) {

           if( blockage.getDistToBlockage() > agent.getDistanceFromTheBlockageThreshold() ) { // reconsider time 30mins
               applicable = true;
               reconsiderBlockage = blockage;
               reconsider_time = agent.getReconsiderLaterTime();
           }
           else if (blockage.getDistToBlockage() <= agent.getDistanceFromTheBlockageThreshold() &&  blockage.isBlockageInCurrentDirection() == true && // reconsider time 5mins
                   blockage.isCongestionNearBlockage() ==false && blockage.getRecencyOfBlockage() == Blockage.recency.OLD) {
                applicable = true;
                reconsiderBlockage = blockage;
               reconsider_time = agent.getReconsiderSoonerTime();
            }

              ;
        }

        ((BushfireAgentV1) getAgent()).memorise(BushfireAgentV1.MemoryEventType.DECIDED.name(), BushfireAgentV1.MemoryEventValue.IS_PLAN_APPLICABLE.name()
                + ":" + getGoal() + "|" + this.getClass().getSimpleName() + "=" + applicable);

        return applicable;
    }

    @Override
    public void setPlanVariables(Map<String, Object> vars) {
    }


    PlanStep[] steps = {
            () -> {
                ((BushfireAgentV1) getAgent()).memorise(BushfireAgentV1.MemoryEventType.DECIDED.name(), BushfireAgentV1.MemoryEventValue.RECONSIDER_AGAIN.name() +  ":" + getGoal() + "|" + this.getClass().getSimpleName() + "=" + reconsider_time);
                 reconsiderBlockage.setReconsiderTime(reconsider_time);

            },


    };
}
