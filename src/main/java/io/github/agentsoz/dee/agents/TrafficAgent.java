package io.github.agentsoz.dee.agents;

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
import io.github.agentsoz.dee.DeePerceptList;
import io.github.agentsoz.dee.blockage.Blockage;
import io.github.agentsoz.ees.Constants;
import io.github.agentsoz.ees.EmergencyMessage;
import io.github.agentsoz.ees.Run;
import io.github.agentsoz.ees.agents.bushfire.BushfireAgent;
import io.github.agentsoz.jill.core.beliefbase.BeliefBaseException;
import io.github.agentsoz.jill.core.beliefbase.BeliefSetField;
import io.github.agentsoz.jill.lang.AgentInfo;
import io.github.agentsoz.util.ActionList;
import io.github.agentsoz.util.Location;
import io.github.agentsoz.util.PerceptList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.PrintStream;
import java.util.*;
import java.util.List;

//import io.github.agentsoz.ees.ActionList;
//import io.github.agentsoz.ees.PerceptList;


/**
 * Methods that are used in ees.BushfireAgent but not used in dee.TrafficAgent:
 * checkBarometersAndTriggerResponseAsNeeded
 * isInitialResponseThresholdBreached
 * isFinalResponseThresholdBreached
 * triggerResponse
 * updateResponseBarometerSocialMessage
 * updateResponseBarometerFieldOfViewPercept
 * updateResponseBarometerMessages
 * Class DependantInfo
 */

@AgentInfo(hasGoals = {"io.github.agentsoz.abmjill.genact.EnvironmentAction", "io.github.agentsoz.dee.agents.GoalAssessBlockageImpact", "io.github.agentsoz.dee.agents.GoalEvaluate", "io.github.agentsoz.dee.agents.GoalDecide"})
public class TrafficAgent extends BushfireAgent {


    private final Logger logger = LoggerFactory.getLogger("io.github.agentsoz.dee");

    //    static final String LOCATION_HOME = "home";
//    static final String LOCATION_EVAC_PREFERRED = "evac";
//    static final String LOCATION_INVAC_PREFERRED = "invac";
    //internal variables
    private final String memory = "memory";
    private PrintStream writer = null;
    private QueryPerceptInterface queryInterface;
    private EnvironmentActionInterface envActionInterface;
    private double time = -1;
    private TrafficAgent.Prefix prefix = new TrafficAgent.Prefix();


    //new attributes
    private boolean assessSituation = false;
    //   private boolean sharedBlockageSNInfo = false;
    private int blockageRecencyThreshold;
    private double distanceFromTheBlockageThreshold;
    private int blockageAngleThreshold = 0; // degrees


    // reconsider times
    static final double RECONSIDER_LATER_TIME = 30.0;
    static final double RECONSIDER_SOONER_TIME = 5.0;
    static final double RECONSIDER_REGULAR_TIME = 10.0;


    enum MemoryEventType {
        BELIEVED,
        PERCEIVED,
        DECIDED,
        ACTIONED
    }

    enum MemoryEventValue {
        DONT_ASSESS,
        IS_PLAN_APPLICABLE,
        GOTO_LOCATION,
        LAST_ENV_ACTION_STATE,
        ASSESS,
        EVALUATE,
        RECONSIDER_AGAIN,
        DECIDE_BLOCKAGE_IMPACT,
        REROUTE
    }

    //defaults
//    private DependentInfo dependentInfo = null;
//    private double initialResponseThreshold = 0.5;
//    private double finalResponseThreshold = 0.5;
//    private double responseBarometerMessages = 0.0;
//    private double responseBarometerFieldOfView = 0.0;
//    private double responseBarometerSocialMessage = 0.0;
//    private boolean sharesInfoWithSocialNetwork = false;
//    private boolean willGoHomeAfterVisitingDependents = false;
//    private boolean willGoHomeBeforeLeaving = false;
//    private double smokeVisualValue = 0.3;
//    private double fireVisualValue = 1.0;
//    private double socialMessageEvacNowValue = 0.3;
    private Map<String, Location> locations;
    private Map<String,EnvironmentAction> activeEnvironmentActions;
    private ActionContent.State lastEnvironmentActionStatus;
    private ActionContent.State lastDriveActionStatus;
    private Set<String> blockagePointsShared; // contains a list of bloc
    private List<Blockage> blockageList;


