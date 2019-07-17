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




if [ $# -ne 8 ]; then
 	printf " This script creates configurations for batch run. Enter following args: \n";
	printf " 1. scenarioType = d/bl(diffusion/baseline) 2. scenarioName 3.main config name 4. matsim config name 5.matsim plans file name 6. diffusion config name 7.  specific run  directory 8. config number  \n ";
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
  scenario=$2
  simMainConfigName=$3
  matsimMainConfigName=$4
  matsimPlansFileName=$5
  expDiffusionConfigName=$6
  outDir=$7
  configNo=$8
fi

#only hardcorded configuration
#batchRunMainConfig="./configs/batch-run-main-config.xml"
simMainConfig="$outDir/scenarios/$scenario/$simMainConfigName"
matsimMainConfig="$outDir/scenarios/$scenario/$matsimMainConfigName"
matsimPlansFile="$outDir/scenarios/$scenario/$matsimPlansFileName"
expDiffusionConfig="$outDir/scenarios/$scenario/$expDiffusionConfigName"
resultsDir="output"
lhs="../latin-hypercube-samples/integrated-model-settings.xls"


#print Configs
printf "configuration files will be created at = $outDir \n"
printf "scenario = $scenario  \n"
printf "scenario type = $scenarioType  \n"
#printf "experiments configuration xml file= $batchRunMainConfig  \n"
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
    distanceT=$col5
    recencyT=$col6
    angleT=$col7


		#4. modify the parameters in the configuration file
		# find the mathing string in each line and replace the entire lines and write to a new file

    if [ $sample -eq $configNo ];
    then
    # copy the new config file to a new dir
    printf "extracted settings row $sample: links: $links | prob: $prob| diffusion step: $step | distanceThreshold: $distanceT | recencyThreshold: $recencyT | angleT: $angleT \n"


    #mkdir -p $outDir/sample$sample

    #config modifications for linux
    if [[ "$OSTYPE" == "linux-gnu" ]]; then

      printf "Operating system: linux-gnu \n"
      # main config modifications
      sed -i "s#<opt id\=\"jLogFile\">[-_[:alnum:]./]*#<opt id=\"jLogFile\">${resultsDir}/jill.log#"  $simMainConfig  # alnum = Any alphanumeric character, [A-Za-z0-9]

      #jOutFile
      sed -i "s#<opt id\=\"jOutFile\">[-_[:alnum:]./]*#<opt id=\"jOutFile\">${resultsDir}/jill.out#"  $simMainConfig
      #matsim output directory
      sed -i "s#<opt id\=\"outputDir\">[-_[:alnum:]./]*#<opt id=\"outputDir\">${resultsDir}/matsim#"  $simMainConfig

      # diffusion config modifications
      sed -i "s#log_file\=\"[-_[:alnum:]./]*#log_file\=\"${resultsDir}/diffusion.log#"  $expDiffusionConfig #logfile
      sed -i "s#out_file\=\"[-_[:alnum:]./]*#out_file\=\"${resultsDir}/diffusion.out#"  $expDiffusionConfig #outfile
      sed -i 's/avg_links=.*/avg_links="'$links'"/g;
  		s|>[0-9,.]*</diffusion_probability>|>'$prob'</diffusion_probability>|g;
  	 	s|>[0-9]*</step_size>|>'$step'</step_size>|g'  $expDiffusionConfig #degree, probability and diffusion step

      #matsim agent plan modifications. --inplace option to allow modification in matsim file rather than sending output to standard output

      extension="${matsimPlansFileName##*.}"
      if [[ "$extension" == "gz"* ]]; then
        printf " MATSim plans file is in gz format as expected. \n"

        gunzip $matsimPlansFile # first uncompress
        xmlFile="${matsimPlansFileName%.*}" # remove .gz extension (.xml is already there)
        #xmlFile="$baseNameWithoutExtension.xml"
        printf " generated MATSim plans xml file name is $xmlFile \n"

        xmlFilePath=$outDir/scenarios/$scenario/$xmlFile # add the file path to filename
        xmlstarlet edit --inplace --update "/population/*/attributes/attribute[@name='distanceFromTheBlockageThreshold']" --value "$distanceT" $xmlFilePath
        xmlstarlet edit --inplace --update "/population/*/attributes/attribute[@name='blockageRecencyThreshold']" --value "$recencyT" $xmlFilePath
        xmlstarlet edit --inplace --update "/population/*/attributes/attribute[@name='blockageAngleThreshold']" --value "$angleT" $xmlFilePath

        #done modifying, now compress it again
        gzip $xmlFilePath

    elif [[ "$extension" == "xml"* ]]; then

      printf " MATSim plans file  is xml format, this is discouraged as diff may not be computed properly. \n"
      xmlstarlet edit --inplace --update "/population/*/attributes/attribute[@name='distanceFromTheBlockageThreshold']" --value "$distanceT" $matsimPlansFile
      xmlstarlet edit --inplace --update "/population/*/attributes/attribute[@name='blockageRecencyThreshold']" --value "$recencyT" $matsimPlansFile
      xmlstarlet edit --inplace --update "/population/*/attributes/attribute[@name='blockageAngleThreshold']" --value "$angleT" $matsimPlansFile

    else
        printf " ERROR: MATSim plans file is in an unknown extension: $extension \n"
    fi




    # config modifications for MAC
    elif [[ "$OSTYPE" == "darwin"* ]]; then

      printf "Operating system: Mac OS \n"

      # main config modifications
      #adding '' after option i as in mac i expects an extension.
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


      # NOT TESTED matsim agent plan modifications. --inplace option to allow modification in matsim file rather than sending output to standard output
      xmlstarlet edit --inplace --update "/population/*/attributes/attribute[@name='distanceFromTheBlockageThreshold']" --value "$distanceT" $matsimPlansFile
      xmlstarlet edit --inplace --update "/population/*/attributes/attribute[@name='blockageRecencyThreshold']" --value "$recencyT" $matsimPlansFile
      xmlstarlet edit --inplace --update "/population/*/attributes/attribute[@name='blockageAngleThreshold']" --value "$angleT" $matsimPlansFile

    else

        printf "unknown OS, aborting"
        exit 1
    fi


  fi

done < $INPUT
IFS=$OLDIFS

fi



printf "************modified configurations for config $sample *********\n\n"
