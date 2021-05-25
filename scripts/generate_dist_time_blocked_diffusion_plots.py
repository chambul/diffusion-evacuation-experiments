## run both to analyse both sn and baseline data, and have the comparison plots in sn. Or you can analyse a single scenario
##
import sys # for arg handling
import pandas as pd
import matplotlib.pyplot as plt
import optparse
import os
import numpy as np

args = len(sys.argv) - 1
if args == 0:
    print("call the script with following options: -s: sn/bl/both ; -d: e.g. ../../results-chapter4/May05")
    exit()

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


#input
matsim_journeys_file =  "output/matsim/output_matsim_journeys.txt"
blocked_percepts_file = "blocked_times.out"
diffusion_file =  "output/diffusion.out"
info_received_times_file = "info_received_times.out"
info_received_distances_file="./info_received_distance_from_blockage.out"

#global data structures
#baseline
journeys_df_bl = pd.DataFrame()
blocked_percepts_df_bl = pd.DataFrame()

# diffusion
journeys_df_sn = pd.DataFrame()
blocked_percepts_df_sn = pd.DataFrame()
diffusion_df = pd.DataFrame()

#comparison
blockage_aware_times_df = pd.DataFrame() # times of agents that hear about the blockage through SN and also hitting
blockage_aware_distances_df = pd.DataFrame() # distances when agents receive blockage info or when hit the blockage

def check_and_remove_nans(df):
    print "check for nans..."
    is_NaN = df.isnull()
    row_has_NaN = is_NaN.any(axis=1)
    rows_with_NaN = df[row_has_NaN]
    print(rows_with_NaN)
    print "removing nans if any..."
    df = df.dropna()
    print " removed nans..."
    return df

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
    p =  "Descriptive statistics of distance distribution:",scenario
    stats = jounrneys_df['distance'].describe()
    print p
    print str(stats)
    outf = output_dir + "/" + 'distribution_stats.txt'
    if os.path.exists(outf):
        os.remove(outf) #remove previous distro stats
    dist_file = open(outf, "a")
    dist_file.write(str(p))
    dist_file.write("\n")
    dist_file.write(str(stats))
    dist_file.close()

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
    p = "Descriptive statistics of travel time distribution:",scenario
    print p
    stats = df['in_vehicle_time'].describe()
    print str(stats)
    outf = output_dir + "/" + 'distribution_stats.txt'
    dist_file = open(outf, "a")
    dist_file.write("\n")
    dist_file.write(str(p))
    dist_file.write("\n")
    dist_file.write(str(stats))
    dist_file.close()

    plt.figure(figure_index) # now plot the histrogram of 1D distance distribution
    plt.hist(df['in_vehicle_time'], alpha=0.5)
    plt.title('Travel time histogram')
    plt.xlabel('Travel time (minutes)')

    print "saving travel plot as travel_distribution.png"
    plt.savefig(output_dir + "/" + 'time_distribution.png')
    return df

