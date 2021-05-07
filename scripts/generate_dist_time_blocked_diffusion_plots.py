## run both to analyse both sn and baseline data, and have the comparison plots in sn. Or you can analyse a single scenario
##
import sys # for arg handling
import pandas as pd
import matplotlib.pyplot as plt
import optparse
import os


parser = optparse.OptionParser()

parser.add_option('-s', "--scenario",action="store", dest="scenario", help="option for one scenario sn/bl/both")
parser.add_option('-d', "--directory",action="store", dest="directory", help="input directory")

options, remainder = parser.parse_args()

scenario = options.scenario
tt_dir = options.directory

if tt_dir is not None and not os.path.isdir(tt_dir):
    print "input directory does not exist, exiting"
    exit()

input_dir = "none"
if scenario == "bl":
    input_dir = tt_dir + "/bl-run1" #FIXME add average data dir
elif scenario == "sn":
    input_dir = tt_dir + "/sn-run1" #FIXME add average data dir

output_dir_scenario = input_dir + "/plots-" + scenario
output_dir_comparison = tt_dir + "/plots"

matsim_journeys_file =  "output/matsim/output_matsim_journeys.txt"
blocked_percepts_file = "blocked_times.out"
diffusion_file =  "output/diffusion.out"



#global data structures
journeys_df_bl = pd.DataFrame()
journeys_df_sn = pd.DataFrame()
blocked_percepts_df_bl = pd.DataFrame()
blocked_percepts_df_sn = pd.DataFrame()
diffusion_df = pd.DataFrame()



def readFile(dir,filename,delim,headerVal):
    file = dir + "/" + filename
    if os.path.isfile(file):
        print "\n reading file..", file
        if headerVal is None: #header false
            df = pd.read_csv(file, sep=delim, header=headerVal)
        else: # header true
            df = pd.read_csv(file, sep=delim)
        return df
    else:
        print "file ",file, " not found!"

def plot_distance_distribution(jounrneys_df, scenario,figure_index,output_dir):
    jounrneys_df['distance'] = jounrneys_df['distance'].div(1000).round(2) # covert meters into km
    print "Descriptive statistics of distance distribution:",scenario
    print jounrneys_df['distance'].describe()

    plt.figure(figure_index) # now plot the histrogram of 1D distance distribution
    if scenario == "bl":
        sce_label = "baseline"
    else:
        sce_label = "social"

    plt.hist(jounrneys_df['distance'],alpha=0.5,label=sce_label)
    plt.title('Travel distance histogram')
    plt.xlabel('Distance (km)')

    print "saving travel plot as distance_distribution.png"
    plt.savefig(output_dir + "/" + 'distance_distribution.png')
    return jounrneys_df

def plot_time_distribution(df, scenario,figure_index,output_dir):
    df['in_vehicle_time'] = df['in_vehicle_time'].div(60).round(2) # covert seconds into mins
    print "Descriptive statistics of travel time distribution:",scenario
    print df['in_vehicle_time'].describe()

    plt.figure(figure_index) # now plot the histrogram of 1D distance distribution
    plt.hist(df['in_vehicle_time'], alpha=0.5)
    plt.title('Travel time histogram')
    plt.xlabel('Travel time (minutes)')

    print "saving travel plot as travel_distribution.png"
    plt.savefig(output_dir + "/" + 'time_distribution.png')
    return df

def plot_blocked_percepts_timing_graph(per_df, scenario,figure_index,output_dir):
    per_df.columns = ['time','id']
    per_df = per_df.astype({"time": int, "id": int})
    per_df = per_df.sort_values(by=['time']) # sort by time to get the cumulative counts over time
    per_df['time'] = per_df['time'].div(60).round(2) # converted secs into minutes
    count = range(1,len(per_df)+1,1)
    #print count
    per_df.insert (2, "count", count)
    plt.figure(figure_index) # to differentiate figures
    #ok to plot lines connecting dots
    plt.plot(per_df['time'], per_df['count'],'-ok', markersize=0.3, linewidth=0.8,alpha=0.5, label=scenario)
    plt.title('Total number of agents that encounter the blockage over time')
    plt.ylabel('Agent count')
    plt.xlabel('Simulation time (minutes)')
    #plt.show()
    print "saving as blocked_percept_times.png"
    plt.savefig(output_dir + "/" + 'blocked_percept_times.png')
    return per_df

def plot_diffusion_graph(dif_df, scenario,figure_index,output_dir):
    content =  dif_df.columns[1] # get content name

    plt.figure(figure_index) # to differentiate figures
    #ok to plot lines connecting dots
    plt.plot(dif_df['time'], dif_df[content],'-ok', markersize=4, linewidth=0.75,color="blue")
    plt.title('Diffusion graph')
    plt.ylabel('Agent count')
    plt.xlabel('Simulation time (minutes)')
    #plt.show()
    print "saving as diffusion_graph.png"
    plt.savefig(output_dir + "/" + 'diffusion_graph.png')
    return dif_df