    public TrafficAgent(String id) {
        super(id);
        locations = new HashMap<>();
        blockagePointsShared = new HashSet<>();
        blockageList = new ArrayList<Blockage>();
        activeEnvironmentActions = new HashMap<>();

    }


    public void setBlockageAngleThreshold(int blockageAngleThreshold) {
        this.blockageAngleThreshold = blockageAngleThreshold;
    }

    public int getBlockageAngleThreshold() {
        return blockageAngleThreshold;
    }

    public static double getReconsiderRegularTime() {
        return RECONSIDER_REGULAR_TIME;
    }

    public static double getReconsiderSoonerTime() {
        return RECONSIDER_SOONER_TIME;
    }

    public static double getReconsiderLaterTime() {
        return RECONSIDER_LATER_TIME;
    }

    public double getDistanceFromTheBlockageThreshold() {
        return distanceFromTheBlockageThreshold;
    }

    public void setBlockageRecencyThreshold(int blockageRecencyThreshold) {
        this.blockageRecencyThreshold = blockageRecencyThreshold;
    }

    public int getBlockageRecencyThreshold() {
        return blockageRecencyThreshold;
    }

    public boolean isAssessSituation() {
        return assessSituation;
    }

    public void setAssessSituation(boolean assessSituation) {
        this.assessSituation = assessSituation;
    }

    public Map<String, Location> getLocations() {
        return locations;
    }

    public void setLocations(Map<String, Location> locations) {
        this.locations = locations;
    }

    //    DependentInfo getDependentInfo() {
//        return dependentInfo;
//    }
    public List<Blockage> getBlockageList() {
        return blockageList;
    }

    boolean isDriving() {
        return activeEnvironmentActions != null && activeEnvironmentActions.containsKey(Constants.DRIVETO);
    }

    private void addActiveEnvironmentAction(EnvironmentAction activeEnvironmentAction) {
        activeEnvironmentActions.put(activeEnvironmentAction.getActionID(), activeEnvironmentAction);
    }

    private EnvironmentAction removeActiveEnvironmentAction(String actionId) {
        if (actionId != null && activeEnvironmentActions.containsKey(actionId)) {
            return activeEnvironmentActions.remove(actionId);
        }
        return null;
    }

    public ActionContent.State getLastEnvironmentActionState() {
        return lastEnvironmentActionStatus;
    }

