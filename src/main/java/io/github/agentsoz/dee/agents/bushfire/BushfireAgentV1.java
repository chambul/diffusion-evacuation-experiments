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

import io.github.agentsoz.abmjill.genact.EnvironmentAction;
import io.github.agentsoz.bdiabm.EnvironmentActionInterface;
import io.github.agentsoz.bdiabm.QueryPerceptInterface;
import io.github.agentsoz.bdiabm.data.ActionContent;
import io.github.agentsoz.dataInterface.DataServer;
import io.github.agentsoz.ees.ActionList;
import io.github.agentsoz.ees.EmergencyMessage;
import io.github.agentsoz.ees.PerceptList;
import io.github.agentsoz.ees.Run;
import io.github.agentsoz.ees.agents.bushfire.BushfireAgent;
import io.github.agentsoz.ees.matsim.MATSimEvacModel;
import io.github.agentsoz.jill.core.beliefbase.BeliefBaseException;
import io.github.agentsoz.jill.core.beliefbase.BeliefSetField;
import io.github.agentsoz.jill.lang.AgentInfo;
import io.github.agentsoz.util.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@AgentInfo(hasGoals = {"io.github.agentsoz.ees.agents.bushfire.GoalActNow","io.github.agentsoz.abmjill.genact.EnvironmentAction"})
public class BushfireAgentV1 extends BushfireAgent {


    private final Logger logger = LoggerFactory.getLogger("io.github.agentsoz.dee");

    private PrintStream writer = null;
    private QueryPerceptInterface queryInterface;
    private EnvironmentActionInterface envActionInterface;
    private double time = -1;
    private BushfireAgentV1.Prefix prefix = new BushfireAgentV1.Prefix();

    //defaults

    enum MemoryEventType {
        BELIEVED,
        PERCEIVED,
        DECIDED,
        ACTIONED
    }

    enum MemoryEventValue {
        DONE_FOR_NOW,
        IS_PLAN_APPLICABLE,
        GOTO_LOCATION,
        LAST_ENV_ACTION_STATE
    }

    //internal variables
    private final String memory = "memory";
    private Map<String,Location> locations;
    private EnvironmentAction activeEnvironmentAction;
    private ActionContent.State lastEnvironmentActionStatus;
    private Set<String> messagesShared;

    public BushfireAgentV1(String id) {
        super(id);
        locations = new HashMap<>();
        messagesShared = new HashSet<>();
    }

    public Map<String, Location> getLocations() {
        return locations;
    }

    public void setLocations(Map<String, Location> locations) {
        this.locations = locations;
    }

//    boolean getWillGoHomeAfterVisitingDependents()
//    {
//        return willGoHomeAfterVisitingDependents;
//    }
//    boolean getWillGoHomeBeforeLeaving()
//    {
//        return willGoHomeBeforeLeaving;
//    }

    boolean isDriving() {
        return activeEnvironmentAction != null && activeEnvironmentAction.getActionID().equals(ActionList.DRIVETO);
    }

    void setActiveEnvironmentAction(EnvironmentAction activeEnvironmentAction) {
        this.activeEnvironmentAction = activeEnvironmentAction;
    }

    public void setLastEnvironmentActionState(ActionContent.State lastEnvironmentActionStatus) {
        this.lastEnvironmentActionStatus = lastEnvironmentActionStatus;
    }

