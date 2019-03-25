#!/bin/bash

matFile=$1
agentType="BushfireAgentV1"

sed "s/>io[.,a-z,A-Z]*/>io.github.agentsoz.dee.agents.bushfire.${agentType}/g" $matFile > scenario_matsim_plans-dee-$agentType.xml


