package io.github.agentsoz.dee.agents.bushfire;

/*-
 * #%L
 * Diffusion Evacuation Experiments
 * %%
 * Copyright (C) 2014 - 2019 by its authors. See AUTHORS file.
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

import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.dataInterface.DataServer;
import io.github.agentsoz.ees.ActionList;
import io.github.agentsoz.ees.PerceptList;
import io.github.agentsoz.ees.Run;
import io.github.agentsoz.ees.agents.bushfire.BushfireAgent;
import io.github.agentsoz.jill.core.beliefbase.BeliefBaseException;
import io.github.agentsoz.jill.core.beliefbase.BeliefSetField;
import io.github.agentsoz.jill.lang.AgentInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.net.SocketOption;

@AgentInfo(hasGoals = {"io.github.agentsoz.ees.agents.bushfire.GoalDoNothing"})
public class BushfireAgentV1 extends BushfireAgent {
    private Prefix prefix = new BushfireAgentV1.Prefix();
    private PrintStream writer = null;
    private  double time;
    private final String memory = "memory";

    private final Logger logger = LoggerFactory.getLogger("io.github.agentsoz.dee");



    enum MemoryEventType {
        BELIEVED,
        PERCEIVED,
        DECIDED,
        ACTIONED
    }

    enum MemoryEventValue {
        LAST_ENV_ACTION_STATE
    }

    public BushfireAgentV1(String id) {
        super(id);
    }


    @Override
    public void start(PrintStream writer, String[] params) {

    this.writer = writer;

        //parseArgs(params);
        // Create a new belief set to store memory
        BeliefSetField[] fields = {
                new BeliefSetField("event", String.class, false),
                new BeliefSetField("value", String.class, false),
        };
        try {
            // Attach this belief set to this agent
            this.createBeliefSet(memory, fields);

//            memorise(MemoryEventType.BELIEVED.name(),
//                    MemoryEventValue.DEPENDENTS_INFO.name() + ":" + getDependentInfo() );

        } catch (BeliefBaseException e) {
            throw new RuntimeException(e);
        }
    }



    @Override
    public void handlePercept(String perceptID, Object parameters) {
            if (perceptID == null || perceptID.isEmpty()) {
                return;
            }
            if (perceptID.equals(PerceptList.TIME)) {
                if (parameters instanceof Double) {
                     time = (double) parameters;
                    log("time is :" + time);
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

    /**
     * Called by the Jill model with the status of a BDI action previously
     * posted by this agent to the ABM environment.
     */
    @Override
    public void updateAction(String actionID, ActionContent content) {
        logger.debug("{} received action update: {}", logPrefix(), content);
        setLastEnvironmentActionState(content.getState()); // save the finish state of the action
        if (content.getAction_type().equals(ActionList.DRIVETO)) {
            ActionContent.State actionState = content.getState();
            if (actionState == ActionContent.State.PASSED ||
                    actionState == ActionContent.State.FAILED ||
                    actionState == ActionContent.State.DROPPED) {
                memorise(MemoryEventType.BELIEVED.name(), MemoryEventValue.LAST_ENV_ACTION_STATE.name() + "=" + actionState.name());
//                setActiveEnvironmentAction(null); // remove the action
                // Wake up the agent that was waiting for external action to finish
                // FIXME: BDI actions put agent in suspend, which won't work for multiple intention stacks
                suspend(false);
            }
        }
    }

    void memorise(String event, String data) {
        try {
            addBelief(memory, event, data);
            log("memory:" + event + ":" + data);
        } catch (BeliefBaseException e) {
            throw new RuntimeException(e);
        }
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