    public void setLastEnvironmentActionState(ActionContent.State lastEnvironmentActionStatus) {
        this.lastEnvironmentActionStatus = lastEnvironmentActionStatus;
    }

//    double getResponseBarometer() {
//        return responseBarometerMessages + responseBarometerFieldOfView + responseBarometerSocialMessage;
//    }

//    boolean getWillGoHomeAfterVisitingDependents()
//    {
//        return willGoHomeAfterVisitingDependents;
//    }
//    boolean getWillGoHomeBeforeLeaving()
//    {
//        return willGoHomeBeforeLeaving;
//    }



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

//            memorise(MemoryEventType.BELIEVED.name(),
//                    MemoryEventValue.DEPENDENTS_INFO.name() + ":" + getDependentInfo() );

        } catch (BeliefBaseException e) {
            throw new RuntimeException(e);
        }

        // perceive congestion and blockage events always
        EnvironmentAction action = new EnvironmentAction(
                Integer.toString(getId()),
                Constants.PERCEIVE,
                new Object[]{Constants.BLOCKED, Constants.CONGESTION});
        post(action);
        addActiveEnvironmentAction(action);
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
     * <p>
     * Seems like Time is received only when there is a another percept packaged, like blocked; otherwise agent will not recieve it
     */
    @Override
    public void handlePercept(String perceptID, Object parameters) {




        if (perceptID == null || perceptID.isEmpty()) {
            return;
        } // first process time percept as other percepts are using current time.
        else if (perceptID.equals(PerceptList.TIME)) {
            if (parameters instanceof Double) {
                //time = (double) parameters;
                updateBlockageInfoBasedOnTime((double) parameters);
            }
            return;
        }

        // save it to memory
        memorise(MemoryEventType.PERCEIVED.name(), perceptID + ":" + parameters.toString());

//        else if (perceptID.equals(DeePerceptList.EMERGENCY_MESSAGE)) {
//            updateResponseBarometerMessages(parameters);
//        } else if (perceptID.equals(DeePerceptList.SOCIAL_NETWORK_MSG)) {
//            updateResponseBarometerSocialMessage(parameters);
//        }
//        else if (perceptID.equals(DeePerceptList.FIELD_OF_VIEW)) {
//          //  updateResponseBarometerFieldOfViewPercept(parameters);
//            if (DeePerceptList.SIGHTED_FIRE.equalsIgnoreCase(parameters.toString())) {
//                handleFireVisual();
//            }


        if (perceptID.equals(PerceptList.ARRIVED)) {
            // do something
        } else if (perceptID.equals(DeePerceptList.BLOCKAGE_INFLUENCE)) {
            processSNBlockageInfo(parameters);
        } else if (perceptID.equals(DeePerceptList.BLOCKAGE_UPDATES)) {
            processSNBlockageUpdates(parameters);

        } else if (perceptID.equals(PerceptList.CONGESTION)) {
            checkCongestionNearBlockage();
        } else if (perceptID.equals(PerceptList.BLOCKED)) { // 1. current link 2. blocked link

            processBlockedPercept((Object[]) parameters);
        }

        // handle percept spread on social network
//        handleSocialPercept(perceptID, parameters);

        // Now trigger a response as needed
//        checkBarometersAndTriggerResponseAsNeeded();

        if (assessSituation) {
            post(new GoalAssessBlockageImpact("assess blockage impact"));
        }
    }


    /*
        Agent can receive a blocked perecpt based on several possibilities:
        1. Agent does not know about the blockage
        2. Agent knows about the blockage from its SN, but decides to reconsider_again/dont reroute

     */
    private void processBlockedPercept(Object[] parameters) {

        String currentLinkID = (String) parameters[0];
        String blockedLinkID = (String) parameters[1];

//        Location currentLoc = ((Location[]) this.getQueryPerceptInterface().queryPercept(
//                String.valueOf(this.getId()), PerceptList.REQUEST_LOCATION, null))[0];

        String blockageName = Blockage.findBlockageNameFromBlockedLink(blockedLinkID);
        if (blockageName == null) {
            logger.error("no blockage name found for agent {} at time {}. Blocked Link id: {}", getId(), time, blockedLinkID);
            return;
        }

        if (!isBlockageExistsInBlockageList(blockageName)) { // agent does not know about the blockage

            // double[] cords = Blockage.findAndGetLocationOfBlockage(blockageName); // get cordinates of the blockage
            Blockage newBlockage = Blockage.createBlockageFromName(blockageName);
            if(newBlockage == null) {
                logger.error("Cannot create blockage instance, unknown blockage name {}", blockageName);
            }
            newBlockage.setLatestBlockageInfoTime(time); // update latest time to now.
            blockageList.add(newBlockage);

            //SN tasks
            String blockageInfo = "road blockage," + newBlockage.getName() + "," + time; // SN INFORMATION
            sendBlockageInfluencetoSN(blockageInfo);
            blockagePointsShared.add(blockageName); // save blockage name as this influence is sent only once.
        } else { // agent knows about the blockage, either from SN or ABM.

            Blockage blockage = getBlockageObjectFromName(blockageName); // get the existing blockage instance
            blockage.setLatestBlockageInfoTime(time); //  update latest time to now.

            //SN tasks
            String blockageInfoUpdate = blockageName + time; // SN INFORMATION UPDATE, send everytime an agenr receives a blocked percept
            sendBlockageUpdatestoSN(blockageInfoUpdate); // no need to save as information updates are sent everytime they are received from ABM.

        }

        //Finally, issue  a BDI action
        replanCurrentDriveTo(Constants.EvacRoutingMode.carGlobalInformation);
        // perceive congestion and blockage events always
        EnvironmentAction action = new EnvironmentAction(
                Integer.toString(getId()),
                Constants.PERCEIVE,
                new Object[] {Constants.BLOCKED, Constants.CONGESTION});
        post(action);
        addActiveEnvironmentAction(action);

    }

    private void processSNBlockageUpdates(Object params) { // #assume format: Blcoakge,time of type String
        if (params == null || !(params instanceof String)) {
            logger.error("unknown blockage update received for agent {}: null or incorrect length ", getId());
            return;
        }

        String[] tokens = ((String) params).split(",");

        String blockageName = (String) tokens[0];
        double latestTimeFromSN = Double.valueOf(tokens[0]);

        Blockage blockage = getBlockageObjectFromName(blockageName);
        blockage.setLatestBlockageInfoTime(latestTimeFromSN); // update most recent time received from
    }

    private void processSNBlockageInfo(Object msg) {


        if (msg == null || !(msg instanceof String)) {
            logger.error("null information received from the SN model for agent {}", getId());
            return;
        }

        //extract pieces of info from the SN msg (example format: road blockage,grossmands,time)
        String[] tokens = ((String) msg).split(",");

        String blockageName = tokens[1]; // second token
        if (isBlockageExistsInBlockageList(blockageName)) { // normally this shouldn't be the case, otherwise agent is active in SN
            logger.warn("agent {} is already aware of the {} SN blockage information at time {}", getId(), blockageName, time);
        } else {

            Blockage newBlockage = Blockage.createBlockageFromName(blockageName);
            if (newBlockage == null) {
                logger.error("could not find the blockage point in the SN information for name {}", blockageName);
            }
            blockageList.add(newBlockage);
            newBlockage.setLatestBlockageInfoTime(time);
            this.assessSituation = true;

        }


    }

    // does the agent already know about the blockage
    private boolean isBlockageExistsInBlockageList(String name) {
        boolean result = false;
        for (Blockage blockage : blockageList) {
            if (name.equals(blockage.getName())) {
                result = true;
                break;
            }
        }
        return result;
    }

    private Blockage getBlockageObjectFromName(String name) {
        Blockage targetBlockage = null;

        for (Blockage blockage : blockageList) {
            if (name.equals(blockage.getName())) {
                targetBlockage = blockage;
                break;
            }
        }
        return targetBlockage;
    }

    private void checkCongestionNearBlockage() {

        if (blockageList.isEmpty()) {
            return;
        }

        for (Blockage blockage : blockageList) {

            calcAndUpdateDistanceToBlockage(blockage); // first update distances
            if (blockage.getDistToBlockage() <= distanceFromTheBlockageThreshold) {
                blockage.setCongestionNearBlockage(true);
                this.assessSituation = true;
            }
        }
    }

    private void updateBlockageInfoBasedOnTime(double curTime) { // Process time percept

        time = curTime;

        if (blockageList.isEmpty()) {
            return;
        }

        for (Blockage blockage : blockageList) {
            if (blockage.getLastUpdatedTime() + blockage.getReconsiderTime() == time) {

                calcAndUpdateDistanceToBlockage(blockage);
                blockage.setLastUpdatedTime(time);

                this.assessSituation = true;


            }
        }
    }

    // Using beeline distance (straightline between two points) which is more natural and not computationally expensive
    private void calcAndUpdateDistanceToBlockage(Blockage blockage) {
        Location currentLoc = ((Location[]) this.getQueryPerceptInterface().queryPercept(
                String.valueOf(this.getId()), PerceptList.REQUEST_LOCATION, null))[0];

        double dist = Location.distanceBetween(currentLoc, blockage);
        blockage.setDistToBlockage(dist);
    }


    /*

        This method returns the angle between the two lines formed by the 4 locations.
        Here, we don't care about the direction (clockwise or anti-clockwise), so need to retreive smallest angle out of the two angles between the two lines.


       Calculation is based on: https://stackoverflow.com/questions/3365171/calculating-the-angle-between-two-lines-without-having-to-calculate-the-slope
 The atan2() method returns theta of polar coordinates(distance r and angle theta from 0,0, anti-clockwise) - a numeric value between -pi and pi.

    public static double angleBetween2Lines(Line2D line1, Line2D line2)
    {
        double angle1 = Math.atan2(line1.getY1() - line1.getY2(),
                line1.getX1() - line1.getX2());
        double angle2 = Math.atan2(line2.getY1() - line2.getY2(),
                line2.getX1() - line2.getX2());
        return angle1-angle2;
    }
     */
    public double getSmallestAngleBetweenTwoLines(Location line1P1, Location line1P2, Location line2P1, Location line2P2 ) {

        // calculate angle of blockage, based on the line connecting current location and destination
        double angle1 = Math.atan2(line1P1.getY() - line1P2.getY(), line1P1.getX() - line1P2.getX());

        //evaluate direction of line connecting current location and blockage
        double angle2 = Math.atan2(line2P1.getY() - line2P2.getY(), line2P1.getX() - line2P2.getX());
        double angleDiffInAntiClockwiseDirection = Math.abs(Math.toDegrees(angle1 - angle2)); // anti-clokwise angle
        double angleDiffInClockwiseDirection = 360 - angleDiffInAntiClockwiseDirection; // clockwise angle

        return Math.min(angleDiffInAntiClockwiseDirection,angleDiffInClockwiseDirection); // return the smallest from the two angles

    }



    // private double
