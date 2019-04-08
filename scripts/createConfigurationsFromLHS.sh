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
	printf " 1. scenario = sn/bl(baseline)/bc(Broadcast)  2. timeStamp/ output directory \n ";
  >&2
  exit 1
 fi

#--------------------starting script--------------------------------------------
printf "\n\n createConfigurationsFromLHS.sh script started.... \n"

#only hardcorded configuration
batchRunMainConfig="./configs/batch-run-main-config.xml"

# set the python di using bash xml_grep : python functions doesnt work until the path is set
#path=`xml_grep "local-python-path" $batchRunMainConfig --text_only`

path=python #FIXME hardcorded because xml_grep does not work in mac.
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
lhs=$(getSingleTagValue $batchRunMainConfig "sn-lhs")

elif [ $scenario == "bl" ];
then
expMainConfig=$(getSingleTagValue $batchRunMainConfig "bl-main") # original diffusion configuration file
elif [[ $scenario == "bc" ]]; then
  printf " currently not implemented for broadcast scenario. \n" ;
  exit 1;
else
  printf "scenario option $scenario is unknown, aborting!";
  exit 1;
fi


#print Configs
printf " cofiguration files and directory paths (relatives directories from script dir) : \n"
printf "batch run main configuration file= $batchRunMainConfig  \n"
printf "scenario/case = $scenario  \n"
printf "original main configuration file = $expMainConfig \n"
printf "original diffusion configuration file  = $expDiffusionConfig \n"
printf "original diffusion configuration file name = $expDiffusionConfig \n"
printf "lhs file-name = $lhs \n"
printf "configuration files will be created at = $outDir \n"


while true; do
    read -p "Do you wish to continue?" yn
    case $yn in
        [Yy]* ) break;;
        [Nn]* ) exit;;
        * ) echo "Please answer yes or no.";;
    esac
done


#1. copy configurations to outDir.
#cp $expMainConfig  $outDir/
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
if [ $scenario == "sn" ];
then
	sample=0
	while read col1 col2 col3 col4 col5 col6 col7 col8
	do
		let "sample++"

		links=$col2 #&& echo $dependants
		prob=$col3
		step=$col4 #&& echo $SclDistance


		#4. modify the parameters in the configuration file
		# find the mathing string in each line and replace the entire lines and write to a new file


              # copy the new config file to a new dir
              mkdir -p $outDir/sample$sample

		sed -e 's/avg_links=.*/avg_links="'$links'"/g;
		s|>[0-9,.]*</diffusion_probability>|>'$prob'</diffusion_probability>|g; #change delimiter
	 	s|>[0-9]*</step_size>|>'$step'</step_size>|g'  $expDiffusionConfig > $outDir/sample$sample/$(basename $expDiffusionConfig)

# copy in main configuration
cp $expMainConfig $outDir/sample$sample/

#record the diff
diff $expDiffusionConfig $outDir/sample$sample/$(basename $expDiffusionConfig) >> $outDir/diffusion_config_diffs.txt

done < $INPUT
IFS=$OLDIFS

fi



printf "generated $sample number of samples \n"
