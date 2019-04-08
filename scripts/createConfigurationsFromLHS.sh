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




if [ $# -ne 2 ]; then
 	printf " This script creates configurations for batch run. Enter following args: \n";
	printf " 1. scenario = sn/bl(baseline)/bc(Broadcast)  2. regex (fem2) / sub scenario directory name (fem1)\n ";
  >&2
  exit 1
 fi

#--------------------starting script--------------------------------------------
printf "\n\n createConfigurationsFromLHS.sh script started.... \n"

#only hardcorded configuration
batchRunMainConfig="./configs/batch-run-main-config.xml"

# set the python di using bash xml_grep : python functions doesnt work until the path is set
path=`xml_grep "local-python-path" $batchRunMainConfig --text_only`
#printf "setting local-python-path: $path \n "
export PYTHONPATH="${PYTHONPATH}:$path"


## Parsing input parameters
if [ $# -ne 0 ];
then
  scenario=$1
  timeStamp=$2  #FIXME passing timeStamp or the specific configuration setting directory?
fi

outDirFormat=$timeStamp-$scenario #FIXME passing timeStamp or the specific configuration setting directory?
resultsDir=$(getSingleTagValue $batchRunMainConfig "local-results")
outDir=$resultsDir/$outDirFormat


#set configurations based on case/scenario

if [ $scenario == "sn" ];
then
expMainConfig=$(getSingleTagValue $batchRunMainConfig "sn-main") # original main configuration file
expDiffusionConfig=$(getSingleTagValue $batchRunMainConfig "sn-diffusion") # original diffusion configuration file
LatinHyperSamplesDir=$(getSingleTagValue $batchRunMainConfig "latinHypercube-dir") # LHS dir.
lhs=$(getSingleTagValue $batchRunMainConfig "sn-lhs")
fi

if [ $scenario == "bl" ];
then
expMainConfig=$(getSingleTagValue $batchRunMainConfig "bl-main") # original diffusion configuration file
fi


#print Configs
printf " cofiguration files and directory paths (relatives directories from script dir) : \n"
printf "batch run main configuration file= $batchRunMainConfig  \n"
printf "scenario/case = $scenario  \n"
printf "original main configuration file (from LHS dir) = $expMainConfig \n"
printf "original diffusion configuration file (from LHS dir) = $expDiffusionConfig \n"
printf "Latin Hypercube dir = $LatinHyperSamplesDir \n"
printf "lhs file-name = $lhs \n"
printf "configuration files will be created at = $outDir \n"


while true; do
    read -p "Do you wish to continue?" yn
    case $yn in
        [Yy]* ) make install; break;;
        [Nn]* ) exit;;
        * ) echo "Please answer yes or no.";;
    esac
done

#changing directory !!! do not read any configs after here
cd $LatinHyperSamplesDir

#0. copy hawkesbury.xml file - working
cp $expMainConfig .

#1. export the excel file to a csv file and remove formatting - ssconvert is a commandline utlity of  Gnumeric application
ssconvert $lhs samples.csv

#2. remove unwanted rows - working
sed -e '1,4d' samples.csv  > modified-samples.csv

# 3. and extract the specific columns
INPUT=modified-samples.csv
OLDIFS=$IFS
IFS=,
[ ! -f $INPUT ] && { echo "$INPUT file not found"; exit 99; }

################## scenario2 ######################
if [ $scenario -eq "2" ];
then
	sample=0
	while read col1 col2 col3 col4 col5 col6 col7 col8
	do
		let "sample++"

		dependants=$col2 #&& echo $dependants
		pickupT=$col3
		SclDistance=$col4 #&& echo $SclDistance
		relsDistance=`expr $col4 \* 1000` #&& echo $relsDistance

		#4. modify the parameters in the configuration file
		# find the mathing string in each line and replace the entire lines and write to a new file

                #set the scenario in addition to the lhs parameters


		sed -e 's/.*\skids=.*/\tkids="'$dependants'"/g;
		s/.*\srelatives=.*/\trelatives="'$dependants'"/g;
                s/.*\sscenarioType=.*/\tscenarioType="'$scenario'"/g;
	 	s/.*\max_pickuptime_for_kids_and_rels=.*/\tmax_pickuptime_for_kids_and_rels="'$pickupT'"/g;
		s/.*max_distance_to_relatives=.*/\tmax_distance_to_relatives="'$relsDistance'"/g;
		s/.*max_distance_to_school=.*/\tmax_distance_to_school="'$SclDistance'"/g'  $expMainConfig > $sample.xml

		#5. copy the new config file to a new dir
		mkdir -p $outConfigDir/s$scenario/$timeStamp/sample$sample
	  	mv $sample.xml $outConfigDir/s$scenario/$timeStamp/sample$sample/hawkesbury.xml


done < $INPUT
IFS=$OLDIFS

fi

################## scenario3 ######################
if [ $scenario -eq "3" ];
then
	sample=0
	while read col1 col2 col3 col4 col5 col6 col7 col8
	do
		let "sample++"

		avgLinks=$col2 #&& echo $dependants
		turn=$col3 #&& echo $turn
		seed=$col4 #&& echo $SclDistance
		low=$col5 #&& echo $vol
		#type=$col6 #&& echo $type


		#set the scenario and network type in addition to the lhs parameters


		#4. modify the parameters in the configuration file
		# find the mathing string in each line and replace the entire lines and write to a new file

		sed -e 's/.*\savg_links=.*/\tavg_links="'$avgLinks'"/g;
		s/.*\sdiff_turn=.*/\tdiff_turn="'$turn'"/g;
	 	s/.*\diff_seed=.*/\tdiff_seed="'$seed'"/g;
		s/.*mean_act_threshold=.*/\tmean_act_threshold="'$low'"/g;
                s/.*\sscenarioType=.*/\tscenarioType="'$scenario'"/g' $expMainConfig > $sample.xml
		#s/.*networkType=.*/\tnetworkType="'$network'"/g'


		#5. copy the new config file to a new dir
		mkdir -p $outConfigDir/s$scenario/$timeStamp/sample$sample
	  	mv $sample.xml $outConfigDir/s$scenario/$timeStamp/sample$sample/hawkesbury.xml


done < $INPUT
IFS=$OLDIFS

fi


printf "generated $sample number of samples \n"


# TESTED configurations:
#	config path
#	scenario2:
#		lhs parameters checked in sample1 and sample17
#
#	scenario3 :
#		scenarioType tested
#		lhs config 1 and 17 checked values of all parameters
#		random and sw network types checked
#
#		changed  sw network lhs file paramters and checked
#		chagned random network paramter(diffusion turn) and then cheked
# ran with run-sim main script, one sample and checked the configs with the lhs excel sheet.