//    private void handleSocialPercept(String perceptID, Object parameters) {
//
////        if (!sharesInfoWithSocialNetwork) {
////            return;
////        }
//        // Spread EVACUATE_NOW if haven't done so already
//        if (perceptID.equals(DeePerceptList.EMERGENCY_MESSAGE) &&
//                !blockagePointsShared.contains(EmergencyMessage.EmergencyMessageType.EVACUATE_NOW.name()) &&
//                parameters instanceof String &&
//                getEmergencyMessageType(parameters) == EmergencyMessage.EmergencyMessageType.EVACUATE_NOW) {
//            shareWithSocialNetwork((String) parameters);
//            blockagePointsShared.add(getEmergencyMessageType(parameters).name());
//        }
//        // Spread BLOCKED for given blocked link if haven't already
//        if (perceptID.equals(DeePerceptList.BLOCKED)) {
//            String blockedMsg = DeePerceptList.BLOCKED + parameters.toString();
//            if (!blockagePointsShared.contains(blockedMsg)) {
//                shareWithSocialNetwork(blockedMsg);
//                blockagePointsShared.add(blockedMsg);
//            }
//        }
//
//    }

    void memorise(String event, String data) {
        try {
            addBelief(memory, event, data);
            log("memory:" + event + ":" + data);
        } catch (BeliefBaseException e) {
            throw new RuntimeException(e);
        }
    }

