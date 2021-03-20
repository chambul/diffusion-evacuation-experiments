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
import io.github.agentsoz.ees.Constants;
import io.github.agentsoz.ees.agents.bushfire.BushfireAgent;
import io.github.agentsoz.jill.lang.Agent;
import io.github.agentsoz.jill.lang.Goal;
import io.github.agentsoz.jill.lang.Plan;
import io.github.agentsoz.jill.lang.PlanStep;

import java.util.Map;


public class PlanReroute extends Plan {

    TrafficAgent agent=null;
    Blockage rerouteBlockage = null;
    boolean isReplanning = false;

    public PlanReroute(Agent agent, Goal goal, String name) {
        super(agent, goal, name);
        this.agent=(TrafficAgent) agent;
        body = steps;
    }

    public boolean context() {
        boolean applicable = false;

        for (Blockage blockage: agent.getBlockageList()) {

            if( blockage.getDistToBlockage() <= agent.getDistanceFromTheBlockageThreshold() && blockage.isBlockageInCurrentDirection() == true &&
                    (blockage.isCongestionNearBlockage() == true || blockage.getRecencyOfBlockage() == Blockage.recency.RECENT) ) {
                applicable =  true;
                rerouteBlockage = blockage;
            }

        }

        ((TrafficAgent) getAgent()).memorise(TrafficAgent.MemoryEventType.DECIDED.name(), TrafficAgent.MemoryEventValue.IS_PLAN_APPLICABLE.name()
                + ":" + getGoal() + "|" + this.getClass().getSimpleName() + "=" + applicable);

        return applicable;
    }


    PlanStep[] steps = {
            () -> {
                ((TrafficAgent) getAgent()).memorise(TrafficAgent.MemoryEventType.ACTIONED.name(), TrafficAgent.MemoryEventValue.REROUTE.name() +  ":" + getGoal() + "|" + this.getClass().getSimpleName() + "=" + true);
                isReplanning = agent.replanCurrentDriveTo(Constants.EvacRoutingMode.carGlobalInformation);
                agent.setReroutedOnce(true);

            },
            () -> {
                if (isReplanning) {
                    // Step subsequent to post must suspend agent when waiting for external stimuli
                    // Will be reset by updateAction()
                    agent.suspend(true);
                    // Do not add any checks here since the above call is non-blocking
                    // Suspend will happen once this step is finished
                }
            },
            () -> {
                ((TrafficAgent) getAgent()).memorise(TrafficAgent.MemoryEventType.BELIEVED.name(),
                        TrafficAgent.MemoryEventValue.STATE_CHANGED.name()
                                + ": replanned current route to avoid blockage");
            },



    };

    @Override
    public void setPlanVariables(Map<String, Object> vars) {
    }
}
