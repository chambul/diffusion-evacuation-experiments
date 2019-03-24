package io.github.agentsoz.dee.agents.bushfire;

import io.github.agentsoz.dataInterface.DataServer;
import io.github.agentsoz.ees.PerceptList;
import io.github.agentsoz.ees.Run;
import io.github.agentsoz.ees.agents.bushfire.BushfireAgent;
import io.github.agentsoz.jill.lang.AgentInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;

@AgentInfo(hasGoals = {"io.github.agentsoz.ees.agents.bushfire.GoalDoNothing"})
public class BushfireAgentV1 extends BushfireAgent {
    private Prefix prefix = new BushfireAgentV1.Prefix();
    private PrintStream writer = null;
    private  double time;

    private final Logger logger = LoggerFactory.getLogger("io.github.agentsoz.dee");

    enum MemoryEventType {
        BELIEVED,
        PERCEIVED,
        DECIDED,
        ACTIONED
    }


    public BushfireAgentV1(String id) {
        super(id);
    }


    @Override
    public void start(PrintStream writer, String[] params) {

    this.writer = writer;
    super.start(writer, params);

    }



    @Override
    public void handlePercept(String perceptID, Object parameters) {
            if (perceptID == null || perceptID.isEmpty()) {
                return;
            }
            if (perceptID.equals(PerceptList.TIME)) {
                if (parameters instanceof Double) {
                     time = (double) parameters;
                   // log("time is :" + time);
                }
                return;
            }

        // handle percept spread on social network
        handleSocialPercept(perceptID, parameters);



    }

    private void handleSocialPercept(String perceptID, Object parameters) {

        log("memory:" + MemoryEventType.PERCEIVED.name() + ":" + perceptID + ":" +parameters.toString());

        // Spread BLOCKED for given blocked link if haven't already
        if (perceptID.equals(PerceptList.BLOCKED)) {
            String blockedMsg = PerceptList.BLOCKED + parameters.toString();
            shareWithSocialNetwork(blockedMsg);

        }

        //test broadcast messages
        if(getTime() == 40159.0) {
            log("spread test broadcasting");
            broadcastToSocialNetwork("Test-broadcast-msg");
        }

    }

    private void shareWithSocialNetwork(String content) {
        String[] msg = {content, String.valueOf(getId())};
        DataServer.getInstance(Run.DATASERVER).publish(PerceptList.SOCIAL_NETWORK_MSG, msg);
    }

    private void broadcastToSocialNetwork(String content) {
        String[] msg = {content, String.valueOf(getId())};
        DataServer.getInstance(Run.DATASERVER).publish(PerceptList.BROADCAST_MSG, msg);
    }

    private void log(String msg) {
        writer.println(logPrefix()+msg);
    }

    private double getTime(){
        return time;
    }

    String logPrefix() {
        return prefix.toString();
    }

    class Prefix {
        public String toString() {
            return String.format("Time %05.0f Resident %-9s : ", getTime(), getId());
        }
    }

}
