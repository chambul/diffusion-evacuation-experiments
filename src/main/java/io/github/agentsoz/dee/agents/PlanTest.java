package io.github.agentsoz.dee.agents;

import io.github.agentsoz.jill.lang.Agent;
import io.github.agentsoz.jill.lang.Goal;
import io.github.agentsoz.jill.lang.Plan;
import io.github.agentsoz.jill.lang.PlanStep;

import java.util.Map;

public class PlanTest extends Plan{


    public PlanTest(Agent agent, Goal goal, String name) {
        super(agent, goal, name);
        body = steps;
    }

    public boolean context() {

        ((TrafficAgent)getAgent()).memorise(TrafficAgent.MemoryEventType.DECIDED.name(), TrafficAgent.MemoryEventValue.IS_PLAN_APPLICABLE.name()
                + ":" + getGoal() + "|" + this.getClass().getSimpleName() + "=" + true);
        return true;
    }

    PlanStep[] steps = {
            () -> {
                subgoal(new GoalReplanToDestination(" sub goal ReplanToDest"));
            },
    };

    @Override
    public void setPlanVariables(Map<String, Object> vars) {
    }
}