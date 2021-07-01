#author chaminda Bulumulla
#read run_config.txt, copy default dee configs, modify, batch-run simulations
#
#

import sys # for arg handling
from datetime import datetime
import subprocess
from lxml import etree as ET
import os
from glob import glob

args = len(sys.argv) - 1
if args != 1:
    print "call the script with following args: run_config.txt"
    exit()

run_config_file = sys.argv[1]
networks = []
blockage_levels = []
diffusion_levels = []

results_dir="../../results-chapter4"
src_dir="../../diffusion-evacuation-experiments"
out_dir="output"
run_d="run"
copy_configs_bash_script="copy_configs.sh"

network_dict = {'rand-reg':"randRegNetwork","random":"randomNetwork","sw":"swNetwork"}

# returns the matching dir path including res_dir
def get_matching_dirs(res_dir,pattern,scenario):
    pattern = res_dir + "/*" + pattern
    # print "searching for patterns ",pattern
    directories = glob(pattern)
    directories.sort(key=os.path.getctime, reverse=True) # uses creation time to sort.
    return directories[0] # reverse order to have most recent one on top

def get_config_for_level(file,config,level):
    with open(file, 'r') as f:
        value = None
        for line in f.readlines():
            line = line.strip()
            text = config + "_" + level
            if text in line:
                value = line.split('=')[1]
        if value is None:
            print " config parameter ",config, "NOT FOUND for level", level
    return value

def modify_disruption_configs_for_bl_cases(run_config,main_config,matsim_main,blevel):

    #read new parameters
    disfile = get_config_for_level(run_config,"disruptions",blevel)
    popfile = get_config_for_level(run_config,"population",blevel)

    print "configs: ","disruptions: ",disfile," matsim population: ",popfile

    # set disruption file
    parser = ET.XMLParser(remove_blank_text=True)
    main_root = ET.parse(main_config, parser).getroot()
    disruption =  main_root.find("xmlns:models/xmlns:model[@id='disruption']/xmlns:opt[@id='fileJson']", namespaces={'xmlns': 'https://github.com/agentsoz/bdi-abm-integration'})
    disruption.text = disfile

    with open(main_config, "w") as f:
        f.write('<?xml version="1.0" encoding="utf-8"?> \n')
        f.write(ET.tostring(main_root, pretty_print=True,encoding="utf-8"))

    #change matsim population file
    parser = ET.XMLParser(remove_blank_text=True)
    matsim_main_root = ET.parse(matsim_main, parser).getroot()
    plans_module =  matsim_main_root.find("module[@name='plans']/param[@name='inputPlansFile']")
    plans_module.attrib["value"] = popfile

    with open(matsim_main, "w") as f:
        f.write('<?xml version="1.0" encoding="utf-8"?> \n <!DOCTYPE config SYSTEM "http://www.matsim.org/files/dtd/config_v2.dtd"> \n')
        f.write(ET.tostring(matsim_main_root, pretty_print=True,encoding="utf-8"))


def modify_diffusion_disruption_configs_for_sn_cases(run_config,main_config,matsim_main,diff_config,net,dlevel,blevel):
    global network_dict
    #read new parameters
    disfile = get_config_for_level(run_config,"disruptions",blevel)
    popfile = get_config_for_level(run_config,"population",blevel)
    prob = get_config_for_level(run_config,"diffusion_probability",dlevel)
    step = get_config_for_level(run_config,"step_size",dlevel)
    degree = get_config_for_level(run_config,"avg_links",dlevel)

    print "configs: ","disruptions: ",disfile," matsim population: ",popfile," diff prob: ",prob," step: ",step," degree: ",degree

    # set disruption file
    parser = ET.XMLParser(remove_blank_text=True)
    main_root = ET.parse(main_config, parser).getroot()
    disruption =  main_root.find("xmlns:models/xmlns:model[@id='disruption']/xmlns:opt[@id='fileJson']", namespaces={'xmlns': 'https://github.com/agentsoz/bdi-abm-integration'})
    disruption.text = disfile

    with open(main_config, "w") as f:
        f.write('<?xml version="1.0" encoding="utf-8"?> \n')
        f.write(ET.tostring(main_root, pretty_print=True,encoding="utf-8"))

    #change matsim population file
    parser = ET.XMLParser(remove_blank_text=True)
    matsim_main_root = ET.parse(matsim_main, parser).getroot()
    plans_module =  matsim_main_root.find("module[@name='plans']/param[@name='inputPlansFile']")
    plans_module.attrib["value"] = popfile

    with open(matsim_main, "w") as f:
        f.write('<?xml version="1.0" encoding="utf-8"?> \n <!DOCTYPE config SYSTEM "http://www.matsim.org/files/dtd/config_v2.dtd"> \n')
        f.write(ET.tostring(matsim_main_root, pretty_print=True,encoding="utf-8"))


    #set diffusion parameters
    parser = ET.XMLParser(remove_blank_text=True)
    dif_root = ET.parse(diff_config, parser).getroot()
    diff_prob =  dif_root.find("ic/diffusion_probability")
    diff_prob.text = prob

    diff_step =  dif_root.find("ic/step_size")
    diff_step.text = step


    net_type = dif_root.find("snModel")
    net_type.attrib["networkType"] = net
    net_ele = network_dict[net] #find the correct network type
    deg =  dif_root.find(str(net_ele))
    deg.attrib["avg_links"] = degree

    with open(diff_config, "w") as f:
        f.write('<?xml version="1.0" encoding="utf-8"?> \n')
        f.write(ET.tostring(dif_root, pretty_print=True,encoding="utf-8"))

