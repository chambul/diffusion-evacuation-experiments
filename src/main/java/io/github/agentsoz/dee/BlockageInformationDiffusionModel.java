package io.github.agentsoz.dee;

/*-
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2018 by its authors. See AUTHORS file.
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


import io.github.agentsoz.dataInterface.DataClient;
import io.github.agentsoz.dataInterface.DataServer;
import io.github.agentsoz.dataInterface.DataSource;
import io.github.agentsoz.dee.agents.SocialTrafficAgent;
import io.github.agentsoz.ees.Constants;
import io.github.agentsoz.ees.DiffusionContent;
import io.github.agentsoz.ees.DiffusionDataContainer;
import io.github.agentsoz.ees.DiffusionModel;
import io.github.agentsoz.socialnetwork.ICModel;
import io.github.agentsoz.socialnetwork.SNConfig;
import io.github.agentsoz.socialnetwork.SocialAgent;
import io.github.agentsoz.socialnetwork.SocialNetworkDiffusionModel;
import io.github.agentsoz.util.Time;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/*

     extends DiffusionModel class in:
     init (with SocialTrafficAgents)
     sendData (include latest blockage updates)
     receiveData (process latest blockage times)

     Other functionalities are same as in DiffusionModel class:
     constructors
     parse (same configs)
     start
     stepDiffusionProcess (standard functionality of the IC model)
     finish


 */
public class BlockageInformationDiffusionModel extends DiffusionModel implements DataSource<SortedMap<Double, DiffusionDataContainer>>, DataClient<Object> {

//    Logger logger =  this.getSnManager().getSocialNetworkDiffusionLogger(); //LoggerFactory.getLogger(BlockageInformationDiffusionModel.class);

    private double lastUpdateTimeInMinutes = -1;
    private Time.TimestepUnit timestepUnit = Time.TimestepUnit.SECONDS;
    private String configFile = null;
    private List<String> agentsIds = null;

    HashMap<String,HashMap<String,Double>> blockageTimesMap;
//    HashMap<String,Double> latestBlockageTimes;

    public BlockageInformationDiffusionModel(String configFile) {
        super(configFile);
//        latestBlockageTimes = new HashMap<>();
        blockageTimesMap =  new HashMap<>();
    }

    public BlockageInformationDiffusionModel(Map<String, String> opts, DataServer dataServer, List<String> agentsIds) {
        super(opts, dataServer, agentsIds);
//        latestBlockageTimes = new HashMap<>();
        blockageTimesMap =  new HashMap<>();
    }


    @Override
    public void init(List<String> idList) {

        this.getSnManager().setupSNConfigsAndLogs(); // first, setup configs and create log
        for (String id : idList) {
            // this.snManager.createSocialAgent(id);
            int agentID = Integer.parseInt(id);
            this.getSnManager().getAgentMap().put(agentID, new SocialTrafficAgent(agentID));
        }
        this.getSnManager().genNetworkAndDiffModels(); // setup configs, gen network and diffusion models
        this.getSnManager().printSNModelconfigs();

        //subscribe to BDI data updates
        this.getDataServer().subscribe(this, Constants.DIFFUSION_DATA_CONTAINDER_FROM_BDI);

    }


