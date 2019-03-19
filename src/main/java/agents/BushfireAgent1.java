package agents;

import io.github.agentsoz.ees.agents.bushfire.BushfireAgent;
import io.github.agentsoz.jill.lang.AgentInfo;

@AgentInfo(hasGoals={
        "io.github.agentsoz.abmjill.genact.EnvironmentAction"})
public class BushfireAgent1 extends BushfireAgent {

    private Prefix prefix = new BushfireAgent1.Prefix();

    public BushfireAgent1(String id) {
        super(id);
    }

    class Prefix{
        public String toString() {
            return String.format("Time %05.0f ResidentPartTime %-1s : ",  getId());
        }
    }


    String logPrefix() {
        return prefix.toString();
    }


}
