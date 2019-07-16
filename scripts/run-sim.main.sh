#!/bin/bash
###
# this is the  main script for running simulations local/remote. all inputs are hardcoded and do not use the config files in exp/scripts/config dir.
# steps:
#	build the diffusion-evacuation-experiments project
#	get the configurations from the LHS script (generateConfigsFromSamples.sh) -> generates the main config file in workspace/configs
#	local:
#		for each sample-run copy data neeeded and replace hawkesbury.xml config
#		exec main-run.sh <input args>
#	remote:
#		for each sample create a single out dir by - copy data neeeded and replace hawkesbury.xml config
#		copy inputs for the main-run.sh script to the file run-properties.csv
#		scp ressults dir with sample dirs - replicate the sample dirs into the required number of out dirs
#		scp *  inside exp/scripts/remote dir
#		run PBS script
#
#
###

##directories
#sn_dir="../../diffusion-evacuation-experiments"
#exp_dir="../../experiments/scripts"
config_dir="../scenarios/grid"
LatinHyperSamplesDir="../latin-hypercube-samples"
#remote_run_scripts_dir="$exp_dir/remote/scripts"
results_dir="../../results"

#matsim_output_dir="/home/chaminda/workspace/sub_projects/NictaMatsim/output"
#act_events="/home/chaminda/workspace/experiment_data/matsim_data"


##test directories
#test_act_events="/home/chaminda/workspace/experiments/experiments/scripts/sh/test/activity_events"
#test_output1="/home/chaminda/workspace/experiments/experiments/scripts/sh/test/output1-actEvents"
#test_output2="/home/chaminda/workspace/experiments/experiments/scripts/sh/test/output2-datafiles"


## Parsing input parameters