def plot_blocked_percepts_timing_graph(per_df, scenario,figure_index,output_dir):
    per_df.columns = ['time','id']
    per_df = per_df.astype({"time": float, "id": int})
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
        # FIXME make this first, 'distribution_stats.txt' delete if exists

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

    #social scenario  - blockage aware times
    print "------------- Scenario ", scenario, ": creating blockage aware times (encounter and receiving info) dataframe --------------"
    info_received_times_df = readFile(input_dir,info_received_times_file,' ',None)
    if info_received_times_df is not None:
        info_received_times_df.columns = ['time','id']
        info_received_times_df = info_received_times_df.astype({"time": float, "id": int})
        info_received_times_df['time'] = info_received_times_df['time'].div(60).round(2) # converted secs into minutes
        print "blockage_aware_times_df size (before adding blocked agents) ", len(info_received_times_df)

        for index, row in blocked_percepts_df.iterrows(): # add agents that got blocked
            blocked_id = int(row['id'])
            id_set = set(info_received_times_df['id'])
            # print id_set
            if blocked_id not in id_set: # not received blocage info, but hit the blockage, maybe earlier than bl
                new_row = {'time': int(row['time']), 'id': blocked_id}
                info_received_times_df = info_received_times_df.append(new_row,ignore_index=True)
            else:  # received info and hit blockage, add the earliest one
                time_blocked = float(row['time'])
                sn_row = info_received_times_df.loc[(info_received_times_df['id'] == blocked_id)]
                time_sn = float(sn_row['time'].iloc[0]) # get the first row value
                if time_sn > time_blocked: # agent hits the blockage before receive info
                    info_received_times_df = info_received_times_df.drop(info_received_times_df[info_received_times_df['id'] == blocked_id].index)
                    new_row = {'time': time_blocked, 'id': blocked_id}
                    info_received_times_df = info_received_times_df.append(new_row,ignore_index=True)
        info_received_times_df = info_received_times_df.astype({"time": float, "id": int})

    #social scenario  - blockage aware distances
    print "------------- Scenario ", scenario, ": creating blockage aware distances (receiving info) dataframe --------------"
    df = readFile(input_dir,info_received_distances_file,' ',None)
    if df is not None:
        df.columns = ['id','distances']
        df = df.astype({"id": int,"distances": float})
        df['distances'] = df['distances'].div(1000).round(2) # converted meters into km
        # print df
        for index, row in blocked_percepts_df.iterrows(): # add agents that got blocked, a distance difference of 0
            blocked_id = int(row['id'])
            id_set = set(df['id'])
            if blocked_id not in id_set: # not received blocage info, but hit the blockage, so distance difference is 0. if id in set dont add
                new_row = { 'id': blocked_id, 'distances':0,}
                df = df.append(new_row,ignore_index=True)

        # print df


    return journeys_df, blocked_percepts_df, diff_df,info_received_times_df,df

def gen_comparison_blocked_percept_plots(df_bl,df_sn,out_dir, figure_index):
    print "--------------------------COMPARISON : Timing of Blocked percepts --------------"
    plt.figure(figure_index) # to differentiate figures
    #ok to plot lines connecting dots
    plt.plot(df_bl['time'], df_bl['count'],'-ok', markersize=0.3, linewidth=1.5, color='red', label="baseline")
    plt.plot(df_sn['time'], df_sn['count'],'-ok', markersize=0.3, linewidth=1.5, color='blue', label="social")
    #plot legend out of plot
    plt.legend(bbox_to_anchor=(1,0), loc="lower right") # upper/lower left/right
    # plt.legend()
    plt.title('Total number of agents that encounter the blockage over time')
    plt.ylabel('Agent count')
    plt.xlabel('Simulation time (minutes)')
    #plt.show()
    print "saving as blocked_percept_times_comparison.png"
    plt.savefig(out_dir + "/" + 'blocked_percept_times_comparison.png')

def gen_comparison_distance_plots(df_bl,df_sn,out_dir, figure_index):
    print "--------------------------COMPARISON : distance distributions --------------"
    plt.figure(figure_index) # to differentiate figures
    plt.hist(df_bl['distance'], alpha=0.3, color="red", label="baseline")
    plt.hist(df_sn['distance'], alpha=0.3, color="blue", label="social")
    plt.legend(bbox_to_anchor=(0.5,1), loc="upper center")
    plt.title('Travel distance comparison')
    plt.xlabel('Travel distance (km)')
    plt.ylabel('Agent count')

    print "saving travel plot as travel_distribution_comparison.png"
    plt.savefig(out_dir + "/" + 'distance_distribution_comparison.png')

def gen_comparison_travel_plots(df_bl,df_sn,out_dir, figure_index):
    print "--------------------------COMPARISON : Travel time distributions --------------"
    plt.figure(figure_index) # to differentiate figures
    plt.hist(df_bl['in_vehicle_time'], alpha=0.3, color="red", label="baseline")
    plt.hist(df_sn['in_vehicle_time'], alpha=0.3, color="blue", label="social")
    plt.legend()
    plt.title('Travel time comparison')
    plt.xlabel('Travel time (minutes)')
    plt.ylabel('Agent count')

    print "saving travel plot as travel_distribution_comparison.png"
    plt.savefig(out_dir + "/" + 'time_distribution_comparison.png')

