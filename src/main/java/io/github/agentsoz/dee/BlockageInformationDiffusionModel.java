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
import io.github.agentsoz.ees.DiffusedContent;
import io.github.agentsoz.ees.DiffusionModel;
import io.github.agentsoz.ees.SNUpdates;
import io.github.agentsoz.socialnetwork.ICModel;
import io.github.agentsoz.socialnetwork.SNConfig;
import io.github.agentsoz.socialnetwork.SocialAgent;
import io.github.agentsoz.socialnetwork.SocialNetworkManager;
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
public class BlockageInformationDiffusionModel extends DiffusionModel implements DataSource<HashMap<String,DiffusedContent>>, DataClient<Object> {

    private final Logger logger = LoggerFactory.getLogger(BlockageInformationDiffusionModel.class);

    private double lastUpdateTimeInMinutes = -1;
    private Time.TimestepUnit timestepUnit = Time.TimestepUnit.SECONDS;
    private String configFile = null;
    private List<String> agentsIds = null;

    Map<String, Set> localContentFromAgents;
    HashMap<String,Double> latestBlockageTimes;
    ArrayList<String> globalContentFromAgents;

    public BlockageInformationDiffusionModel(String configFile) {
        super(configFile);
    }

    public BlockageInformationDiffusionModel(Map<String, String> opts, DataServer dataServer, List<String> agentsIds) {
        super(opts, dataServer, agentsIds);
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
        this.getDataServer().subscribe(this, Constants.BDI_REASONING_UPDATES);

    }


    /*
            1. Update social states (Same as Diffusion model)
            2. Share blockage times with dierct neighbours -> update Hash
            3. run IC diffusion process -> update Hash
            4.
     */
    @Override
    public HashMap<String,DiffusedContent> sendData(double timestep, String dataType) {
        Double nextTime = timestep + SNConfig.getDiffturn();

        //create the data structure that is passed to the BDI side
        HashMap<String, DiffusedContent> currentStepDiffusedContents = new HashMap<>();

        if (nextTime != null) {
            getDataServer().registerTimedUpdate(Constants.DIFFUSION, this, nextTime);
            // update the model with any new messages form agents
            ICModel icModel = (ICModel) this.getSnManager().getDiffModel();

            if (!localContentFromAgents.isEmpty()) { // update local content
                Map<String, String[]> map = new HashMap<>();
                for (String key : localContentFromAgents.keySet()) {
                    Object[] set = localContentFromAgents.get(key).toArray(new String[0]);
                    String[] newSet = new String[set.length];
                    for (int i = 0; i < set.length; i++) {
                        newSet[i] = (String)set[i];
                    }
                    map.put(key,newSet);
                    logger.info(String.format("At time %.0f, total %d agents will spread new message: %s", timestep, newSet.length, key));
                    logger.info("Agents spreading new message are: {}", Arrays.toString(newSet));
                }
                icModel.updateSocialStatesFromLocalContent(map);
            }

            if(!globalContentFromAgents.isEmpty()) { // update global contents

                logger.info("Global content received to spread: {}", globalContentFromAgents.toString());
                icModel.updateSocialStatesFromGlobalContent(globalContentFromAgents);

            }

            //share blockage time infomration with ONLY direct neighbours (no processing of whole agent list)
            for(Map.Entry entry: latestBlockageTimes.entrySet()){
                String id = (String) entry.getKey();
                double time = (double) entry.getValue();
                updateAndShareLatestBlockageTimeWithNeighbours(id,time, currentStepDiffusedContents);
            }


            // step the model before begin called again
            stepDiffusionProcess(currentStepDiffusedContents);

            // clear the contents
            globalContentFromAgents.clear();
            localContentFromAgents.clear();
            latestBlockageTimes.clear(); // clear last step blockage percept times

        }

        double currentTime = Time.convertTime(timestep, timestepUnit, Time.TimestepUnit.MINUTES);
        lastUpdateTimeInMinutes = currentTime;
        return (currentStepDiffusedContents.isEmpty()) ? null : currentStepDiffusedContents;

    }

    @Override
    public void receiveData(double time, String dataType, Object data) { // data package from the BDI side

        switch (dataType) {
            case Constants.BDI_REASONING_UPDATES: // update Diffusion model based on BDI updates
                if (!(data instanceof SNUpdates)) {
                    logger.error("received unknown data: " + data.toString());
                    break;
                }

                SNUpdates newUpdates = (SNUpdates) data;
                String agentId = String.valueOf(newUpdates.getAgentId());

                //process local contents
                for(String localContent: newUpdates.getContentsMap().keySet()){
                    logger.debug("Agent {} received local content type {}. Message: {}",agentId,localContent);

                    if(localContent.equals(DeePerceptList.BLOCKAGE_INFLUENCE)) {
                        Set<String> agents = (localContentFromAgents.containsKey(localContent)) ? localContentFromAgents.get(localContent) :
                                new HashSet<>();
                        agents.add(agentId);
                        localContentFromAgents.put(localContent, agents);
                        String[] params = (String[]) newUpdates.getContentsMap().get(localContent);
                        String msg = params[0];   // do something with parameters
                    }

                    else if (localContent.equals(DeePerceptList.BLOCKAGE_UPDATES)){
                        Object[] params = (Object[]) newUpdates.getContentsMap().get(localContent);
                        String blockageName = (String) params[0];
                        double blockedPercepttime = (double) params[1];
                        latestBlockageTimes.put(agentId,blockedPercepttime );  //#FIXME time should be updated per blockage
                    }
                    else{
                        logger.error("unknown local content received: {} for agent {}",localContent, agentId);
                    }

                }

                //process global (broadcast) contents
                for(String globalContent: newUpdates.getBroadcastContentsMap().keySet()){
                    logger.debug("received global content " + globalContent);
                    if(!globalContentFromAgents.contains(globalContent)) {
                        globalContentFromAgents.add(globalContent);
                    }
                    String[] params = (String[])newUpdates.getContentsMap().get(globalContent);
                    // do something with parameters

                }

                //process SN actions
                for(String action: newUpdates.getSNActionsMap().keySet()){
                    Object[] params = newUpdates.getContentsMap().get(action);
                    // do something with parameters
                }
                break;
            default:
                throw new RuntimeException("Unknown data type received: " + dataType);
        }
    }

    public void updateAndShareLatestBlockageTimeWithNeighbours(String id,double time, HashMap<String,DiffusedContent> contentHashMap){ // no comparison needed, time of blocked percept will be the latest for all agents.

        SocialTrafficAgent agent = (SocialTrafficAgent) this.getSnManager().getAgentMap().get(id); // update own time.
        agent.setLastKnownBlockageTime(time);

        for (int neighbourID : agent.getLinkMap().keySet()){
            SocialTrafficAgent neighbourAgent = (SocialTrafficAgent) this.getSnManager().getAgentMap().get(neighbourID);
            neighbourAgent.setLastKnownBlockageTime(time);

            //package update to send to the BDI agent
            DiffusedContent content = getOrCreateDiffusedContent(id,contentHashMap);
            Object[] params = {time};
            content.getContentsMap().put(DeePerceptList.BLOCKAGE_UPDATES,params );
        }


    }

}
