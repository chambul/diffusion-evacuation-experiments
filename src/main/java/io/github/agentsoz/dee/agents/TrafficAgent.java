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


    private final Logger logger = LoggerFactory.getLogger(JillBDIModel.class);

    //    static final String LOCATION_HOME = "home";
//    static final String LOCATION_EVAC_PREFERRED = "evac";
//    static final String LOCATION_INVAC_PREFERRED = "invac";
    //internal variables
    private final String memory = "memory";
    private static String BLOCKAGE_NAME = " ";
    private  static long seedLimit = 0;
    private  static int seedSize = 0;
    private PrintStream writer = null;
    private QueryPerceptInterface queryInterface;
    private EnvironmentActionInterface envActionInterface;
    private double time = -1;
    private TrafficAgent.Prefix prefix = new TrafficAgent.Prefix();


    //new attributes
    private boolean assessSituation = false;
    private boolean travelPlanCompleted = false; // checks if the agent is doing/completed the final activity.
    private boolean reroutedOnce = false; // checks if the agent has rerouted once.
    private boolean inActivity = true; // initially set to true as act end/start percepts are registered at 61secs.
    protected boolean  assessWhenDeparting = false;
    //   private boolean sharedBlockageSNInfo = false;
    private int blockageRecencyThreshold=0; // in minutes
    private double distanceFromTheBlockageThreshold; //  specified in km in configs, converted to meters when assigning
    private int blockageAngleThreshold = 0; // degrees

    // reconsider times
    static final double RECONSIDER_LATER_TIME = 15.0*60;
    static final double RECONSIDER_SOONER_TIME = 3.0*60;
    static final double RECONSIDER_REGULAR_TIME = 5.0*60;

    //counters
    protected  static int proactive_reroute_count =0;
    protected static int reactive_reroute_count = 0;
    protected static boolean onetime = true; // to print the dee model stats only once

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
        // an agent gets two blocked percepts between two consequative seconds
        if(onetime) {
            logger.info("model stats: proactive reroute count: {} | reactive reroute count: {}", proactive_reroute_count, reactive_reroute_count);
            logger.info("number of agents spread info when hit (seed) {}",seedSize);
            onetime = false;
        }

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
        else if (perceptID.equals(PerceptList.TIME)) { // time percept is received every second.
            time = (double) parameters;
            if(time==61.0){ // first time percept, register for activity start/end. so all cativity end times must  atleast end at 00:02:00
                registerPercepts(new String[] {Constants.ACTIVITY_ENDED,Constants.ACTIVITY_STARTED,PerceptList.CONGESTION});
            }
            if(blockageList.isEmpty()){
                return;
            }
            else if (parameters instanceof Double) {
                for(Blockage blockage: blockageList) {
                    //reconsider time needs to be set from other events accordingly. if not set, then no need to do anything with time percept.
                    if (blockage.getLastUpdatedTime() + blockage.getReconsiderTime() == time) {
                        updateBlockageInfoBasedOnTime(time, blockage);
                    }
                }
            }
        }

        // save it to memory
        if(!perceptID.equals(Constants.DIFFUSION_CONTENT) && !perceptID.equals(PerceptList.TIME)
                && !perceptID.equals(PerceptList.ACTIVITY_ENDED) && !perceptID.equals(PerceptList.ACTIVITY_STARTED)) {


            memorise(MemoryEventType.PERCEIVED.name(),
                    perceptID + ":" + parameters.toString()  );
        }

        if (perceptID.equals(PerceptList.ARRIVED)) {
            // do something
        } else if (perceptID.equals(Constants.DIFFUSION_CONTENT)) {
            DiffusionContent diffusedContent = (DiffusionContent) parameters;
            HashMap<String,Object[]> contents = diffusedContent.getContentsMapFromDiffusionModel();

            for(String contentType: contents.keySet()){

                if(contentType.equals(DeePerceptList.BLOCKAGE_INFLUENCE)){
                    String content= (String)contents.get(contentType)[0];
                    double dist_to_blockage = processSNBlockageInfo(content);

                    memorise(MemoryEventType.PERCEIVED.name(), perceptID + ":" + content);
                    logger.info("agent " + this.getId() +" received blockage info, blockage driving distance is " + dist_to_blockage);

                }
                else if(contentType.equals(DeePerceptList.BLOCKAGE_UPDATES)){
                    Object[] params = contents.get(contentType);
                    processSNBlockageUpdates(params);
                }
                else{
                    logger.error("unknown social network content type received for BDI agent {}: {} ", getId(),contentType);
                }
            }

        } else if (perceptID.equals(PerceptList.CONGESTION)) {
            if(blockageList.isEmpty()){
                return;
            }
            else if(!reroutedOnce){
                checkCongestionNearBlockage();
            }

            registerPercepts(new String [] {PerceptList.CONGESTION});

        } else if (perceptID.equals(PerceptList.BLOCKED)) { // 1. current link 2. blocked link
            if(!reroutedOnce) { // same agent receives 2 blocked percepts
                processBlockedPercept((Map<String,String>)parameters );
            }
            registerPercepts(new String[] {Constants.BLOCKED});
        }
        else if(perceptID.equals(Constants.ACTIVITY_ENDED) || perceptID.equals(Constants.ACTIVITY_STARTED)){
            if(perceptID.equals(Constants.ACTIVITY_ENDED)) {
                inActivity=false;
                if(assessWhenDeparting){
//                    assessSituation = true;
                    replanCurrentDriveTo(Constants.EvacRoutingMode.carGlobalInformation);
                    setReroutedOnce(true);
                    proactive_reroute_count++;
                    memorise(MemoryEventType.ACTIONED.name(),
                            MemoryEventValue.REROUTE.name()
                                    + ": departing now; actioned the postponed reoute ");

                    //all done
                    assessWhenDeparting = false;

                }

            }
            else{ // ACTIVITY_STARTED
                inActivity = true;

            }

            if(!travelPlanCompleted || !reroutedOnce) {
                registerPercepts(new String[] {Constants.ACTIVITY_STARTED, Constants.ACTIVITY_ENDED});
            }
        }

        // handle percept spread on social network