def gen_comparison_travel_time_diff_plot(df_bl,df_sn,out_dir,filename,figure_index):
    print "--------------------------COMPARISON : Travel time differences --------------"
    times_bl = df_bl[['person_id', 'in_vehicle_time']]
    times_sn = df_sn[['person_id', 'in_vehicle_time']]
    # if len(times_bl) == len (times_sn):
    #     print "number of agents in baseline and social cases differ, aborting"
    #     exit()
    diff_df = pd.merge(times_bl, times_sn.rename(columns={'in_vehicle_time':'in_vehicle_time_sn'}), on='person_id',  how='left')
    diff_df['time_diff'] = diff_df['in_vehicle_time'] -diff_df['in_vehicle_time_sn'] # baseline - social
    diff_df = check_and_remove_nans(diff_df)

    bins = [-30,-15,-5,5,15, 30, 45, 60, 75]
    diff_df['time_diff_binned'] = pd.cut(diff_df['time_diff'], bins)
    outf = out_dir + "/" +  filename + '.csv'
    diff_df.to_csv(outf, sep="\t")
    s = pd.cut(diff_df['time_diff'], bins=bins).value_counts().sort_index()
    print (s)

    plt.figure(figure_index) # to differentiate figures


    plt.hist(diff_df['time_diff'], color="blue")
    plt.title('Travel time differences')
    plt.ylabel('Agent count')
    plt.xlabel('Travel time (minutes)')

    print "saving travel plot as", filename
    plt.savefig(out_dir + "/" + filename + ".png")


def gen_comparison_travel_dist_diff_plot(df_bl,df_sn,out_dir,filename, figure_index):
    print "--------------------------COMPARISON : distance differences --------------"
    dist_bl = df_bl[['person_id', 'distance']]
    dist_sn = df_sn[['person_id', 'distance']]
    # if len(times_bl) == len (times_sn):
    #     print "number of agents in baseline and social cases differ, aborting"
    #     exit()
    diff_df = pd.merge(dist_bl, dist_sn.rename(columns={'distance':'distance_sn'}), on='person_id',  how='left')
    diff_df['dist_diff'] = diff_df['distance'] - diff_df['distance_sn'] # baseline - social
    diff_df = check_and_remove_nans(diff_df)
    bins = [-10,-5,-1,1,5,10,15,20,25,30,35,40,45,50]
    diff_df['dist_diff_binned'] = pd.cut(diff_df['dist_diff'], bins)
    outf = out_dir + "/" +  filename + '.csv'
    diff_df.to_csv(outf, sep="\t")
    s = pd.cut(diff_df['dist_diff'], bins=bins).value_counts().sort_index()
    print (s)

    plt.figure(figure_index) # to differentiate figures
    plt.hist(diff_df['dist_diff'], color="blue")
    plt.title('Distance differences')
    plt.ylabel('Agent count')
    plt.xlabel('Distance (km)')

    print "saving travel plot as", filename
    plt.savefig(out_dir + "/" + filename + ".png")