//    boolean startDrivingTo(Location location, Constants.EvacRoutingMode routingMode) {
//        if (location == null) return false;
//        memorise(MemoryEventType.DECIDED.name(), MemoryEventValue.GOTO_LOCATION.name() + ":" + location.toString());
//        double distToTravel = getTravelDistanceTo(location);
//        if (distToTravel == 0.0) {
//            // already there, so no need to drive
//            return false;
//        }
//
//        // perceive congestion and blockage events always
//        EnvironmentAction action = new EnvironmentAction(
//                Integer.toString(getId()),
//                Constants.PERCEIVE,
//                new Object[] {Constants.BLOCKED, Constants.CONGESTION});
//        post(action);
//        addActiveEnvironmentAction(action);
//
//        Object[] params = new Object[4];
//        params[0] = Constants.DRIVETO;
//        params[1] = location.getCoordinates();
//        params[2] = getTime() + 5.0; // five secs from now;
//        params[3] = routingMode;
//        memorise(MemoryEventType.ACTIONED.name(), Constants.DRIVETO
//                + ":"+ location + ":" + String.format("%.0f", distToTravel) + "m away");
//        action = new EnvironmentAction(
//                Integer.toString(getId()),
//                Constants.DRIVETO, params);
//        addActiveEnvironmentAction(action); // will be reset by updateAction()
//        subgoal(action); // should be last call in any plan step
//        return true;
//    }