    /*
            1. Update social states (Same as Diffusion model)
            2. Share blockage times with dierct neighbours -> update Hash
            3. run IC diffusion process -> update Hash
            4.
     */
    @Override
    public SortedMap<Double, DiffusionDataContainer> sendData(double timestep, String dataType) {
        double currentTime = Time.convertTime(timestep, timestepUnit, Time.TimestepUnit.MINUTES);
        timestep = timestep -1; // when SNM is called DataServer time is incremented by 1 (than expected).
        // create data structure to store current step contents and params
        DiffusionDataContainer currentStepDataContainer =  new DiffusionDataContainer();

//        if (nextTime != null) {
          // update the model with any new messages form agents
            ICModel icModel = (ICModel) this.getSnManager().getDiffModels()[0];

            if (!getLocalContentFromAgents().isEmpty()) { // update local content
                Map<String, String[]> map = new HashMap<>();
                for (String key : getLocalContentFromAgents().keySet()) {
                    Object[] set = getLocalContentFromAgents().get(key).toArray(new String[0]);
                    String[] newSet = new String[set.length];
                    for (int i = 0; i < set.length; i++) {
                        newSet[i] = (String)set[i];
                    }
                    map.put(key,newSet);
                    this.getSnManager().getSocialNetworkDiffusionLogger().trace(String.format("At time %.0f, total %d agents will spread content: %s", timestep, newSet.length, key));
                    this.getSnManager().getSocialNetworkDiffusionLogger().trace("Agents spreading the content are: {}", Arrays.toString(newSet));
                }
                icModel.updateSocialStatesFromLocalContent(map);
            }

            if(!getGlobalContentFromAgents().isEmpty()) { // update global contents

                this.getSnManager().getSocialNetworkDiffusionLogger().trace("Global content received to spread: {}", getGlobalContentFromAgents().toString());
                icModel.updateSocialStatesFromGlobalContent(getGlobalContentFromAgents());

            }


            // Step1 - diffusion processes: step the models and update data container
            stepDiffusionProcess(currentStepDataContainer,DeePerceptList.BLOCKAGE_INFLUENCE,timestep); // pass in seconds

        //Step2 - share blockage percept times with neighbours that are influenced (about the blockage event)
                updateAndShareLatestBlockageTimeWithNeighbours(currentStepDataContainer,timestep);



        //All done, register next time to call
        Double nextTime =  this.getSnManager().getEarliestTimeForNextStep()  ; //
        getDataServer().registerTimedUpdate(Constants.DIFFUSION_DATA_CONTAINER_FROM_DIFFUSION_MODEL, this, nextTime);

        //now put the current step data container to all steps data map
            if(!currentStepDataContainer.getDiffusionDataMap().isEmpty()){
                getAllStepsDiffusionData().put(currentTime, currentStepDataContainer);
            }



            // clear the contents
            getGlobalContentFromAgents().clear();
            getLocalContentFromAgents().clear();
//            latestBlockageTimes.clear(); // this is maintained through out the simulation, so no need to clear per step

//        }


        //+1 to avoid returning empty map for diffusion data for first step (toKey = fromKey)
        SortedMap<Double, DiffusionDataContainer> periodicDiffusionData =   getAllStepsDiffusionData().subMap(lastUpdateTimeInMinutes,currentTime+1);
        lastUpdateTimeInMinutes = currentTime;

        return (currentStepDataContainer.getDiffusionDataMap().isEmpty()) ? null : periodicDiffusionData;

    }

    @Override
    public void receiveData(double time, String dataType, Object data) { // data package from the BDI side

        int influencecount = 0;
        int updateCount = 0;

        switch (dataType) {
            case Constants.DIFFUSION_DATA_CONTAINDER_FROM_BDI: // update Diffusion model based on BDI updates
                DiffusionDataContainer dataContainer = (DiffusionDataContainer) data;
                if (!(data instanceof DiffusionDataContainer)) {
                    this.getSnManager().getSocialNetworkDiffusionLogger().error("received unknown data: " + data.toString());
                    break;
                }

                HashMap<String, DiffusionContent> bdiModelcontentsMap = ( HashMap<String, DiffusionContent>) ((DiffusionDataContainer) data).getDiffusionDataMap();

                for(Map.Entry entry: bdiModelcontentsMap.entrySet()) {
                    String agentId = (String) entry.getKey();
                    DiffusionContent bdiDiffusionContent = (DiffusionContent) entry.getValue();

                    //process local contents
                    for (Map.Entry contentEntry : bdiDiffusionContent.getContentsMapFromBDIModel().entrySet()) {
                        String localContentType = (String) contentEntry.getKey();
                        String[] params = (String[]) contentEntry.getValue();
                        String content = params[0];
                        this.getSnManager().getSocialNetworkDiffusionLogger().trace("Agent {} received local content type {}. Content: {} ", agentId, localContentType,content);

                        if (localContentType.equals(DeePerceptList.BLOCKAGE_INFLUENCE)) { // store Blockage Influence content type
                            Set<String> agents = (getLocalContentFromAgents().containsKey(content)) ? getLocalContentFromAgents().get(content) :
                                    new HashSet<>();
                            agents.add(agentId);
                            getLocalContentFromAgents().put(content, agents);
                            influencecount++;
                        }
                        else if (localContentType.equals(DeePerceptList.BLOCKAGE_UPDATES)) {
                            String[] params2 = (String[]) bdiDiffusionContent.getContentsMapFromBDIModel().get(localContentType);
                            String blockageName = params2[0];
                            double blockedPercepttime = Double.valueOf(params2[1]);

                            // first place where blockage updates are received
                            if (!blockageTimesMap.containsKey(blockageName)){
                                HashMap<String,Double> latestBlockageTimes = new HashMap<String,Double>();
                                blockageTimesMap.put(blockageName,latestBlockageTimes);
                            }
                            HashMap<String,Double> latestBlockageTimes = blockageTimesMap.get(blockageName);
                            latestBlockageTimes.put(agentId, blockedPercepttime);
                            updateCount++;
                        }
                        else {
                            this.getSnManager().getSocialNetworkDiffusionLogger().error("unknown local content received: {} for agent {}", localContentType, agentId);
                        }

                    }

                    //process global (broadcast) contents
                    for (String globalContent : bdiDiffusionContent.getBroadcastContentsMapFromBDIModel().keySet()) {
                        this.getSnManager().getSocialNetworkDiffusionLogger().debug("received global content " + globalContent);
                        if (!getGlobalContentFromAgents().contains(globalContent)) {
                            getGlobalContentFromAgents().add(globalContent);
                        }
                        String[] params = (String[]) bdiDiffusionContent.getBroadcastContentsMapFromBDIModel().get(globalContent);
                        // do something with parameters

                    }

                    //process SN actions
                    for (String action : bdiDiffusionContent.getSnActionsMapFromBDIModel().keySet()) {
                        Object[] params = bdiDiffusionContent.getSnActionsMapFromBDIModel().get(action);
                        // do something with parameters
                    }
                }
                break;
            default:
                throw new RuntimeException("Unknown data type received: " + dataType);
        }

        this.getSnManager().getSocialNetworkDiffusionLogger().info("time {}: updates from BDI: {} agents received blockage information, {} agents received blockage updates", time,influencecount,updateCount);
    }