    public ActionContent.State getLastEnvironmentActionState() {
        return lastEnvironmentActionStatus;
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

    /**
     * Called by the Jill model when terminating
     */
    @Override
    public void finish() {
    }

    @Override
    public void handlePercept(String perceptID, Object parameters) {

        // save it to memory
        memorise(MemoryEventType.PERCEIVED.name(), perceptID + ":" +parameters.toString());


        if (perceptID == null || perceptID.isEmpty()) {
            return;
        }
        else if (perceptID.equals(PerceptList.TIME)) {
            if (parameters instanceof Double) {
                time = (double) parameters;
            }
            return;
        }
//        else if (perceptID.equals(PerceptList.FIELD_OF_VIEW)) {
//          //  updateResponseBarometerFieldOfViewPercept(parameters);
//            if (PerceptList.SIGHTED_FIRE.equalsIgnoreCase(parameters.toString())) {
//                handleFireVisual();
//            }
        else if (perceptID.equals(PerceptList.ARRIVED)) {
            // do something
        }
        else if (perceptID.equals(PerceptList.BLOCKED)) {
            if (activeEnvironmentAction == null) {
                replanCurrentDriveTo(MATSimEvacModel.EvacRoutingMode.carGlobalInformation); //FIXME change routing mode to FreeSpeed
            }
        }

        // handle percept spread on social network
        handleSocialPercept(perceptID, parameters);

        // Now trigger a response as needed
//        checkBarometersAndTriggerResponseAsNeeded();

    }

    private void handleSocialPercept(String perceptID, Object parameters) {


        // Spread EVACUATE_NOW if haven't done so already
        if (perceptID.equals(PerceptList.EMERGENCY_MESSAGE) &&
                !messagesShared.contains(EmergencyMessage.EmergencyMessageType.EVACUATE_NOW.name()) &&
                parameters instanceof String &&
                getEmergencyMessageType(parameters) == EmergencyMessage.EmergencyMessageType.EVACUATE_NOW) {
            shareWithSocialNetwork((String) parameters);
            messagesShared.add(getEmergencyMessageType(parameters).name());
        }
        // Spread BLOCKED for given blocked link if haven't already
        if (perceptID.equals(PerceptList.BLOCKED)) {
            String blockedMsg = PerceptList.BLOCKED + parameters.toString();
            if (!messagesShared.contains(blockedMsg)) {
                shareWithSocialNetwork(blockedMsg);
                messagesShared.add(blockedMsg);
            }
        }

        //test broadcast messages //FIXME remove after social network broadcasting is tested fully
        if (getTime() == 40159.0) {
            log("spread test broadcasting");
            broadcastToSocialNetwork("Test-broadcast-msg");
        }

    }

//    private void handleFireVisual() {
//        // Always replan when we see fire
//        replanCurrentDriveTo(MATSimEvacModel.EvacRoutingMode.carGlobalInformation);
//    }


    void memorise(String event, String data) {
        try {
            addBelief(memory, event, data);
            log("memory:" + event + ":" + data);
        } catch (BeliefBaseException e) {
            throw new RuntimeException(e);
        }
    }

    boolean startDrivingTo(Location location, MATSimEvacModel.EvacRoutingMode routingMode) {
        if (location == null) return false;
        memorise(MemoryEventType.DECIDED.name(), MemoryEventValue.GOTO_LOCATION.name() + ":" + location.toString());
        double distToTravel = getTravelDistanceTo(location);
        if (distToTravel == 0.0) {
            // already there, so no need to drive
            return false;
        }
        Object[] params = new Object[4];
        params[0] = ActionList.DRIVETO;
        params[1] = location.getCoordinates();
        params[2] = getTime() + 5.0; // five secs from now;
        params[3] = routingMode;
        memorise(MemoryEventType.ACTIONED.name(), ActionList.DRIVETO
                + ":"+ location + ":" + String.format("%.0f", distToTravel) + "m away");
        EnvironmentAction action = new EnvironmentAction(Integer.toString(getId()), ActionList.DRIVETO, params);
        setActiveEnvironmentAction(action); // will be reset by updateAction()
        subgoal(action); // should be last call in any plan step
        return true;
    }

    boolean replanCurrentDriveTo(MATSimEvacModel.EvacRoutingMode routingMode) {
        memorise(MemoryEventType.ACTIONED.name(), ActionList.REPLAN_CURRENT_DRIVETO);
        EnvironmentAction action = new EnvironmentAction(
                Integer.toString(getId()),
                ActionList.REPLAN_CURRENT_DRIVETO,
                new Object[] {routingMode});
        setActiveEnvironmentAction(action); // will be reset by updateAction()
        subgoal(action); // should be last call in any plan step
        return true;
    }


    double getTravelDistanceTo(Location location) {
        return (double) getQueryPerceptInterface().queryPercept(
                String.valueOf(getId()),
                PerceptList.REQUEST_DRIVING_DISTANCE_TO,
                location.getCoordinates());
    }

    private EmergencyMessage.EmergencyMessageType getEmergencyMessageType(Object msg) {
        if (msg == null || !(msg instanceof String)) {
            return null;
        }
        String[] tokens = ((String) msg).split(",");
        EmergencyMessage.EmergencyMessageType type = EmergencyMessage.EmergencyMessageType.valueOf(tokens[0]);
        return type;
    }

    private void shareWithSocialNetwork(String content) {
        String[] msg = {content, String.valueOf(getId())};
        memorise(MemoryEventType.ACTIONED.name(), PerceptList.SOCIAL_NETWORK_MSG
                + ":" + content);
        DataServer.getInstance(Run.DATASERVER).publish(PerceptList.SOCIAL_NETWORK_MSG, msg);
    }

    private void broadcastToSocialNetwork(String content) {
        String[] msg = {content, String.valueOf(getId())};
        memorise(MemoryEventType.ACTIONED.name(), PerceptList.SOCIAL_NETWORK_BROADCAST_MSG
                + ":" + content);
        DataServer.getInstance(Run.DATASERVER).publish(PerceptList.SOCIAL_NETWORK_BROADCAST_MSG, msg);
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
                setActiveEnvironmentAction(null); // remove the action
                // Wake up the agent that was waiting for external action to finish
                // FIXME: BDI actions put agent in suspend, which won't work for multiple intention stacks
                suspend(false);
            }
        }
    }


    @Override
    public void setQueryPerceptInterface(QueryPerceptInterface queryInterface) {
        this.queryInterface = queryInterface;
    }

    @Override
    public QueryPerceptInterface getQueryPerceptInterface() {
        return queryInterface;
    }

    public void setEnvironmentActionInterface(EnvironmentActionInterface envActInterface) {
        this.envActionInterface = envActInterface;
    }

    public EnvironmentActionInterface getEnvironmentActionInterface() {
        return envActionInterface;
    }

    class Prefix {
        public String toString() {
            return String.format("Time %05.0f Resident %-9s : ", getTime(), getId());
        }
    }

    String logPrefix() {
        return prefix.toString();
    }


    private void log(String msg) {
        writer.println(logPrefix() + msg);
    }

    private double getTime() {
        return time;
    }




}