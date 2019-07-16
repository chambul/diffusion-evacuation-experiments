#!/bin/bash
#

# goto LHSdir
# copy initial config file to LHS dir
# xls -> csv -> remove first lines -> modified-samples.csv
# for each row in modified-samples :
#	get each column
#	find keyword in initial config -> replace the value  and create a new file as sample.xml
#	mv the file tot the configs dir
#
# to TEST : this script can only be executed alone with some random inputs args
# Note:for S3: avglinks will be replaced for each networktype
#
#

: '
input args  = scenario timestamp
function :  extract hypercube values and genearate new config (hawkesbury) files for each sample
Note:
1. make sure that the patterns you enter to capture parameters in the initial hawkesbury config file will not replace other parts of the parameters - make the pattern unique- check by searching the .xml file
'


#---------------------function : retreive a unique xml tag value ---------------------------------------------------

function getSingleTagValue() {
xmlFile=$1
tag=$2
#echo "retreiving xml tag value of $tag from $xmlFile.."
tagVal=`python -c 'from getOutputFiles import *; print " ".join(getXMLTagValue("'$xmlFile'","'$tag'"))'`
echo $tagVal ## whatever is echoed in the function is returned to the variable
return
}




if [ $# -ne 3 ]; then
 	printf " This script creates configurations for batch run. Enter following args: \n";
	printf " 1. scenarioType = d/bl(diffusion/baseline) 2. specific run  directory 3. config number  \n ";
  >&2
  exit 1
 fi

#--------------------starting script--------------------------------------------
printf "\n\n createConfigurationsFromLHS.sh script started.... \n"


# set the python di using bash xml_grep : python functions doesnt work until the path is set
#path=`xml_grep "local-python-path" $batchRunMainConfig --text_only`

path=python #FIXME hardcorded because xml_grep does not work in mac.
#printf "setting local-python-path: $path \n "
export PYTHONPATH="${PYTHONPATH}:$path"


## Parsing input parameters
if [ $# -ne 0 ];
then
  scenarioType=$1
  outDir=$2
  configNo=$3
fi

#only hardcorded configuration
batchRunMainConfig="./configs/batch-run-main-config.xml"
scenario="grid"
simMainConfig="$outDir/scenarios/$scenario/dee-main.xml"
matsimMainConfig="$outDir/scenarios/$scenario/matsim-main.xml"
matsimPlansFile="$outDir/scenarios/$scenario/scenario_matsim_plans-dee-traffic-agents-2.xml"
expDiffusionConfig="$outDir/scenarios/$scenario/scenario_diffusion_config.xml"
resultsDir="output"
lhs="../latin-hypercube-samples/integrated-model-settings.xls"

#outDirFormat=$timeStamp-$scenario #FIXME passing timeStamp or the specific configuration setting directory?
#resultsDir=$(getSingleTagValue $batchRunMainConfig "local-results")

#outDir=$resultsDir/$outDirFormat


#set configurations based on case/scenario
: '
if [ $scenarioType == "d" ];
then
#simMainConfig=$(getSingleTagValue $batchRunMainConfig "sn-main") # original main configuration file
#expDiffusionConfig=$(getSingleTagValue $batchRunMainConfig "sn-diffusion") # original diffusion configuration file
#lhs=$(getSingleTagValue $batchRunMainConfig "sn-lhs")

elif [ $scenarioType == "bl" ];
then
#simMainConfig=$(getSingleTagValue $batchRunMainConfig "bl-main") # original diffusion configuration file
elif [[ $scenarioType == "bc" ]]; then
  printf " currently not implemented for broadcast scenario. \n" ;
  exit 1;
else
  printf "scenario option $scenarioType is unknown, aborting!";
  exit 1;
fi
'



#print Configs
printf "configuration files will be created at = $outDir \n"
printf "scenario = $scenario  \n"
printf "scenario type = $scenarioType  \n"
printf "experiments configuration xml file= $batchRunMainConfig  \n"
printf "simulation main configuration file = $simMainConfig \n"
printf "matsim main configuration file = $matsimMainConfig \n"
printf "simulation diffusion configuration file  = $expDiffusionConfig \n"
printf "matsim plans file = $matsimPlansFile \n"
printf "lhs file-name = $lhs \n"



while true; do
    read -p "Do you wish to continue?" yn
    case $yn in
        [Yy]* ) break;;
        [Nn]* ) exit;;
        * ) echo "Please answer yes or no.";;
    esac
done


#1. copy configurations to outDir.
#cp $simMainConfig  $outDir/
ssconvert $lhs $outDir/samples.csv #1. export the excel file to a csv file and remove formatting - ssconvert is a commandline utlity of  Gnumeric application


#cd $outDir



#2. remove unwanted rows - working
sed -e '1,4d' $outDir/samples.csv  > $outDir/modified-samples.csv

# 3. and extract the specific columns
INPUT=$outDir/modified-samples.csv
OLDIFS=$IFS
IFS=,
[ ! -f $INPUT ] && { echo "$INPUT file not found"; exit 99; }


################## scenario SN######################
if [ $scenarioType == "d" ];
then
	sample=0
	while read col1 col2 col3 col4 col5 col6 col7 col8
	do
		let "sample++"

		links=$col2
		prob=$col3
		step=$col4
    farT=$col5
    recencyT=$col6
    angleT=$col7


		#4. modify the parameters in the configuration file
		# find the mathing string in each line and replace the entire lines and write to a new file

    if [ $sample -eq $configNo ];
    then
    # copy the new config file to a new dir
    printf "links: $links | prob: $prob| diffusion step: $step"

    mkdir -p $outDir/sample$sample

    # main config modifications
    #FIXME adding '' after option i as in mac i expects an extension. Effects of this on ubuntu not tested.
    sed -i '' "s#<opt id\=\"jLogFile\">[-_[:alnum:]./]*#<opt id=\"jLogFile\">${resultsDir}/jill.log#"  $simMainConfig  # alnum = Any alphanumeric character, [A-Za-z0-9]
    #jOutFile
    sed -i '' "s#<opt id\=\"jOutFile\">[-_[:alnum:]./]*#<opt id=\"jOutFile\">${resultsDir}/jill.out#"  $simMainConfig
    #matsim output directory
    sed -i '' "s#<opt id\=\"outputDir\">[-_[:alnum:]./]*#<opt id=\"outputDir\">${resultsDir}/matsim#"  $simMainConfig

    # diffusion config modifications
    sed -i '' "s#log_file\=\"[-_[:alnum:]./]*#log_file\=\"diffusion.log#"  $expDiffusionConfig #logfile
    sed -i '' "s#out_file\=\"[-_[:alnum:]./]*#out_file\=\"diffusion.out#"  $expDiffusionConfig #outfile
    sed -i '' 's/avg_links=.*/avg_links="'$links'"/g;
		s|>[0-9,.]*</diffusion_probability>|>'$prob'</diffusion_probability>|g;
	 	s|>[0-9]*</step_size>|>'$step'</step_size>|g'  $expDiffusionConfig #degree, probability and diffusion step


    #matsim agent plan modifications
    


  fi

done < $INPUT
IFS=$OLDIFS

fi



printf "generated $sample number of samples \n"
