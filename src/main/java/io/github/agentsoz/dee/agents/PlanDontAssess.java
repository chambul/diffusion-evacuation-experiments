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

import io.github.agentsoz.jill.lang.Agent;
import io.github.agentsoz.jill.lang.Goal;
import io.github.agentsoz.jill.lang.Plan;
import io.github.agentsoz.jill.lang.PlanStep;

import java.util.Map;


public class PlanDontAssess extends Plan {


    public PlanDontAssess(Agent agent, Goal goal, String name) {
        super(agent, goal, name);
        body = steps;
    }

    public boolean context() {
        boolean applicable =  ((TrafficAgent) getAgent()).getBlockageList().isEmpty();

        if(applicable){
            ((TrafficAgent) getAgent()).memorise(TrafficAgent.MemoryEventType.DECIDED.name(), TrafficAgent.MemoryEventValue.IS_PLAN_APPLICABLE.name()
                    + ":" + getGoal() + "|" + this.getClass().getSimpleName() + "=" + applicable);
        }

        return applicable;
    }

    @Override
    public void setPlanVariables(Map<String, Object> vars) {
    }


    PlanStep[] steps = {
            () -> {
                ((TrafficAgent) getAgent()).memorise(TrafficAgent.MemoryEventType.DECIDED.name(), TrafficAgent.MemoryEventValue.DONT_ASSESS.name() +  ":" + getGoal() + "|" + this.getClass().getSimpleName() + "=" + true);
                    ((TrafficAgent) getAgent()).setAssessSituation(false);
            },


    };
}
