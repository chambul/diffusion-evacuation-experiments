#!/usr/bin/env bash


if [ $# -ne 3 ]; then
  printf "script failed. specify arguments: scenario(bl/sn)  config dir  run directory \n " >&2
  exit 1
 fi


  scenario=$1
  src_dir=$2
  run_dir=$3


  if [ $scenario == "bl" ] || [ $scenario == "sn" ];
  then
    # printf "SCENARIO: $scenario | src dir: $src_dir run dir: $run_dir"
    echo " creating directory $run_dir \n"
    mkdir -p $run_dir/scenarios
  else
    printf " invalid scenarion parameter $scenario, aborting \n" >&2
    exit 1
  fi




out_dir="output"

#copy needed files
cp -r $src_dir/scenarios/hawkesbury $run_dir/scenarios
cp -r $src_dir/scenarios/xsd $run_dir/scenarios
cp -r $src_dir/scripts/run.sh $run_dir
cp -r $src_dir/scripts/extract_basic_stats.sh $run_dir
[ ! -f $src_dir/target/dee-1.0-SNAPSHOT.jar ] && echo "dee JAR file not found, aborting" && exit 1
cp $src_dir/target/dee-1.0-SNAPSHOT.jar $run_dir

#modify output directories to ./output
# echo "modifying configuration files to redirect the output to output directory"

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
# run=`expr "$startRun" + 1`;
# run_dir_start_run=$results_dir/$timestamp/"$scenario-run$startRun"
#
# printf "makikng copies until $endRun run directories"
#
# 		while [ "$run" -le "$endRun" ]; do
#
#       run_dir=$results_dir/$timestamp/"$scenario-run$run"
#
#       #copy run1 directory to run2, run3
#       mkdir -p $run_dir
#       cp -r $run_dir_start_run/* $run_dir
#
#     run=`expr "$run" + 1`;
# 		done ;

#running run scripts
#run run.sh script
# run=$startRun
#
# 		while [ "$run" -le "$endRun" ]; do
#
#       echo "run$run"
#      cd $run_dir
#      sh run.sh &> stdout.out
#      cd -
#
#     run=`expr "$run" + 1`;
# 		done ;