def gen_scenario_plots(figure_index):
    # global journeys_df_bl
    # global blocked_percepts_df_bl

    print " scenario: ",scenario, " | input dir: ",input_dir, " | output dir (scenario): ",output_dir_scenario , " | output dir (comparison ): ",output_dir_comparison
    if not os.path.isdir(output_dir_scenario):
        os.makedirs(output_dir_scenario)

    #Baseline - distance distribution
    print "------------- Scenario ", scenario, "ANALYSIS 1: travel distance distribution--------------"
    journeys_df = readFile(input_dir,matsim_journeys_file,"\t",True)
    if journeys_df is not None:
        print "journeys_df size ",len(journeys_df)
        journeys_df = plot_distance_distribution(journeys_df,scenario,figure_index,output_dir_scenario)

        #Baseline - distance distribution
        print "-------------ANALYSIS 2: travel time distribution--------------"
        journeys_df = plot_time_distribution(journeys_df,scenario,figure_index+1,output_dir_scenario)

    #Baseline - timing of blocked percepts
    print "------------- Scenario ", scenario, "-------------ANALYSIS 3: timing of blocked percepts --------------"
    blocked_percepts_df = readFile(input_dir,blocked_percepts_file,' ',None)
    if blocked_percepts_df is not None:
        print "blocked_percepts_df size ", len(blocked_percepts_df)
        blocked_percepts_df = plot_blocked_percepts_timing_graph(blocked_percepts_df,scenario,figure_index+2,output_dir_scenario)

    #social scenario  - diffusion graph
    print "------------- Scenario ", scenario, "-------------ANALYSIS 4: Diffusion graph --------------"
    diff_df = readFile(input_dir,diffusion_file,'\t',True)
    if diff_df is not None:
        print "diffusion_df size ", len(diff_df)
        diff_df = plot_diffusion_graph(diff_df,scenario,figure_index+3,output_dir_scenario)

    return journeys_df, blocked_percepts_df, diff_df

def gen_comparison_blocked_percept_plots(df_bl,df_sn,out_dir, figure_index):
    print "--------------------------COMPARISON 1: Timing of Blocked percepts --------------"
    plt.figure(figure_index) # to differentiate figures
    #ok to plot lines connecting dots
    plt.plot(df_bl['time'], df_bl['count'],'-ok', markersize=0.3, linewidth=1.5, color='red', label="baseline")
    plt.plot(df_sn['time'], df_sn['count'],'-ok', markersize=0.3, linewidth=1.5, color='blue', label="social")
    plt.legend()
    plt.title('Total number of agents that encounter the blockage over time')
    plt.ylabel('Agent count')
    plt.xlabel('Simulation time (minutes)')
    #plt.show()
    print "saving as blocked_percept_times_comparison.png"
    plt.savefig(out_dir + "/" + 'blocked_percept_times_comparison.png')

def gen_comparison_distance_plots(df_bl,df_sn,out_dir, figure_index):
    print "--------------------------COMPARISON 1: Timing of distance distributions --------------"
    plt.figure(figure_index) # to differentiate figures
    plt.hist(df_bl['distance'], alpha=0.3, color="red", label="baseline")
    plt.hist(df_sn['distance'], alpha=0.3, color="blue", label="social")
    plt.legend()
    plt.title('Travel distance comparison')
    plt.xlabel('Travel time (minutes)')

    print "saving travel plot as travel_distribution_comparison.png"
    plt.savefig(out_dir + "/" + 'distance_distribution_comparison.png')

def gen_comparison_travel_plots(df_bl,df_sn,out_dir, figure_index):
    print "--------------------------COMPARISON 1: Timing of travel distributions --------------"
    plt.figure(figure_index) # to differentiate figures
    plt.hist(df_bl['in_vehicle_time'], alpha=0.3, color="red", label="baseline")
    plt.hist(df_sn['in_vehicle_time'], alpha=0.3, color="blue", label="social")
    plt.legend()
    plt.title('Travel time comparison')
    plt.xlabel('Travel time (minutes)')

    print "saving travel plot as travel_distribution_comparison.png"
    plt.savefig(out_dir + "/" + 'time_distribution_comparison.png')



#---------- run analyse functions-------------
if scenario == "both":
    input_dir = tt_dir + "/bl-run1" # set input directory
    scenario = "bl"
    output_dir_scenario = input_dir + "/plots-" + scenario

    res = gen_scenario_plots(1)
    journeys_df_bl = res[0]
    blocked_percepts_df_bl  = res[1]
    print "journeys_df_bl size ",len(journeys_df_bl)
    print "blocked_percepts_df_bl size ", len(blocked_percepts_df_bl)

    input_dir = tt_dir + "/sn-run1"
    scenario = "sn"
    output_dir_scenario = input_dir + "/plots-" + scenario

    res = gen_scenario_plots(4)
    journeys_df_sn = res[0]
    blocked_percepts_df_sn  = res[1]
    diffusion_df  = res[2]
    print "journeys_df_sn size ", len(journeys_df_sn)
    print "blocked_percepts_df_sn size ",len(blocked_percepts_df_sn)
    print "diffusion_df size ",len(diffusion_df)

    #comparision
    # print blocked_percepts_df_sn
    # print journeys_df_sn
    if not os.path.isdir(output_dir_comparison):
        os.makedirs(output_dir_comparison)
    gen_comparison_blocked_percept_plots(blocked_percepts_df_bl,blocked_percepts_df_sn,output_dir_comparison, 10)
    gen_comparison_distance_plots(journeys_df_bl,journeys_df_sn,output_dir_comparison,11)
    gen_comparison_travel_plots(journeys_df_bl,journeys_df_sn,output_dir_comparison,12)

elif scenario == "bl":
    res = gen_scenario_plots(1)
    journeys_df_bl = res[0]
    blocked_percepts_df_bl  = res[1]
    print "blocked_percepts_df size ", len(blocked_percepts_df_bl)
    print "journeys_df_bl size ",len(journeys_df_bl)
elif scenario == "sn":
    res = gen_scenario_plots(1)
    journeys_df_sn = res[0]
    blocked_percepts_df_sn  = res[1]
    diffusion_df  = res[2]
    print "blocked_percepts_df size ", len(journeys_df_sn)
    print "journeys_df size ",len(journeys_df_sn)
    print "diffusion_df size ",len(diffusion_df)
