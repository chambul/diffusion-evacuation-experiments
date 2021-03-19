package io.github.agentsoz.dee.agents;

import io.github.agentsoz.jill.lang.Goal;
import io.github.agentsoz.jill.lang.GoalInfo;


@GoalInfo(hasPlans={
        "io.github.agentsoz.dee.agents.PlanTest",
})
public class GoalTest extends Goal {
    public GoalTest(String name) {
        super(name);
    }
}