def gen_comparison_blockage_aware_times_diff_plot(df_bl,df_sn,out_dir,figure_index):
    print "--------------------------COMPARISON : blockage aware time differences --------------"

    # diff_df = pd.merge(df_bl, df_sn.rename(columns={'time':'time_sn'}), on='id',  how='left')
    # info received agents may be higher if not all hitting blockage, so dont merge.
    time_diff = pd.DataFrame()

    for index, row in df_bl.iterrows(): # comparing against only agents that blcked
        blocked_id = int(row['id'])
        blocked_time = float(row['time'])
        sn_row = df_sn.loc[(df_sn['id'] == blocked_id)]
        info_received_time = float(sn_row['time'].iloc[0])
        # print blocked_id, blocked_time, info_received_time
        diff = blocked_time - info_received_time # baseline - social

        new_row = {'id': blocked_id,'diff': diff}
        time_diff = time_diff.append(new_row,ignore_index=True)

    time_diff = time_diff.astype({"id": int,"diff": float})
    time_diff['diff'] = time_diff['diff'].round(2)
    # print time_diff
    bins = [-30,-5,5,30,60,90,120,150,180,210,240,270,300,330,360,390,420]
    time_diff['time_diff_binned'] = pd.cut(time_diff['diff'], bins)
    outf = out_dir + "/" +  'blockage_aware_time_differences.csv'
    time_diff.to_csv(outf, sep="\t")
    s = pd.cut(time_diff['diff'], bins=bins).value_counts().sort_index()
    print (s)

    plt.figure(figure_index) # now plot the histrogram of 1D distance distribution
    binwidth = 10 # number of bins = 10

    # plt.hist(time_diff['diff'], color='steelblue', bins=np.arange(min(time_diff['diff']), max(time_diff['diff']) + binwidth, binwidth))
    plt.hist(time_diff['diff'], color='steelblue', bins=bins)
    plt.axvline(time_diff['diff'].mean(), color='black', linestyle='dashed', linewidth=1)
    min_ylim, max_ylim = plt.ylim()
    print_stats=time_diff['diff'].describe()[['mean','std','min','max']].reset_index().to_string(header=None, index=None)
    plt.text(time_diff['diff'].mean()*1.1, max_ylim*0.8,print_stats)
    # plt.text(time_diff['diff'].mean()*1.1, max_ylim*0.8, 'Std: {:.2f}'.format(time_diff['diff'].std()))

    # plt.hist(time_diff['diff'])
    plt.title('Difference in time of agents becoming aware about the blockage')
    plt.xlabel('Time (minutes)')
    plt.ylabel('Agent count')

    print "saving travel plot as blockage_aware_time_differences.png"
    plt.savefig(out_dir + "/" + 'blockage_aware_time_differences.png')
    return time_diff

def gen_comparison_time_distance_diff_plots_for_early_aware_agents(jny_bl,jny_sn,aware_time_diffs,out_dir,figure_index):
        for index, row in aware_time_diffs.iterrows():
            id = int(row['id'])
            time_diff = float(row['diff'])
            if time_diff <= 0 : # agents who got to know later than baseline, remove from both dfs
                jny_bl = jny_bl.drop(jny_bl[jny_bl['person_id'] == id].index)
                jny_sn = jny_sn.drop(jny_sn[jny_sn['person_id'] == id].index)

        print "removed agents who go to know later than in the baseline, now df sizes: baseline:", len(jny_bl), " sn:", len(jny_sn)
        gen_comparison_travel_time_diff_plot(jny_bl,jny_sn,output_dir_comparison,'time_differences_early_aware_agents',figure_index)
        figure_index = figure_index +1
        gen_comparison_travel_dist_diff_plot(jny_bl,jny_sn,out_dir,'distance_differences_early_aware_agents',figure_index)

