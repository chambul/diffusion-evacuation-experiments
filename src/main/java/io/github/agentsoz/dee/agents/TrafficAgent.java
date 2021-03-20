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
import io.github.agentsoz.dee.DeePerceptList;
import io.github.agentsoz.dee.blockage.Blockage;
import io.github.agentsoz.ees.*;
import io.github.agentsoz.ees.agents.bushfire.BushfireAgent;
import io.github.agentsoz.jill.core.beliefbase.BeliefBaseException;
import io.github.agentsoz.jill.core.beliefbase.BeliefSetField;
import io.github.agentsoz.jill.lang.AgentInfo;
import io.github.agentsoz.util.Location;
import io.github.agentsoz.util.PerceptList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.PrintStream;
import java.nio.file.WatchEvent;
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

@AgentInfo(hasGoals = {"io.github.agentsoz.abmjill.genact.EnvironmentAction",
        "io.github.agentsoz.dee.agents.GoalAssessBlockageImpact",
        "io.github.agentsoz.dee.agents.GoalEvaluate",
        "io.github.agentsoz.dee.agents.GoalDecide",
        "io.github.agentsoz.dee.agents.GoalReplanToDestination",
        "io.github.agentsoz.dee.agents.GoalTest"
})
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
    private boolean travelPlanCompleted = false; // checks if the agent is doing/completed the final activity.
    private boolean reroutedOnce = false; // checks if the agent has rerouted once.
    //   private boolean sharedBlockageSNInfo = false;
    private int blockageRecencyThreshold=0; // in minutes
    private double distanceFromTheBlockageThreshold; // km
    private int blockageAngleThreshold = 0; // degrees

    // reconsider times
    static final double RECONSIDER_LATER_TIME = 30.0*60;
    static final double RECONSIDER_SOONER_TIME = 5.0*60;
    static final double RECONSIDER_REGULAR_TIME = 15.0*60;


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
        REROUTE,
        MATSIM_PLAN_COMPLETED,
        STATE_CHANGED;
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
 //   private Set<String> blockagePointsShared; // contains a list of bloc
    private List<Blockage> blockageList;


    public TrafficAgent(String id) {
        super(id);
        locations = new HashMap<>();
//        blockagePointsShared = new HashSet<>();
        blockageList = new ArrayList<Blockage>();
        activeEnvironmentActions = new HashMap<>();

    }


    public void setBlockageAngleThreshold(int blockageAngleThreshold) {
        this.blockageAngleThreshold = blockageAngleThreshold;
    }

    public int getBlockageAngleThreshold() {
        return blockageAngleThreshold;
    }

    public boolean isReroutedOnce() {
        return reroutedOnce;
    }

    public void setReroutedOnce(boolean reroutedOnce) {
        this.reroutedOnce = reroutedOnce;
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

    public int getBlockageRecencyThresholdInSeconds() {
        return blockageRecencyThreshold*60;
    }

    public boolean isAssessSituation() {
        return assessSituation;
    }

    public boolean isTravelPlanCompleted() {
        return travelPlanCompleted;
    }

    public void setTravelPlanCompleted(boolean travelPlanCompleted) {
        this.travelPlanCompleted = travelPlanCompleted;
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

//    boolean isDriving() {
//        return activeEnvironmentActions != null && activeEnvironmentActions.containsKey(Constants.DRIVETO);
//    }

    private void addActiveEnvironmentAction(EnvironmentAction activeEnvironmentAction) {
        activeEnvironmentActions.put(activeEnvironmentAction.getActionID(), activeEnvironmentAction);
    }

//    private EnvironmentAction removeActiveEnvironmentAction(String actionId) {
//        if (actionId != null && activeEnvironmentActions.containsKey(actionId)) {
//            return activeEnvironmentActions.remove(actionId);
//        }
//        return null;
//    }

//    public ActionContent.State getLastEnvironmentActionState() {
//        return lastEnvironmentActionStatus;
//    }

//    public void setLastEnvironmentActionState(ActionContent.State lastEnvironmentActionStatus) {
//        this.lastEnvironmentActionStatus = lastEnvironmentActionStatus;
//    }

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
        registerPercepts(new String[] {Constants.BLOCKED, Constants.CONGESTION});
        super.start(writer, params); // need to set BushfireAgent class writer as this is used when replanCurrentDriveTo method is called.
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

        resetDiffusionContent(); //set Diffusion content to null.

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
        if(!perceptID.equals(Constants.DIFFUSION_CONTENT)) {
            memorise(MemoryEventType.PERCEIVED.name(), perceptID + ":" + parameters.toString());
        }

        if (perceptID.equals(PerceptList.ARRIVED)) {
            // do something
        } else if (perceptID.equals(Constants.DIFFUSION_CONTENT)) {
            DiffusionContent diffusedContent = (DiffusionContent) parameters;
            HashMap<String,Object[]> contents = diffusedContent.getContentsMapFromDiffusionModel();

            for(String contentType: contents.keySet()){

                if(contentType.equals(DeePerceptList.BLOCKAGE_INFLUENCE)){
                    String content= (String)contents.get(contentType)[0];
                    processSNBlockageInfo(content);

                    memorise(MemoryEventType.PERCEIVED.name(), perceptID + ":" + content);

                }
//                else if(content.equals(DeePerceptList.BLOCKAGE_UPDATES)){
//
//                    processSNBlockageUpdates(contents.get(content));
//                }
                else{
                    logger.error("unknown social network content type received for BDI agent {}: {} ", getId(),contentType);
                }
            }




        } else if (perceptID.equals(PerceptList.CONGESTION)) {
            checkCongestionNearBlockage();
        } else if (perceptID.equals(PerceptList.BLOCKED)) { // 1. current link 2. blocked link

            processBlockedPercept((Map<String,String>)parameters );
            registerPercepts(new String[] {Constants.BLOCKED, Constants.CONGESTION});
        }

        // handle percept spread on social network
//        handleSocialPercept(perceptID, parameters);

        // Now trigger a response as needed
//        checkBarometersAndTriggerResponseAsNeeded();

        if (assessSituation && !travelPlanCompleted && !reroutedOnce) {
            post(new GoalAssessBlockageImpact("assess blockage impact")); //
         //   post(new GoalReplanToDestination("replan journey"));
//            post(new GoalTest("test goal"));
//            replanCurrentDriveTo(Constants.EvacRoutingMode.carGlobalInformation);
        }



    }


    /*
        Agent can receive a blocked perecpt based on several possibilities:
        1. Agent does not know about the blockage
        2. Agent knows about the blockage from its SN, but decides to reconsider_again/dont reroute

     */
    private void processBlockedPercept(Map<String,String> parameters) {

//        String currentLinkID = (String) parameters.get("link");
        String blockedLinkID = (String) parameters.get("nextlink");

//        Location currentLoc = ((Location[]) this.getQueryPerceptInterface().queryPercept(
//                String.valueOf(this.getId()), PerceptList.REQUEST_LOCATION, null))[0];

        String blockageName = Blockage.findBlockageNameFromBlockedLink(blockedLinkID);
        if (blockageName == null) {
            logger.error("no blockage name found for agent {} at time {}. Blocked Link id: {}", getId(), time, blockedLinkID);
            return;
        }

        if (!isBlockageExistsInBlockageList(blockageName)) { // agent does not know about the blockage
            Blockage newBlockage = Blockage.createBlockageFromName(blockageName);
            if(newBlockage == null) {
                logger.error("Cannot create blockage instance, unknown blockage name {}", blockageName);
            }
            blockageList.add(newBlockage);

            // blockage influence,  should only send one/first time, road blockage at X
            String blockageInfo = "road blockage at " + newBlockage.getName() ; //+ "," + time
            putBlockageInfluencetoDiffusionContent(DeePerceptList.BLOCKAGE_INFLUENCE,blockageInfo);
        }

        //  for every blocked percept, update blockage time, create content and
        Blockage blockage = getBlockageObjectFromName(blockageName);
        blockage.setLatestBlockageObservedTime(time); // time known of the actual event

        //SN tasks

           // blockagePointsShared.add(blockageName); // save blockage name as this influence is sent only once.
//        } else { // agent knows about the blockage, either from SN or ABM.
//
//            Blockage blockage = getBlockageObjectFromName(blockageName); // get the existing blockage instance
//            blockage.setLatestBlockageObservedTime(time); //  update latest time to now.
//
//            //SN tasks
//            putBlockageTimeToDiffusionContent(DeePerceptList.BLOCKAGE_UPDATES,blockageName,time); // no need to save as information updates are sent everytime they are received from ABM.
//
//        }

        //Finally, issue  a BDI action
        replanCurrentDriveTo(Constants.EvacRoutingMode.carGlobalInformation);
        this.setReroutedOnce(true);

        // perceive congestion and blockage events always
        registerPercepts(new String[] {Constants.BLOCKED, Constants.CONGESTION});

        //finally, publish diffusion content updates
        sendDiffusionContentToBDIModel();

    }

//    public boolean reRouteNow(){
//        return replanCurrentDriveTo(Constants.EvacRoutingMode.carGlobalInformation);
//    }


//    private void processSNBlockageUpdates(Object[] params) { // expected parameters: Blcoakge name ,time
//        if (params == null ) { // || !(params instanceof String
//            logger.error("unknown blockage update received for agent {}: null or incorrect length ", getId());
//            return;
//        }
//
//       // String[] tokens = params.split(",");
//
//        String blockageName = (String) params[0];
//        double latestTimeFromSN =  (double) params[1];
//
//        Blockage blockage = getBlockageObjectFromName(blockageName);
//        blockage.setLatestBlockageObservedTime(latestTimeFromSN); // update most recent time received from
//    }

    private void processSNBlockageInfo(String content) {


        if (content == null || !(content instanceof String)) {
            logger.error("null information received from the SN model for agent {}", getId());
            return;
        }


        try{
            //#FIXME token extraction is disabled for the influence message.
//            String[] tokens = ((String) content).split(","); //extract pieces of info from the SN msg (example format: road blockage,grossmands,time)

//            String blockageName = tokens[1]; // grossmands
//            double newObservedTime = Double.valueOf(tokens[2]); // time
                String blockageName= "grid_network_blockage";
                double newObservedTime  = time;
            if (!isBlockageExistsInBlockageList(blockageName)) { // first time hearing about the blockage, create new blockage and add to list
                Blockage newBlockage = Blockage.createBlockageFromName(blockageName);
                if (newBlockage == null) {
                    logger.error("could not find the blockage {} in the blockage list", blockageName);
                }
                blockageList.add(newBlockage);
            }

            Blockage blockage = getBlockageObjectFromName(blockageName);
           if(blockage.getLatestObservedTime() < newObservedTime){ // update if later than the current observed time
               blockage.setLatestBlockageObservedTime(newObservedTime); // time blockage event was observed
           }
            blockage.setLatestInfoReceivedTime(time); // update received time to the latest, which is now
            this.assessSituation = true;

        }
        catch(ArrayIndexOutOfBoundsException e) {
            logger.error("Cannot find blockage name by splitting received content", e.getMessage());

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

    //type: DeePerceptList.BLOCKAGE_INFLUENCE
    private void putBlockageInfluencetoDiffusionContent(String contentType, String content) {
        DiffusionContent dc =  getOrCreateDiffusionContent();
        String[] params = {content};
        dc.getContentsMapFromBDIModel().put(contentType,params);

        memorise(MemoryEventType.ACTIONED.name(), contentType
                + ":" + content);
        setPublishDiffusionContentToTrue(); // publish to data server
    }

    //type: DeePerceptList.BLOCKAGE_UPDATES
//    private void putBlockageTimeToDiffusionContent(String contentType, String blockage, double time) {
//        Object[] params = {blockage, time};
//        getOrCreateDiffusionContent().getContentsMapFromBDIModel().put(contentType,params);
//
//        memorise(MemoryEventType.ACTIONED.name(), contentType // blockage information
//                + ":" + params.toString());
//        setPublishDiffusionContentToTrue(); //publish to data server
//    }

    // type: DeePerceptList.BLOCKAGE_INFO_BROADCAST
    private void putBroadcastContenttoDiffusionContent(String type, String content) {

        String[] msg = {content};
        getOrCreateDiffusionContent().getBroadcastContentsMapFromBDIModel().put(type,msg);
        memorise(MemoryEventType.ACTIONED.name(),
                type + ":" + content);
        setPublishDiffusionContentToTrue(); //publish to data server
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
