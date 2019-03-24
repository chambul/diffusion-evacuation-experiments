package io.github.agentsoz.dee.agents.bushfire;

/*-
 * #%L
 * Emergency Evacuation Simulator
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
import io.github.agentsoz.ees.matsim.MATSimEvacModel;
import io.github.agentsoz.jill.core.beliefbase.BeliefBaseException;
import io.github.agentsoz.jill.core.beliefbase.BeliefSetField;
import io.github.agentsoz.jill.lang.Agent;
import io.github.agentsoz.jill.lang.AgentInfo;
import io.github.agentsoz.util.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@AgentInfo(hasGoals = {})
public class BushfireAgentV3 extends Agent implements io.github.agentsoz.bdiabm.Agent {

    private final Logger logger = LoggerFactory.getLogger("io.github.agentsoz.dee");

    static final String LOCATION_HOME = "home";
    static final String LOCATION_EVAC_PREFERRED = "evac";
    static final String LOCATION_INVAC_PREFERRED = "invac";

    private PrintStream writer = null;
    private QueryPerceptInterface queryInterface;
    private EnvironmentActionInterface envActionInterface;
    private double time = -1;
    private BushfireAgentV3.Prefix prefix = new BushfireAgentV3.Prefix();

    // Defaults
//    private DependentInfo dependentInfo = null;
    private double initialResponseThreshold = 0.5;
    private double finalResponseThreshold = 0.5;
    private double responseBarometerMessages = 0.0;
    private double responseBarometerFieldOfView = 0.0;
    private double responseBarometerSocialMessage = 0.0;
    private boolean sharesInfoWithSocialNetwork = false;
    //    private boolean willGoHomeAfterVisitingDependents = false;
    private boolean willGoHomeBeforeLeaving = false;
    private double smokeVisualValue = 0.3;
    private double fireVisualValue = 1.0;
    private double socialMessageEvacNowValue = 0.3;


    enum MemoryEventType {
        BELIEVED,
        PERCEIVED,
        DECIDED,
        ACTIONED
    }

    enum MemoryEventValue {
        DEPENDENTS_INFO,
        DONE_FOR_NOW,
        IS_PLAN_APPLICABLE,
        STATE_CHANGED,
        RESPONSE_BAROMETER_MESSAGES_CHANGED,
        RESPONSE_BAROMETER_FIELD_OF_VIEW_CHANGED,
        RESPONSE_BAROMETER_SOCIAL_MESSAGE_CHANGED,
        INITIAL_RESPONSE_THRESHOLD_BREACHED,
        FINAL_RESPONSE_THRESHOLD_BREACHED,
        INITIAL_AND_FINAL_RESPONSE_THRESHOLDS_BREACHED_TOGETHER,
        TRIGGER_INITIAL_RESPONSE_NOW,
        TRIGGER_FINAL_RESPONSE_NOW,
        GOTO_LOCATION,
        DISTANCE_TO_LOCATION,
        SAFE,
        LAST_ENV_ACTION_STATE,
        SOCIAL_NETWORK_MESSAGE,
    }

    // Internal variables
    private final String memory = "memory";
    private Map<String, Location> locations;
    private EnvironmentAction activeEnvironmentAction;
    private ActionContent.State lastEnvironmentActionStatus;
    private Set<String> messagesShared;


    public BushfireAgentV3(String id) {
        super(id);
        locations = new HashMap<>();
        messagesShared = new HashSet<>();

    }

    // DependentInfo getDependentInfo() {
//        return dependentInfo;
//    }

    public Map<String, Location> getLocations() {
        return locations;
    }

    public void setLocations(Map<String, Location> locations) {
        this.locations = locations;
    }

    double getResponseBarometer() {
        return responseBarometerMessages + responseBarometerFieldOfView + responseBarometerSocialMessage;
    }

    //    boolean getWillGoHomeAfterVisitingDependents()
//    {
//        return willGoHomeAfterVisitingDependents;
//    }
    boolean getWillGoHomeBeforeLeaving() {
        return willGoHomeBeforeLeaving;
    }

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

    /**
     * Called by the Jill model when starting a new agent.
     * There is no separate initialisation call prior to this, so all
     * agent initialisation should be done here (using params).
     */
    @Override
    public void start(PrintStream writer, String[] params) {
        this.writer = writer;
        parseArgs(params);
        // Create a new belief set to store memory
        BeliefSetField[] fields = {
                new BeliefSetField("event", String.class, false),
                new BeliefSetField("value", String.class, false),
        };
        try {
            // Attach this belief set to this agent
            this.createBeliefSet(memory, fields);

//            memorise(io.github.agentsoz.dee.agents.bushfire.BushfireAgentV1.MemoryEventType.BELIEVED.name(),
//                    io.github.agentsoz.dee.agents.bushfire.BushfireAgentV1.MemoryEventValue.DEPENDENTS_INFO.name() + ":" + getDependentInfo() );

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

    /**
     * Called by the Jill model with the status of a BDI percept
     * for this agent, coming from the ABM environment.
     */
    @Override
    public void handlePercept(String perceptID, Object parameters) {
        if (perceptID == null || perceptID.isEmpty()) {
            return;
        }
        if (perceptID.equals(PerceptList.TIME)) {
            if (parameters instanceof Double) {
                time = (double) parameters;
            }
            return;
        }

        // save it to memory
        memorise(io.github.agentsoz.dee.agents.bushfire.BushfireAgentV3.MemoryEventType.PERCEIVED.name(), perceptID + ":" + parameters.toString());

        if (perceptID.equals(PerceptList.EMERGENCY_MESSAGE)) {
            // updateResponseBarometerMessages(parameters);
        } else if (perceptID.equals(PerceptList.SOCIAL_NETWORK_MSG)) {
            //  updateResponseBarometerSocialMessage(parameters);
        } else if (perceptID.equals(PerceptList.FIELD_OF_VIEW)) {
            //  updateResponseBarometerFieldOfViewPercept(parameters);
            if (PerceptList.SIGHTED_FIRE.equalsIgnoreCase(parameters.toString())) {
                //      handleFireVisual();
            }
        } else if (perceptID.equals(PerceptList.ARRIVED)) {
            // do something
        } else if (perceptID.equals(PerceptList.BLOCKED)) {
            if (activeEnvironmentAction == null) {
                replanCurrentDriveTo(MATSimEvacModel.EvacRoutingMode.carGlobalInformation);
            }
        }

        // handle percept spread on social network
        handleSocialPercept(perceptID, parameters);

        // Now trigger a response as needed
        //      checkBarometersAndTriggerResponseAsNeeded();
    }

    private void handleSocialPercept(String perceptID, Object parameters) {
        // Nothing to do if this is not an agent who shares with the social networks
        if (!sharesInfoWithSocialNetwork) {
            return;
        }
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
    }

    private void handleFireVisual() {
        // Always replan when we see fire
        replanCurrentDriveTo(MATSimEvacModel.EvacRoutingMode.carGlobalInformation);
    }

    protected void checkBarometersAndTriggerResponseAsNeeded() {
        try {
            // Nothing to do if already triggered responses once
            io.github.agentsoz.dee.agents.bushfire.BushfireAgentV3.MemoryEventValue breach = null;
            if (!eval("memory.value = " + io.github.agentsoz.dee.agents.bushfire.BushfireAgentV3.MemoryEventValue.INITIAL_RESPONSE_THRESHOLD_BREACHED.name())) {
                // initial response threshold not breached yet
                if (isInitialResponseThresholdBreached()) {
                    // initial response threshold breached for the first time
                    memorise(io.github.agentsoz.dee.agents.bushfire.BushfireAgentV3.MemoryEventType.DECIDED.name(), io.github.agentsoz.dee.agents.bushfire.BushfireAgentV3.MemoryEventValue.INITIAL_RESPONSE_THRESHOLD_BREACHED.name());
                    breach = io.github.agentsoz.dee.agents.bushfire.BushfireAgentV3.MemoryEventValue.INITIAL_RESPONSE_THRESHOLD_BREACHED;
                }
            }
            if (!eval("memory.value = " + io.github.agentsoz.dee.agents.bushfire.BushfireAgentV3.MemoryEventValue.FINAL_RESPONSE_THRESHOLD_BREACHED.name())) {
                // final response threshold not breached yet
                if (isFinalResponseThresholdBreached()) {
                    // final response threshold breached for the first time
                    memorise(io.github.agentsoz.dee.agents.bushfire.BushfireAgentV3.MemoryEventType.DECIDED.name(), io.github.agentsoz.dee.agents.bushfire.BushfireAgentV3.MemoryEventValue.FINAL_RESPONSE_THRESHOLD_BREACHED.name());
                    if (!isInitialResponseThresholdBreached()) {
                        // final breached bu not initial, so force initial breach now as well
                        logger.warn("{} had final threshold breached but not initial; will assume both have breached", logPrefix());
                        memorise(io.github.agentsoz.dee.agents.bushfire.BushfireAgentV3.MemoryEventType.DECIDED.name(), io.github.agentsoz.dee.agents.bushfire.BushfireAgentV3.MemoryEventValue.INITIAL_AND_FINAL_RESPONSE_THRESHOLDS_BREACHED_TOGETHER.name());
                        breach = io.github.agentsoz.dee.agents.bushfire.BushfireAgentV3.MemoryEventValue.INITIAL_AND_FINAL_RESPONSE_THRESHOLDS_BREACHED_TOGETHER;
                    } else if (breach == null) {
                        // only final response breached just now, not initial
                        breach = io.github.agentsoz.dee.agents.bushfire.BushfireAgentV3.MemoryEventValue.FINAL_RESPONSE_THRESHOLD_BREACHED;
                    } else {
                        // both thresholds breached together
                        memorise(io.github.agentsoz.dee.agents.bushfire.BushfireAgentV3.MemoryEventType.DECIDED.name(), io.github.agentsoz.dee.agents.bushfire.BushfireAgentV3.MemoryEventValue.INITIAL_AND_FINAL_RESPONSE_THRESHOLDS_BREACHED_TOGETHER.name());
                        breach = io.github.agentsoz.dee.agents.bushfire.BushfireAgentV3.MemoryEventValue.INITIAL_AND_FINAL_RESPONSE_THRESHOLDS_BREACHED_TOGETHER;
                    }
                }
            }
            if (breach != null) {
                //  triggerResponse(breach);
            }

        } catch (BeliefBaseException e) {
            throw new RuntimeException(e);
        }
    }

    boolean isInitialResponseThresholdBreached() {
        return (getResponseBarometer() >= initialResponseThreshold);
    }

    boolean isFinalResponseThresholdBreached() {
        return (getResponseBarometer() >= finalResponseThreshold);
    }

    void memorise(String event, String data) {
        try {
            addBelief(memory, event, data);
            log("memory :" + event + ":" + data);
        } catch (BeliefBaseException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Called after a new percept has been processed
     * @param breach
     */
//    private void triggerResponse(io.github.agentsoz.dee.agents.bushfire.BushfireAgentV1.MemoryEventValue breach) {
//        if (breach == io.github.agentsoz.dee.agents.bushfire.BushfireAgentV1.MemoryEventValue.INITIAL_AND_FINAL_RESPONSE_THRESHOLDS_BREACHED_TOGETHER) {
//            memorise(io.github.agentsoz.dee.agents.bushfire.BushfireAgentV1.MemoryEventType.DECIDED.name(), io.github.agentsoz.dee.agents.bushfire.BushfireAgentV1.MemoryEventValue.TRIGGER_INITIAL_RESPONSE_NOW.name());
//            memorise(io.github.agentsoz.dee.agents.bushfire.BushfireAgentV1.MemoryEventType.DECIDED.name(), io.github.agentsoz.dee.agents.bushfire.BushfireAgentV1.MemoryEventValue.TRIGGER_FINAL_RESPONSE_NOW.name());
//            // CAREFUL: Must use LIFO when pushing multiple goals that should be executed sequentially
//            // onto the Jill stack
//            post(new GoalActNow("ActNow")); // 1. will execute second
//            post(new GoalInitialResponse("InitialResponse")); // 2. will execute first
//
//        } else if (breach == io.github.agentsoz.dee.agents.bushfire.BushfireAgentV1.MemoryEventValue.INITIAL_RESPONSE_THRESHOLD_BREACHED) {
//            memorise(io.github.agentsoz.dee.agents.bushfire.BushfireAgentV1.MemoryEventType.DECIDED.name(), io.github.agentsoz.dee.agents.bushfire.BushfireAgentV1.MemoryEventValue.TRIGGER_INITIAL_RESPONSE_NOW.name());
//            post(new GoalInitialResponse("InitialResponse"));
//
//        } else if (breach == io.github.agentsoz.dee.agents.bushfire.BushfireAgentV1.MemoryEventValue.FINAL_RESPONSE_THRESHOLD_BREACHED) {
//            memorise(io.github.agentsoz.dee.agents.bushfire.BushfireAgentV1.MemoryEventType.DECIDED.name(), io.github.agentsoz.dee.agents.bushfire.BushfireAgentV1.MemoryEventValue.TRIGGER_FINAL_RESPONSE_NOW.name());
//            post(new GoalActNow("ActNow"));
//        }
//    }

    /**
     * Writes {@link #responseBarometerSocialMessage} with value of the incoming msg
     *
     * @param msg the incoming social network message
     */
    private void updateResponseBarometerSocialMessage(Object msg) {
        double value = 0.0;
        if (!messagesShared.contains(EmergencyMessage.EmergencyMessageType.EVACUATE_NOW.name())
                && getEmergencyMessageType(msg) == EmergencyMessage.EmergencyMessageType.EVACUATE_NOW) {
            // The EVAC_NOW message has value if we haven't shared it before
            value = socialMessageEvacNowValue;
        } // else if (...) {}
        if (value > responseBarometerSocialMessage) {
            responseBarometerSocialMessage = value;
            memorise(io.github.agentsoz.dee.agents.bushfire.BushfireAgentV3.MemoryEventType.BELIEVED.name(), io.github.agentsoz.dee.agents.bushfire.BushfireAgentV3.MemoryEventValue.RESPONSE_BAROMETER_SOCIAL_MESSAGE_CHANGED.name() + "=" + value);
        }
    }

    /**
     * Overwrites {@link #responseBarometerFieldOfView} with the value of the incoming visual
     * if the incoming value is higher.
     *
     * @param view the incoming visual percept
     */
    private void updateResponseBarometerFieldOfViewPercept(Object view) {
        if (view == null) {
            return;
        }
        double value = 0.0;
        if (PerceptList.SIGHTED_EMBERS.equalsIgnoreCase(view.toString())) {
            value = smokeVisualValue;
        } else if (PerceptList.SIGHTED_FIRE.equalsIgnoreCase(view.toString())) {
            value = fireVisualValue;
        } else {
            logger.warn("{} ignoring field of view percept: {}", logPrefix(), view);
            return;
        }
        if (value > responseBarometerFieldOfView) {
            responseBarometerFieldOfView = value;
            memorise(io.github.agentsoz.dee.agents.bushfire.BushfireAgentV3.MemoryEventType.BELIEVED.name(), io.github.agentsoz.dee.agents.bushfire.BushfireAgentV3.MemoryEventValue.RESPONSE_BAROMETER_FIELD_OF_VIEW_CHANGED.name() + "=" + value);
        }
    }

    /**
     * Overwrites {@link #responseBarometerMessages} with the value of the incoming message
     * if the incoming value is higher.
     *
     * @param msg the incoming emergency message
     */
    private void updateResponseBarometerMessages(Object msg) {
        if (msg == null || !(msg instanceof String)) {
            return;
        }
        String[] tokens = ((String) msg).split(",");
        EmergencyMessage.EmergencyMessageType type = EmergencyMessage.EmergencyMessageType.valueOf(tokens[0]);
        try {
            String loc = tokens[1];
            double[] coords = new double[]{Double.parseDouble(tokens[2]), Double.parseDouble(tokens[3])};
            locations.put(LOCATION_EVAC_PREFERRED, new Location(loc, coords[0], coords[1]));
            memorise(io.github.agentsoz.dee.agents.bushfire.BushfireAgentV3.MemoryEventType.BELIEVED.name(), LOCATION_EVAC_PREFERRED + "=" + locations.get(LOCATION_EVAC_PREFERRED));
        } catch (Exception e) {
        }
        double value = type.getValue();
        // Allow the barometer to go down as well if the intensity of the situation (message) is reduced
        //if (value > responseBarometerMessages) {
        responseBarometerMessages = value;
        memorise(io.github.agentsoz.dee.agents.bushfire.BushfireAgentV3.MemoryEventType.BELIEVED.name(), io.github.agentsoz.dee.agents.bushfire.BushfireAgentV3.MemoryEventValue.RESPONSE_BAROMETER_MESSAGES_CHANGED.name() + "=" + value);
        //}
    }

    boolean startDrivingTo(Location location, MATSimEvacModel.EvacRoutingMode routingMode) {
        if (location == null) return false;
        memorise(io.github.agentsoz.dee.agents.bushfire.BushfireAgentV3.MemoryEventType.DECIDED.name(), io.github.agentsoz.dee.agents.bushfire.BushfireAgentV3.MemoryEventValue.GOTO_LOCATION.name() + ":" + location.toString());
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
        memorise(io.github.agentsoz.dee.agents.bushfire.BushfireAgentV3.MemoryEventType.ACTIONED.name(), ActionList.DRIVETO
                + ":" + location + ":" + String.format("%.0f", distToTravel) + "m away");
        EnvironmentAction action = new EnvironmentAction(Integer.toString(getId()), ActionList.DRIVETO, params);
        setActiveEnvironmentAction(action); // will be reset by updateAction()
        subgoal(action); // should be last call in any plan step
        return true;
    }

    boolean replanCurrentDriveTo(MATSimEvacModel.EvacRoutingMode routingMode) {
        memorise(io.github.agentsoz.dee.agents.bushfire.BushfireAgentV3.MemoryEventType.ACTIONED.name(), ActionList.REPLAN_CURRENT_DRIVETO);
        EnvironmentAction action = new EnvironmentAction(
                Integer.toString(getId()),
                ActionList.REPLAN_CURRENT_DRIVETO,
                new Object[]{routingMode});
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
        memorise(io.github.agentsoz.dee.agents.bushfire.BushfireAgentV3.MemoryEventType.ACTIONED.name(), PerceptList.SOCIAL_NETWORK_MSG
                + ":" + content);
        DataServer.getInstance(Run.DATASERVER).publish(PerceptList.SOCIAL_NETWORK_MSG, msg);
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
                memorise(io.github.agentsoz.dee.agents.bushfire.BushfireAgentV3.MemoryEventType.BELIEVED.name(), io.github.agentsoz.dee.agents.bushfire.BushfireAgentV3.MemoryEventValue.LAST_ENV_ACTION_STATE.name() + "=" + actionState.name());
                setActiveEnvironmentAction(null); // remove the action
                // Wake up the agent that was waiting for external action to finish
                // FIXME: BDI actions put agent in suspend, which won't work for multiple intention stacks
                suspend(false);
            }
        }
    }

    /**
     * BDI-ABM agent init function; Not used by Jill.
     * Use {@link #start(PrintStream, String[])} instead
     * to perform any agent specific initialisation.
     */
    @Override
    public void init(String[] args) {
        parseArgs(args);
    }

    /**
     * BDI-ABM agent start function; Not used by Jill.
     * Use {@link #start(PrintStream, String[])} instead
     * to perform agent startup.
     */
    @Override
    public void start() {
        logger.warn("{} using a stub for io.github.agentsoz.bdiabm.Agent.start()", logPrefix());
    }

    /**
     * BDI-ABM agent kill function; Not used by Jill.
     * Use {@link #finish()} instead
     * to perform agent termination.
     */

    @Override
    public void kill() {
        logger.warn("{} using a stub for io.github.agentsoz.bdiabm.Agent.kill()", logPrefix());
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


    double getTime() {
        return time;
    }

    private void parseArgs(String[] args) {
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "HasDependentsAtLocation":
                        if (i + 1 < args.length) {
                            i++;
//                            if (!args[i].isEmpty()) { // empty arg is ok, signifies no dependents
//                                try {
//                                    String[] vals = args[i].split(",");
//                                    Location location = new Location("Dependent", Double.parseDouble(vals[0]), Double.parseDouble(vals[1]));
//                                    dependentInfo = new DependentInfo();
//                                    dependentInfo.setLocation(location);
//                                } catch (Exception e) {
//                                    System.err.println("Could not parse dependent's location '"
//                                            + args[i] + "' : " + e.getMessage());
//                                }
//                            }
                        }
                        break;
                    case "InitialResponseThreshold":
                        if (i + 1 < args.length) {
                            i++;
                            try {
                                initialResponseThreshold = Double.parseDouble(args[i]);
                                // limit it to between 0 and 1
                                initialResponseThreshold =
                                        (initialResponseThreshold < 0.0) ? 0.0 :
                                                (initialResponseThreshold > 1.0) ? 1.0 :
                                                        initialResponseThreshold;
                            } catch (Exception e) {
                                logger.error("Could not parse double '" + args[i] + "'", e);
                            }
                        }
                        break;
                    case "FinalResponseThreshold":
                        if (i + 1 < args.length) {
                            i++;
                            try {
                                finalResponseThreshold = Double.parseDouble(args[i]);
                                // limit it to between 0 and 1
                                finalResponseThreshold =
                                        (finalResponseThreshold < 0.0) ? 0.0 :
                                                (finalResponseThreshold > 1.0) ? 1.0 :
                                                        finalResponseThreshold;
                            } catch (Exception e) {
                                logger.error("Could not parse double '" + args[i] + "'", e);
                            }

                        }
                        break;
                    case LOCATION_HOME:
                        if (i + 1 < args.length) {
                            i++;
                            try {
                                String[] vals = args[i].split(",");
                                Location location = new Location(LOCATION_HOME, Double.parseDouble(vals[0]), Double.parseDouble(vals[1]));
                                locations.put(LOCATION_HOME, location);
                            } catch (Exception e) {
                                System.err.println("Could not parse dependent's location '"
                                        + args[i] + "' : " + e.getMessage());
                            }
                        }
                        break;
                    case "EvacLocationPreference":
                    case "InvacLocationPreference":
                        if (i + 1 < args.length) {
                            i++;
                            try {
                                String[] vals = args[i].split(",");
                                Location location = new Location(vals[0], Double.parseDouble(vals[1]), Double.parseDouble(vals[2]));
                                String loc = args[i - 1].equals("EvacLocationPreference") ?
                                        LOCATION_EVAC_PREFERRED : LOCATION_INVAC_PREFERRED;
                                locations.put(loc, location);
                            } catch (Exception e) {
                                System.err.println("Could not parse location '"
                                        + args[i] + "' : " + e.getMessage());
                            }
                        }
                        break;
                    case "sharesInfoWithSocialNetwork":
                        if (i + 1 < args.length) {
                            i++;
                            try {
                                sharesInfoWithSocialNetwork = Boolean.parseBoolean(args[i]);
                            } catch (Exception e) {
                                System.err.println("Could not parse double '"
                                        + args[i] + "' : " + e.getMessage());
                            }
                        }
                        break;
                    case "WillGoHomeAfterVisitingDependents":
                        if (i + 1 < args.length) {
                            i++;
//                            try {
//                                //willGoHomeAfterVisitingDependents = Boolean.parseBoolean(args[i]);
//                            } catch (Exception e) {
//                                System.err.println("Could not parse boolean '"
//                                        + args[i] + "' : " + e.getMessage());
//                            }
                        }
                        break;
                    case "WillGoHomeBeforeLeaving":
                        if (i + 1 < args.length) {
                            i++;
                            try {
                                willGoHomeBeforeLeaving = Boolean.parseBoolean(args[i]);
                            } catch (Exception e) {
                                System.err.println("Could not parse boolean '"
                                        + args[i] + "' : " + e.getMessage());
                            }
                        }
                        break;
                    default:
                        // ignore other options
                        break;
                }
            }
        }
    }

    class Prefix {
        public String toString() {
            return String.format("Time %05.0f BushfireAgent %-4s : ", getTime(), getId());
        }
    }

    String logPrefix() {
        return prefix.toString();
    }

    void log(String msg) {
        writer.println(logPrefix() + msg);
    }

//    class DependentInfo {
//        private Location location;
//        private double lastVisitedAtTime = -1;
//
//        public Location getLocation() {
//            return location;
//        }
//
//        public void setLocation(Location location) {
//            this.location = location;
//        }
//
//        public double getLastVisitedAtTime() {
//            return lastVisitedAtTime;
//        }
//
//        public void setLastVisitedAtTime(double lastVisitedAtTime) {
//            this.lastVisitedAtTime = lastVisitedAtTime;
//        }
//
//        public boolean isVisited() {
//            return getLastVisitedAtTime()==-1;
//        }
//
//        @Override
//        public String toString() {
//            String s = (location==null) ? "null" : location.toString();
//            s += ", last visited at time="+ lastVisitedAtTime;
//            return s;
//        }
//    }
}