# comparing only against agents that hit blockage in baseline.
# agent does not log distance after hit  blockage, because then agent is already active, meaning u cannot receive info again in SN
# agents blocked in baseline, may not become receive info (inactive), so always check based on basline df
def gen_comparison_blockage_aware_distance_diff_plot(df_bl,df_sn,out_dir,figure_index):
    print "--------------------------COMPARISON : blockage aware distance differences --------------"

    dist_diff = pd.DataFrame()
    id_set_bl = set(df_bl['id'])
    id_set_sn = set(df_sn['id']) #blockage aware agents
    print " Removing agents received info in SN but not blocked in BL:",len(id_set_sn.difference(id_set_bl))
    for id in id_set_sn: # iterate through this and remove agents that are not in df_bl
        # print type(id)
        if id not in id_set_bl:  # comparing against only agents got blocked in the baseline
            df_sn = df_sn.drop(df_sn[df_sn['id'] == id].index) # remove if not blocked in baseline



    bins = [-10,-5,-1,1,5,10,15,20,25,30,35,40,45,50]
    df_sn['distances'] = df_sn['distances'].round(2)
    df_sn['distances_binned'] = pd.cut(df_sn['distances'], bins)
    # outf = out_dir + "/" +  'blockage_aware_distance_differences.csv'
    # df_sn.to_csv(outf, sep="\t")
    s = pd.cut(df_sn['distances'], bins=bins).value_counts().sort_index()
    print (s)

    plt.figure(figure_index) # now plot the histrogram of 1D distance distribution
    binwidth = 2.5 # number of bins = 10
    plt.hist(df_sn['distances'], color='steelblue', bins=np.arange(min(df_sn['distances']), max(df_sn['distances']) + binwidth, binwidth))
    plt.axvline(df_sn['distances'].mean(), color='black', linestyle='dashed', linewidth=1)
    min_xlim, max_xlim = plt.xlim()
    min_ylim, max_ylim = plt.ylim()
    print_stats=df_sn['distances'].describe()[['mean','std','min','max']].reset_index().to_string(header=None, index=None)
    plt.text(max_xlim*0.5, max_ylim*0.8,print_stats)
    # plt.hist(df_sn['distances'])
    plt.title('Difference in distance of agents becoming  aware about the blockage')
    plt.xlabel('Distance (km)')
    plt.ylabel('Agent count')

    print "saving distance plot as blockage_aware_distance_differences.png"
    plt.savefig(out_dir + "/" + 'blockage_aware_distance_differences.png')
    return df_sn
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
    blockage_aware_times_df  = res[3]
    blockage_aware_distances_df = res[4]

    print "journeys_df_sn size ", len(journeys_df_sn)
    print "blocked_percepts_df_sn size ",len(blocked_percepts_df_sn)
    print "diffusion_df size ",len(diffusion_df)
    print "blockage_aware_times_df size ",len(blockage_aware_times_df)
    print "blockage_aware_distances_df size ",len(blockage_aware_distances_df)


    if not os.path.isdir(output_dir_comparison):
        os.makedirs(output_dir_comparison)
    gen_comparison_blocked_percept_plots(blocked_percepts_df_bl,blocked_percepts_df_sn,output_dir_comparison, 10)
    gen_comparison_distance_plots(journeys_df_bl,journeys_df_sn,output_dir_comparison,11)
    gen_comparison_travel_plots(journeys_df_bl,journeys_df_sn,output_dir_comparison,12)
    gen_comparison_travel_time_diff_plot(journeys_df_bl,journeys_df_sn,output_dir_comparison,'time_differences_all',13)
    gen_comparison_travel_dist_diff_plot(journeys_df_bl,journeys_df_sn,output_dir_comparison,'distance_differences_all',14)
    blockage_aware_time_diffs = gen_comparison_blockage_aware_times_diff_plot(blocked_percepts_df_bl,blockage_aware_times_df,output_dir_comparison,15)
    blockage_aware_distances_df = gen_comparison_blockage_aware_distance_diff_plot(blocked_percepts_df_bl, blockage_aware_distances_df,output_dir_comparison,16)
    ## global blockage_aware_distances_df is modified now !!
    gen_comparison_time_distance_diff_plots_for_early_aware_agents(journeys_df_bl,journeys_df_sn,blockage_aware_time_diffs,output_dir_comparison,17)

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
    blockage_aware_times_df  = res[3]
    blockage_aware_distances_df =  res[4]

    print "blocked_percepts_df size ", len(journeys_df_sn)
    print "journeys_df size ",len(journeys_df_sn)
    print "diffusion_df size ",len(diffusion_df)
    print "blockage_aware_times_df size",len(blockage_aware_times_df)
    print "blockage_aware_distances_df size ",len(blockage_aware_distances_df)