    protected void stepDiffusionProcess(DiffusionDataContainer dataContainer, String contentType, double timestep) {
        this.getSnManager().stepDiffusionModels(timestep); // step the diffusion model

        if (this.getSnManager().getDiffModels()[0] instanceof ICModel) {
            ICModel icModel = (ICModel) this.getSnManager().getDiffModels()[0];
//            icModel.recordCurrentStepSpread((int)timestep); // this is done in SNManager

            HashMap<String, ArrayList<String>> latestUpdate = icModel.getLatestDiffusionUpdates();
            if (!latestUpdate.isEmpty()) {

                for(Map.Entry<String,ArrayList<String>> contents: latestUpdate.entrySet()) {
                    String content = contents.getKey();
                    ArrayList<String> agentIDs = contents.getValue();
                    this.getSnManager().getSocialNetworkDiffusionLogger().info("time {}: {} agents activated for content {}",(int)timestep,agentIDs.size(),content);

                    for(String id: agentIDs) { // for each agent create a DiffusionContent and put content type and parameters
                        //   DiffusionContent content = dataContainer.getOrCreateDiffusedContent(id);
                        String[] params = {content};
                        dataContainer.putContentToContentsMapFromDiffusionModel(id,contentType, params);
                        //  content.getContentsMapFromDiffusionModel().put(contentType,params );
                    }
                }

            }

        }


    }

    public void updateAndShareLatestBlockageTimeWithNeighbours(DiffusionDataContainer dataContainer, double time) {

        int bdiUpdateCounter = 0;

       for(Map.Entry<String,HashMap<String,Double>> entry: blockageTimesMap.entrySet()) {

           String blockageName = entry.getKey();
           HashMap<String,Double> latestBlockageTimes = entry.getValue();

           for (SocialAgent agent : this.getSnManager().getAgentMap().values()) {
               double current_latest_time = -1;
               String agentid = String.valueOf(agent.getID());

               if (latestBlockageTimes.containsKey(agentid)) {
                   current_latest_time = latestBlockageTimes.get(agentid);
               }
               if (agent.getLinkMap().size() == 0 || !agent.getAdoptedContentList().contains(DeePerceptList.BLOCKAGE_INFLUENCE))  {
                   // no neighbours, so only BDI can update its observed time.  OR if inactive
                   continue;
               }
               for (int neiID : agent.getLinkMap().keySet()) { // iterate over neighbours and collect their latest observed times.
                   String neiId = String.valueOf(neiID);
                   if (latestBlockageTimes.containsKey(neiId)) { // if neighbour already has an observed time
                       if (latestBlockageTimes.get(neiId) > current_latest_time) {
                           current_latest_time = latestBlockageTimes.get(neiId);
                       }
                   }

               }

               // current_latest_time > 0 && !latestBlockageTimes.containsKey(agentid) : a neighbour knows the latest blockage observed time
               // current_latest_time > latestBlockageTimes.get(agentid): agent has found an updated time than the current one
               if( (current_latest_time > 0 && !latestBlockageTimes.containsKey(agentid)) || latestBlockageTimes.containsKey(agentid) && (current_latest_time > latestBlockageTimes.get(agentid) )) { // if both conditions meet, then update SN ds and send to BDI side

                       latestBlockageTimes.put(agentid,current_latest_time); // update SN side data structure

                       //package update to send to the BDI agent
                       Object[] params = {blockageName, current_latest_time};
                       dataContainer.putContentToContentsMapFromDiffusionModel(agentid, DeePerceptList.BLOCKAGE_UPDATES, params);

                        bdiUpdateCounter++;

               }

           }
       }

            this.getSnManager().getSocialNetworkDiffusionLogger().info("time {}: updates to BDI: {} agents sending blockage updates",time,bdiUpdateCounter);

    }




}