def modify_and_run_bl_runs(blevel,scenario,start,end):
    global results_dir,src_dir,run_dir,out_dir,run_config_file,new_dir

    print " blocakge level=",blevel
    if new_dir == "y":
        tt = datetime.today().strftime('%h%d-%H-%M')
        dir = results_dir + "/" + tt + "-"+ blevel + "-blockage"

    elif new_dir == "n": #call new function to get matching dir
        pattern = blevel + "-blockage"
        dir = get_matching_dirs(results_dir,pattern,scenario)
        print " target results directry found:  ",dir


    run=startRun
    while run <= endRun:

        run_dir = dir + "/" + scenario + "-" +run_d + str(run)
        #copy default dee configs and change output dirs accordingly
        rc = subprocess.check_call("sh %s %s %s %s" % (copy_configs_bash_script, scenario, src_dir, run_dir), shell=True)
        #modify diffusion,disruption parameters
        main_config = run_dir + "/scenarios/hawkesbury/social_network_experiments_diffusion.xml"
        matsim_main = run_dir + "/scenarios/hawkesbury/matsim_main.xml"
        modify_disruption_configs_for_bl_cases(run_config_file,main_config,matsim_main,blevel)
        #now run script
        command1 = "cd " + run_dir + " && " + " sh run.sh &> stdout.out" + " && cd -  > /dev/null"
        os.system(command1)

        command2 = "cd " + run_dir + " && " + " sh extract_basic_stats.sh  > extract.out" + " && cd -  > /dev/null"
        os.system(command2)

        run = run + 1

def modify_and_run_sn_runs(net,blevel,dlevel,scenario,start,end):

    global results_dir,src_dir,run_dir,out_dir,run_config_file,new_dir

    print " network=",net," blocakge level=",blevel," diffusion level=",dlevel
    if new_dir == "y":
        tt = datetime.today().strftime('%h%d-%H') #e.g. Jun04-10
        dir = results_dir + "/" + tt + "-net-"+ net + "-"+ blevel + "-blockage-" + dlevel +"-diff"

    elif new_dir == "n": #call new function to get matching dir
        pattern = "-net-"+ net + "-"+ blevel + "-blockage-" + dlevel +"-diff"
        dir = get_matching_dirs(results_dir,pattern,scenario)
        print " target results directry found:  ",dir

    run=startRun
    while run <= endRun:

        run_dir = dir + "/" + scenario + "-" +run_d + str(run)
        #copy default dee configs and change output dirs accordingly
        rc = subprocess.check_call("sh %s %s %s %s" % (copy_configs_bash_script, scenario, src_dir, run_dir), shell=True)
        #modify diffusion,disruption parameters
        main_config = run_dir + "/scenarios/hawkesbury/social_network_experiments_diffusion.xml"
        matsim_main = run_dir + "/scenarios/hawkesbury/matsim_main.xml"
        dif_config = run_dir +  "/scenarios/hawkesbury/scenario_diffusion_config.xml"
        modify_diffusion_disruption_configs_for_sn_cases(run_config_file,main_config,matsim_main,dif_config,net,dlevel,blevel)

        #now run script
        command = "cd " + run_dir + " && " + " sh run.sh &> stdout.out" + " && cd -"
        os.system(command)

        command2 = "cd " + run_dir + " && " + " sh extract_basic_stats.sh  > extract.out" + " && cd -"
        os.system(command2)

        run = run + 1

with open(run_config_file, 'r') as f:
    for line in f.readlines():
        line = line.strip()
        if "scenario=" in line:
            scenario = line.split('=')[1]
        if "new_dir" in line:
            new_dir = line.split('=')[1]
        elif "startRun" in line:
            startRun = int(line.split('=')[1])
        elif "endRun" in line:
            endRun = int(line.split('=')[1])
        elif 'networks' in line:
            networks = line.split('=')[1].split(',')
        elif 'blockage_levels' in line:
            blockage_levels = line.split('=')[1].split(',')
        elif 'diffusion_levels' in line:
            diffusion_levels = line.split('=')[1].split(',')
print "read run configurations.."
print "scenario :",scenario
print "new_dir :",new_dir
print "startRun :",startRun
print "endRun :",endRun
print "network set:", networks
print "blockage levels:",blockage_levels
print "diffusion levels:", diffusion_levels

# print "proceed running simulations?"
# confirm = str(raw_input())
# if confirm != "y":
#     exit()

print "continuing.."


if scenario == "sn":
    for net in networks:
        for b_level in blockage_levels:
            for diff_level in diffusion_levels:
                modify_and_run_sn_runs(net,b_level,diff_level,scenario,startRun,endRun)

if scenario == "bl":
    for b_level in blockage_levels:
        modify_and_run_bl_runs(b_level,scenario,startRun,endRun)
