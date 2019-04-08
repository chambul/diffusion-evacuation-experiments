#!/bin/bash
###
# main run of the experiment
# 
###

##test directories
#test_act_events="/home/chaminda/workspace/experiments/experiments/scripts/sh/test/activity_events"
#test_output1="/home/chaminda/workspace/experiments/experiments/scripts/sh/test/output1-actEvents"
#test_output2="/home/chaminda/workspace/experiments/experiments/scripts/sh/test/output2-datafiles"

statusFile=../run-status.txt
logCheckStatusFile="log-status.txt"
## Parsing input parameters
if [ $# -ne 0 ]; then
  run=$1
  outDir=$2
  sample=$3
  #timestamp=$4
fi

# change dir to the out dir
cd $outDir

#function to execute the main run
function mainrun {
	echo         
	echo "simulation run $run started.."

	# run the simlation
	./run.sh	

	# catch issues in haw and matsim logs:
	./checkForErrors.sh
	
	# compressing data
	[ -f "hawkesbury.log" ] && gzip hawkesbury.log 
	#gzip ./case_studies/hawkesbury/haw_pop_route_defined.xml	

		
	logStatus=`cat $logCheckStatusFile`
	#echo "$logStatus"
	if [ $logStatus -eq 1 ] 
	then
 		status="completed"
		echo "sample$sample-run$run: $status" >> $statusFile  #append
	else
 		status="failed"
		echo "sample$sample-run$run: $status" >> $statusFile  #append

		# get pid of the current process and kill - does it kill hanging processes?
		pid=$$	
		kill $pid
			
	fi
	
	



}


mainrun


exit