//    private void handleFireVisual() {
//        // Always replan when we see fire
//        replanCurrentDriveTo(MATSimEvacModel.EvacRoutingMode.carGlobalInformation);
//    }

    boolean replanCurrentDriveTo(Constants.EvacRoutingMode routingMode) {
        memorise(MemoryEventType.ACTIONED.name(), Constants.REPLAN_CURRENT_DRIVETO);
        EnvironmentAction action = new EnvironmentAction(
                Integer.toString(getId()),
                Constants.REPLAN_CURRENT_DRIVETO,
                new Object[] {routingMode});
        addActiveEnvironmentAction(action); // will be reset by updateAction()
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

    private void sendBlockageInfluencetoSN(String content) {
        String[] msg = {content, String.valueOf(getId())};
        memorise(MemoryEventType.ACTIONED.name(), DeePerceptList.BLOCKAGE_INFLUENCE // blockage information
                + ":" + content);
        DataServer.getInstance(Run.DATASERVER).publish(DeePerceptList.BLOCKAGE_INFLUENCE, msg);
    }

    private void sendBlockageUpdatestoSN(String content) {
        String[] msg = {content, String.valueOf(getId())};
        memorise(MemoryEventType.ACTIONED.name(), DeePerceptList.BDI_STATE_UPDATES // blockage information
                + ":" + content);
        DataServer.getInstance(Run.DATASERVER).publish(DeePerceptList.BLOCKAGE_UPDATES, msg);
    }

    private void broadcastToSocialNetwork(String content) {
        String[] msg = {content, String.valueOf(getId())};
        memorise(MemoryEventType.ACTIONED.name(), DeePerceptList.BLOCKAGE_INFO_BROADCAST
                + ":" + content);
        DataServer.getInstance(Run.DATASERVER).publish(DeePerceptList.BLOCKAGE_INFO_BROADCAST, msg);
    }

    /**
     * Called by the Jill model with the status of a BDI action previously
     * posted by this agent to the ABM environment.
     */
    @Override
    public void updateAction(String actionID, ActionContent content) {
        logger.debug("{} received action update: {}", logPrefix(), content);
        ActionContent.State actionState = content.getState();
        if (actionState == ActionContent.State.PASSED ||
                actionState == ActionContent.State.FAILED ||
                actionState == ActionContent.State.DROPPED) {
            memorise(MemoryEventType.BELIEVED.name(), MemoryEventValue.LAST_ENV_ACTION_STATE.name() + "=" + actionState.name());
            removeActiveEnvironmentAction(content.getAction_type()); // remove the action

            if (content.getAction_type().equals(Constants.DRIVETO)) {
                setLastDriveActionStatus(content.getState()); // save the finish state of the action
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
    public void start() { // TWO START METHODS
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
    public QueryPerceptInterface getQueryPerceptInterface() {
        return queryInterface;
    }

    @Override
    public void setQueryPerceptInterface(QueryPerceptInterface queryInterface) {
        this.queryInterface = queryInterface;
    }

    public EnvironmentActionInterface getEnvironmentActionInterface() {
        return envActionInterface;
    }

    public void setEnvironmentActionInterface(EnvironmentActionInterface envActInterface) {
        this.envActionInterface = envActInterface;
    }

    private void parseArgs(String[] args) {
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "distanceFromTheBlockageThreshold":
                        if (i + 1 < args.length) {
                            i++;
                            try {
                                distanceFromTheBlockageThreshold = Double.parseDouble(args[i]);
                                logger.trace("distanceFromTheBlockageThreshold: {}", distanceFromTheBlockageThreshold);
                            } catch (Exception e) {
                                logger.error("Could not parse double '" + args[i] + "'", e);
                            }

                        }
                        break;
                    case "blockageRecencyThreshold":
                        if (i + 1 < args.length) {
                            i++;
                            try {
                                blockageRecencyThreshold = Integer.parseInt(args[i]);
                                logger.trace("blockageRecencyThreshold: {}", blockageRecencyThreshold);
                            } catch (Exception e) {
                                logger.error("Could not parse int '" + args[i] + "'", e);
                            }

                        }
                        break;
                    case "blockageAngleThreshold":
                        if (i + 1 < args.length) {
                            i++;
                            try {
                                blockageAngleThreshold = Integer.parseInt(args[i]);
                                logger.trace("blockageAngleThreshold: {}", blockageAngleThreshold);
                            } catch (Exception e) {
                                logger.error("Could not parse int '" + args[i] + "'", e);
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

    private void setLastDriveActionStatus(ActionContent.State lastDriveActionStatus) {
        this.lastDriveActionStatus = lastDriveActionStatus;
    }

    public ActionContent.State getLastDriveActionStatus() {
        return lastDriveActionStatus;
    }

    public double getTime() {
        return time;
    }

    public String logPrefix() {
        return prefix.toString();
    }

    private void log(String msg) {
        writer.println(logPrefix() + msg);
    }


    class Prefix {
        public String toString() {
            return String.format("Time %05.0f TrafficAgent %-9s : ", getTime(), getId());
        }
    }

}