if [ $# -eq 0 ]; then
  printf "script failed. specify arguments: scenario (baseline=bl/diffusion=d) startSample endSample startRun endRun expType (local/remote) \n" >&2
  exit 1
 fi

if [ $# -ne 0 ];
then
  scenario_type=$1
  startSample=$2
  endSample=$3
  startRun=$4
  endRun=$5
  expType=$6
fi


echo "SCENARIO  = $scenario | SAMPLES  start: $startSample end: $endSample | RUNS start: $startRun end: $endRun | execution type: $expType"



	#1.build the project
	#cd .. && mvn clean install -DskipTests=true && cd - # back to where this script is# build project
  if [ ! -f ../target/dee-1.0-SNAPSHOT.jar ]; then
 	printf " file  target/dee-1.0-SNAPSHOT.jar not found, probably build is not succeesful -  aborting! \n";
  >&2
  exit 1
 fi


	#generate a unique timestamp for the output dirs
	timestamp=$(date +"%h%d-%H-%M-%S")
  if [ $scenario_type == "bl" ]; then timestampDirName=$timestamp-baseline;
elif [ $scenario_type == "d" ]; then timestampDirName=$timestamp-diffusion;
  else printf "scenario option $scenario is unknown, aborting!"; exit 1;
  fi

printf "new directory name: $timestampDirName \n"
timeStampDir=$results_dir/$timestampDirName
	#settting the results directory depending on running locally or remotely

	#2. create dirs if does not exist and cd
	echo $timeStampDir #&&  pwd && mkdir -p $timeStampDir


  #copy scenario configurations
#  cp $config_dir $timeStampDir
#	echo $LatinHyperSamplesDir
	#3. generate the configs from the samples
#	./generateConfigsFromSamples.sh  $scenario $LatinHyperSamplesDir $timestamp


	#current dir - testScripts

############# local-running #################################################################

	if [ "$expType" = "l" ]; then

		run=$startRun
		sample=$startSample
		#4. ready out dirs and run - for each sample, create runs number of output dirs, copy the essentials from the build of sn-integration and the configuration hawkesbury file
		while [ "$sample" -le "$endSample" ]; do


		#create sample dir and the run-status file for the sample
    configDir=$timeStampDir-sample$sample
		#mkdir -p $configDir
    logFile=$results_dir/log-$timestampDirName.txt  # create run-status file
    touch $logFile
		printf "running $runs simulation runs...\n" > $logFile


		while [ "$run" -le "$endRun" ]; do

			#4.1 mkdir out directory
			outDir=$configDir/run$run
			mkdir -p $outDir/scenarios

			#4.2 copy configurations and jar file.
      cp -r $config_dir $outDir/scenarios/
      cp -r ../scenarios/xsd $outDir/scenarios/
      cp  ../target/dee-1.0-SNAPSHOT.jar $outDir


			#4.3 copy  main-run scripts
			#cp -r $exp_dir/main-run.sh $outDir

			#4.4 run config modifying script
      ./createConfigurationsFromLHS.sh  $scenario_type $outDir  $sample

      #4.5 FIXME  save diffs of config files after modifications
      #diff $expDiffusionConfig $outDir/sample$sample/$(basename $expDiffusionConfig) >> $outDir/diffusion_config_diffs.txt


			#4.5 run main-run script: #looping over samples and parrelising the simulation runs and conducting experiments
			#CHNAGE using outdir mai-run script
		#	$outDir/main-run.sh  $run $outDir $sample & # why dont you execute the main-run in the out directory?
    cd $outDir && java -cp dee-1.0-SNAPSHOT.jar io.github.agentsoz.dee.Main --config scenarios/grid/dee-main.xml && cd -


			run=`expr "$run" + 1`;

			done

			run=$startRun
			sample=`expr "$sample" + 1`;

		done ;
	 wait
	 fi

############# remote-running #################################################################
: '
	if [ "$expType" = "r" ]; then

		printf "setting up the remote execution process started...\n"

		run=$startRun
		sample=$startSample
		#4. make out dirs ready to run - for each sample, create runs number of output dirs, copy the essentials from the build of sn-integration and the configuration hawkesbury file
		while [ "$sample" -le "$endSample" ]; do # samples loop - only one outdir per sample


			#4.1 mkdir sample and out directories in the results dir
			outDir=$results_dir/s$scenario/$timestamp/sample$sample/out
			mkdir -p $outDir

			#4.2 copy sn.jar, hawkesbury config, libs and the scripts from sn integration and the experiments project
			cp -r $sn_dir/target/social_network_integration-0.0.1-SNAPSHOT/{run.sh,checkForErrors.sh,social_network_integration-0.0.1-SNAPSHOT.jar,lib,panic_data,test_data} $outDir
			mkdir -p $outDir/case_studies && cp -r $sn_dir/target/social_network_integration-0.0.1-SNAPSHOT/case_studies/hawkesbury $outDir/case_studies/

			#4.3 copy the extractActivities, main-run scripts -- experiments jar file???
			cp -r $exp_dir/main-run.sh $outDir

			#4.4 replace with the hawkesbury configuration file
			rm $outDir/case_studies/hawkesbury/hawkesbury.xml
			cp -r $config_dir/s$scenario/$timestamp/sample$sample/hawkesbury.xml $outDir/case_studies/hawkesbury

			#test purpose
			#cp -r $sn_dir/test_data $outDir

			sample=`expr "$sample" + 1`;

		done




		#5. create run.properties in results/timestamp dir   DO NOT USE printf to create a file
		echo "$startSample,$endSample,$startRun,$endRun" > $results_dir/s$scenario/$timestamp/run-properties.csv


		#6. copy results/timestamp directory
		workspaceDir=/short/ij2/cbu595/workspace #workspace

		ssh cbu595@raijin.nci.org.au "mkdir -p $workspaceDir/results/s$scenario"
		scp -r $results_dir/s$scenario/$timestamp cbu595@raijin.nci.org.au:$workspaceDir/results/s$scenario/   #copy  local/tt to created remote/tt dir

		remoteTTDir=$workspaceDir/results/s$scenario/$timestamp
		#7. replicate the out directories in remote

		#re-initialiase the sample and runs after the above while loop
		run=$startRun
		sample=$startSample

			while [ "$sample" -le "$endSample" ]; do # samples loop
				while [ "$run" -le "$endRun" ]; do # runs loop

				 ssh cbu595@raijin.nci.org.au "cp -r $remoteTTDir/sample$sample/out $remoteTTDir/sample$sample/out$run"

				run=`expr "$run" + 1`;
				done
			# remove the out dir used for the replicating
			ssh cbu595@raijin.nci.org.au "rm -r $remoteTTDir/sample$sample/out"

			run=$startRun
			sample=`expr "$sample" + 1`;

		done

		#7. copy PBS script and bash script needed to run to results/timestamp
		scp -r $remote_run_scripts_dir/* cbu595@raijin.nci.org.au:$workspaceDir/results/s$scenario/$timestamp #copy  PBS script and the script geting called by the PBS script

		#FINALLY - run the PBS script - ssh connection will be broken from here
		#ssh cbu595@raijin.nci.org.au "bash workspace/results/s2/Oct09-15-22-47/remtoe-main-run.sh" - works
		ssh cbu595@raijin.nci.org.au "cd $workspaceDir/results/s$scenario/$timestamp/ && qsub jobscript.pbs " #qsub jobscript.pbs




	fi

wait
printf "main-script shutting down \n"
exit

'
