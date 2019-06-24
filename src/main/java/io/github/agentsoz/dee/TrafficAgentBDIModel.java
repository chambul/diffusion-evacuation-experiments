package io.github.agentsoz.dee;

import io.github.agentsoz.dataInterface.DataClient;
import io.github.agentsoz.dee.agents.TrafficAgent;
import io.github.agentsoz.ees.Constants;
import io.github.agentsoz.ees.DiffusedContent;
import io.github.agentsoz.ees.JillBDIModel;
import io.github.agentsoz.ees.SNUpdates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;


public class TrafficAgentBDIModel extends JillBDIModel {


    private final Logger logger = LoggerFactory.getLogger(TrafficAgentBDIModel.class);

    public TrafficAgentBDIModel(String[] initArgs) {
        super(initArgs);

    }


    /**
     * Creates a listener for each type of message we expect from the DataServer
     * @return
     */
    private Map<String, DataClient> createDataListeners() {
        Map<String, DataClient> listeners = new HashMap<>();

        listeners.put(Constants.TAKE_CONTROL_BDI, (DataClient<io.github.agentsoz.bdiabm.v2.AgentDataContainer>) (time, dataType, data) -> {
            //takeControl(data);
            synchronized (getSequenceLock()) {
                getAgentDataContainer().clear();
                clearAgentSNUpdates();
                if(!this.getContentsMap().isEmpty()){
                    sendSocialNetworkMessagesToAgents(data);
                }
                takeControl(time, data);
                publishSNUpdates();
                this.getDataServer().publish(Constants.AGENT_DATA_CONTAINER_FROM_BDI, getAgentDataContainer());
            }
        });

//        listeners.put(Constants.FIRE_ALERT, (DataClient<Double>) (time, dataType, data) -> {
//            fireAlertTime = time;
//        });

        listeners.put(Constants.DIFFUSION, (DataClient<Map<String, DiffusedContent>>) (time, dataType, data) -> {
            this.setContentsMap(data);
        });

        listeners.put(Constants.SOCIAL_NETWORK_CONTENT, (DataClient<String[]>) (time, dataType, data) -> {
            logger.warn("Ignoring received data of type {}", dataType);
        });

        return listeners;
    }

    private void clearAgentSNUpdates() {


        for(String id: mapJillToMATsimIds.keySet()){

            TrafficAgent agent = (TrafficAgent) getAgent(Integer.valueOf(id));
            agent.clearSNUpdate();

        }
    }

    private void publishSNUpdates(){

        HashMap<String,SNUpdates> snUpdatesHashMap = new HashMap<>();

        for(String id: mapJillToMATsimIds.keySet()){
            TrafficAgent agent = (TrafficAgent) getAgent(Integer.valueOf(id));
            SNUpdates update = agent.getSNUpdates();
            if(update != null) {
                snUpdatesHashMap.put(id,update);
            }

        }

        //now publish data
        this.getDataServer().publish(Constants.BDI_REASONING_UPDATES,snUpdatesHashMap);
    }
}
