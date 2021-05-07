#!/usr/bin/env bash


if [ $# -ne 4 ]; then
  printf "script failed. specify arguments: create results directory(y) or use existing latest directory(n) \n scenario(bl/sn) \n startRun \n endRun \n" >&2
  exit 1
 fi

  create_dir=$1
  scenario=$2
  startRun=$3
  endRun=$4

  if [ $scenario == "bl" ] || [ $scenario == "sn" ];
  then
    printf "SCENARIO: $scenario | start run: $startRun end: $endRun"

  else
    printf " invalid scenarion parameter $scenario, aborting \n" >&2
    exit 1
  fi


results_dir="../../results-chapter4"
src_dir="../../diffusion-evacuation-experiments"
out_dir="output"
run_dir="run"



#generate a unique timestamp for the output dirs
if [ $create_dir == "y" ];
then
  printf "creating new timestamp directory...\n"
  timestamp=$(date +"%h%d-%H-%M") # per minute based
elif [ $create_dir == "n" ];
then
  printf " finding the latest existing timestamp directory.. \n "
  timestamp=$(ls -1t $results_dir | head -n 1) # get the latest directory
else
  printf "invalid argmument for creating directory: $create_dir, aboring"
  exit 1
fi

read -p "selected timestamp directory $timestamp. Proceed the script? " -n 1 -r
echo    # (optional) move to a new line
if [[ $REPLY = "n" ]]
then
    exit 0
fi




run=$startRun
run_dir=$results_dir/$timestamp/"$scenario-run$run"

echo " creating directory $run_dir"
mkdir -p $run_dir/scenarios



#copy needed files
cp -r $src_dir/scenarios/hawkesbury $run_dir/scenarios
cp -r $src_dir/scenarios/xsd $run_dir/scenarios
cp -r $src_dir/scripts/run.sh $run_dir
cp -r $src_dir/scripts/extract_basic_stats.sh $run_dir
[ ! -f $src_dir/target/dee-1.0-SNAPSHOT.jar ] && echo "dee JAR file not found, aborting" && exit 1
cp $src_dir/target/dee-1.0-SNAPSHOT.jar $run_dir

#modify output directories to ./output
echo "modifying configuration files to redirect the output to output directory"

dee_main_config="$run_dir/scenarios/hawkesbury/social_network_experiments_diffusion.xml"
expDiffusionConfig=$run_dir/scenarios/hawkesbury/"scenario_diffusion_config.xml"
matim_main_config=$run_dir/scenarios/hawkesbury/"matsim_main.xml"

      sed -i '' -e "s#<opt id\=\"jLogFile\">[-_[:alnum:]./]*#<opt id=\"jLogFile\">${out_dir}/jill.log#"  $dee_main_config
      sed -i '' -e "s#<opt id\=\"jOutFile\">[-_[:alnum:]./]*#<opt id=\"jOutFile\">${out_dir}/jill.out#"  $dee_main_config
      sed -i '' -e "s#<opt id\=\"outputDir\">[-_[:alnum:]./]*#<opt id=\"outputDir\">${out_dir}/matsim#"  $dee_main_config
      if [ $scenario == "bl" ];
      then
        sed -i '' -e "s#<opt id\=\"configFile\">[-_[:alnum:]./<>]*#<!-- & -->#"  $dee_main_config
      fi

      sed -i '' -e "s#log_file\=\"[-_[:alnum:]./]*#log_file\=\"${out_dir}/diffusion.log#"  $expDiffusionConfig #logfile
      sed -i '' -e "s#out_file\=\"[-_[:alnum:]./]*#out_file\=\"${out_dir}/diffusion.out#"  $expDiffusionConfig #outfile
      sed -i '' -e "s#<out_file>[-_[:alnum:]./]*#<out_file>./${out_dir}/diffusion.out#" $expDiffusionConfig



#TODO loop over runs
run=`expr "$startRun" + 1`;
run_dir_start_run=$results_dir/$timestamp/"$scenario-run$startRun"

printf "makikng copies until $endRun run directories"

		while [ "$run" -le "$endRun" ]; do

      run_dir=$results_dir/$timestamp/"$scenario-run$run"

      #copy run1 directory to run2, run3
      mkdir -p $run_dir
      cp -r $run_dir_start_run/* $run_dir

    run=`expr "$run" + 1`;
		done ;

#running run scripts
#run run.sh script
run=$startRun

		while [ "$run" -le "$endRun" ]; do

      echo "run$run"
     cd $run_dir
     sh run.sh &> stdout.out
     cd -

    run=`expr "$run" + 1`;
		done ;
