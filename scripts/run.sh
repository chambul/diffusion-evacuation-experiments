#!/usr/bin/env bash

[ -d output ] && { printf "run.sh:removing any existing output directories.." ; rm -r output; }


DIR=`dirname "$0"`
PROGRAM='java -Xmx6g -cp dee-1.0-SNAPSHOT.jar io.github.agentsoz.dee.Main --config scenarios/hawkesbury/social_network_experiments_diffusion.xml'
#cp=class path for the jar to run. Important to give dee main class otherwise wil run on ees main class
#DEFAULT_ARGS='-c scenarios/nicta_original/nicta.xml -l hawkesbury.log -level DEBUG'



# print full command
CMD="$PROGRAM $DEFAULT_ARGS $USER_ARGS"
printf "running:\n  "
printf "started on `date +"%B %d, %Y at %r"` \n  "
printf "$CMD\n  "

#$CMD # prints all the logs to the console.
#2>&1 redirects channel 2 (stderr/standard error) into channel 1 (stdout/standard output), such that both is written as stdout.
#It is also directed to the given output file as of the tee command.
$CMD 2>&1 | tee -a stdout.out # -a append to the same file
#/dev/null 2>&1 #does not print all
printf "finished on `date +"%B %d, %Y at %r"` \n\n"
