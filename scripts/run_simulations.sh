#!/usr/bin/env bash


if [ $# -ne 3 ]; then
  printf "script failed. specify arguments: scenario(bs/sn) startRun endRun \n" >&2
  exit 1
 fi

  scenario=$1
  startRun=$2
  endRun=$3


  echo "SCENARIO: $scenario | start run: $startRun end: $endRun"


results_dir="../../results-chapter4"
src_dir="../../diffusion-evacuation-experiments"
out_dir="output"
run_dir="run"



#generate a unique timestamp for the output dirs
timestamp="test" #$(date +"%h%d-%H-%M-%S")
run=$startRun
run_dir=$results_dir/$timestamp/"run$run"

echo " creating directory $run_dir"
mkdir -p $run_dir/scenarios


#copy needed files
cp -r $src_dir/scenarios/hawkesbury $run_dir/scenarios
cp -r $src_dir/scripts/run.sh $run_dir
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

      sed -i '' -e "s#log_file\=\"[-_[:alnum:]./]*#log_file\=\"${out_dir}/diffusion.log#"  $expDiffusionConfig #logfile
      sed -i '' -e "s#out_file\=\"[-_[:alnum:]./]*#out_file\=\"${out_dir}/diffusion.out#"  $expDiffusionConfig #outfile
      sed -i '' -e "s#<out_file>[-_[:alnum:]./]*#<out_file>${out_dir}/diffusion.out#" $expDiffusionConfig



#TODO loop over runs
run=`expr "$startRun" + 1`;

		while [ "$run" -le "$endRun" ]; do

      #copy run1 directory

      #run run.sh script
      #sh $run_dir/run.sh

    run=`expr "$run" + 1`;
		done ;