//        handleSocialPercept(perceptID, parameters);

        // Now trigger a response as needed
//        checkBarometersAndTriggerResponseAsNeeded();

        if (assessSituation && !travelPlanCompleted && !reroutedOnce) {
//            if(inActivity){ // post goal when departing
//                assessWhenDeparting = true;
//            }
//            else{
                post(new GoalAssessBlockageImpact("Assess")); //
//                assessWhenDeparting = false;
//            post(new GoalReplanToDestination("replan journey"));
//            post(new GoalTest("test goal"));
//            replanCurrentDriveTo(Constants.EvacRoutingMode.carGlobalInformation);
//            }

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
            else if(BLOCKAGE_NAME == " "){
                    logger.info(" Initialising blockage: {} {} {} ", blockageName, newBlockage.getX(), newBlockage.getY());
                    BLOCKAGE_NAME = blockageName; // assuming only one blockage, so that all agents can get this name
                    int popSize = newBlockage.getPopulationSizeBasedOnBlockageName(blockageName);//#fixme remove hardcoded agent pop sizes
                    if(popSize == 0){
                        logger.warn("seed size is 0, no dynamic seeding will occur!");
                    }
                    seedLimit = Math.round(popSize*0.05);
                    logger.info("Blockage population seed limit: " + seedLimit);
            }
            blockageList.add(newBlockage);

            // blockage influence,  should only send one/first time, road blockage at X
            if(seedSize < seedLimit){
                String blockageInfo =  newBlockage.getName() ; //"road blockage at " +
                String[] params1 = {blockageInfo};
                putBlockageInfluencetoDiffusionContent(DeePerceptList.BLOCKAGE_INFLUENCE,params1);

                //update blockage observed time. this also may happen one time
                String blockageUpdate = String.valueOf(time);
                String[] params2 = {blockageName,blockageUpdate};
                putBlockageInfluencetoDiffusionContent(DeePerceptList.BLOCKAGE_UPDATES,params2);
                seedSize++;
            }

        }

        // iniitialise blockage object
        Blockage blockage = getBlockageObjectFromName(blockageName);
        blockage.setLatestBlockageObservedTime(time); // time known of the actual event
        blockage.setLastUpdatedTime(time);
        blockage.setReconsiderTime(RECONSIDER_REGULAR_TIME);

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
        reactive_reroute_count++;

        // perceive congestion and blockage events always
        registerPercepts(new String[] {Constants.BLOCKED, Constants.CONGESTION});

        //finally, publish diffusion content updates
        if(seedSize <= 250){
            sendDiffusionContentToBDIModel();
        }

    }

//    public boolean reRouteNow(){
//        return replanCurrentDriveTo(Constants.EvacRoutingMode.carGlobalInformation);
//    }


    private void processSNBlockageUpdates(Object[] params) { // expected parameters: Blcoakge name ,time
        if (params == null ) { // || !(params instanceof String
            logger.error("unknown blockage update received for agent {}: null or incorrect length ", getId());
            return;
        }

       // String[] tokens = params.split(",");

        String blockageName = (String) params[0];
        double latestTimeFromSN =  (double) params[1];

        Blockage blockage = getBlockageObjectFromName(blockageName);
        blockage.setLatestBlockageObservedTime(latestTimeFromSN); // update most recent time received from
    }

    private double processSNBlockageInfo(String content) {
        double res = -1;

        if (content == null || !(content instanceof String)) {
            logger.error("null information received from the SN model for agent {}", getId());
            return res;
        }


        try{
            //#FIXME token extraction is disabled for the influence message.
//            String[] tokens = ((String) content).split(","); //extract pieces of info from the SN msg (example format: road blockage,grossmands,time)

//            String blockageName = tokens[1]; // grossmands
//            double newObservedTime = Double.valueOf(tokens[2]); // time
//                String blockageName= "grid_network_blockage";
                double newObservedTime  = time;
            if (!isBlockageExistsInBlockageList(BLOCKAGE_NAME)) { // first time hearing about the blockage, create new blockage and add to list
                Blockage newBlockage = Blockage.createBlockageFromName(BLOCKAGE_NAME);
                if (newBlockage == null) {
                    logger.error("could not find the blockage {} in the blockage list", BLOCKAGE_NAME);
                }
                blockageList.add(newBlockage);
            }

            Blockage blockage = getBlockageObjectFromName(BLOCKAGE_NAME);
            // get driving distance to blockage location(from node of blockage link)
            double[] cords = {blockage.getX(),blockage.getY()};
            double dist =  (double)this.getQueryPerceptInterface().queryPercept(
                    String.valueOf(this.getId()), PerceptList.REQUEST_DRIVING_DISTANCE_TO, cords);

//            memorise(MemoryEventType.BELIEVED.name(),  " distance to blockage: " + dist);
            blockage.setLatestInfoReceivedTime(time); // update received time to the latest, which is now
            blockage.setLastUpdatedTime(time);
            this.assessSituation = true;
            res = dist;

        }
        catch(ArrayIndexOutOfBoundsException e) {
            logger.error("Cannot find blockage name by splitting received content", e.getMessage());

        }
        catch(NullPointerException e){
            logger.error("null value", e.getMessage());
        }

        return res;
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

//        if (blockageList.isEmpty()) {
//            return;
//        }

        for (Blockage blockage : blockageList) {

            calculateAndGetCurrentDistanceToBlockage(blockage); // first update distances
            if (blockage.getDistToBlockage() <= distanceFromTheBlockageThreshold) {
                blockage.setCongestionNearBlockage(true);
                this.assessSituation = true;
            }
        }
    }

    private void updateBlockageInfoBasedOnTime(double t, Blockage blockage) { // Process time percept

                calculateAndGetCurrentDistanceToBlockage(blockage);
                blockage.setLastUpdatedTime(t);
                blockage.setReconsiderTime(-1.0); // reset reconsider time so that this method wont be called periodicallly.

                this.assessSituation = true;

    }

    // Using beeline distance (straightline between two points) which is more natural and not computationally expensive
    // distance in meters
    protected double calculateAndGetCurrentDistanceToBlockage(Blockage blockage) {
        Location currentLoc = ((Location[]) this.getQueryPerceptInterface().queryPercept(
                String.valueOf(this.getId()), PerceptList.REQUEST_LOCATION, null))[1]; // get to node of the current link

        double dist = Location.distanceBetween(currentLoc, blockage);
        blockage.setDistToBlockage(dist);
        return dist;
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

    // arctan is the angle in euclidean space, given in radians (conversion o cartisian coordinates to polar coordinates).
    //atan2 returns the theta componant of the polar cordinate(r, theta)
    // use cur locatio coords as 0,0 calculate the angle between curloc-dest and curloc-blockage lines.
    //this method always returns the smallest angle between the two lines (does not consider the direction clockwise or anti-clockwise), tested in testBlockageInDirectionEstimation() in TestDeeUtils
    public double getSmallestAngleBetweenTwoLines(Location curLoc, Location blockageLoc, Location destLoc ) {

        // calculate angle based on the line connecting current location and destination
        double angle1 = Math.atan2(destLoc.getY() - curLoc.getY(), destLoc.getX() - curLoc.getX());

        //calculate angle based on the line connecting current location and blockage
        double angle2 = Math.atan2(blockageLoc.getY() - curLoc.getY(), blockageLoc.getX() - curLoc.getX());

        // return the abs value as we dont care about the direction (clockwise/anti-clockwise) of the angle.
        return Math.abs(Math.toDegrees(angle1 - angle2));

    }
//    public double getSmallestAngleBetweenTwoLines(Location line1P1, Location line1P2, Location line2P1, Location line2P2 ) {
//
//        // calculate angle of blockage, based on the line connecting current location and destination
//        double angle1 = Math.atan2(line1P1.getY() - line1P2.getY(), line1P1.getX() - line1P2.getX());
//
//        //evaluate direction of line connecting current location and blockage
//        double angle2 = Math.atan2(line2P1.getY() - line2P2.getY(), line2P1.getX() - line2P2.getX());
//        double angleDiffInAntiClockwiseDirection = Math.abs(Math.toDegrees(angle1 - angle2)); // anti-clokwise angle
//        double angleDiffInClockwiseDirection = 360 - angleDiffInAntiClockwiseDirection; // clockwise angle
//
//        return Math.min(angleDiffInAntiClockwiseDirection,angleDiffInClockwiseDirection); // return the smallest from the two angles
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
    private void putBlockageInfluencetoDiffusionContent(String contentType, String[] params) {
        DiffusionContent dc =  getOrCreateDiffusionContent();
        dc.getContentsMapFromBDIModel().put(contentType,params);

        memorise(MemoryEventType.ACTIONED.name(), contentType
                + ":" + params.toString());
        setPublishDiffusionContentToTrue(); // publish to data server
    }



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
                                distanceFromTheBlockageThreshold = Double.parseDouble(args[i]) * 1000; // convert to meters
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
    public boolean isInActivity(){
        return this.inActivity;
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
