import sys # for arg handling
import os
from glob import glob
import pandas as pd
import numpy as np
import fnmatch
from pathlib import Path
import matplotlib.pyplot as plt

args = len(sys.argv) - 1
if args != 1:
    print("argmument mismatch! call with correct args. E.g.:python script.py ../../results-chapter4/May")
    exit()

tt_dir = sys.argv[1]
avgdir =  tt_dir + '/avg' # having a different pattern than scenario-run; otherwise mavg dir will also be considered for avg

if not os.path.exists(avgdir):  # if avg dir doesnt exist, create one
    os.mkdir(avgdir)


def get_mean_diffs_for_given_bins(dir,file,figure_index,column,bins,title,xlabel,ylabel):
    file_list = get_outputfiles_in_matching_subdirs(dir,file)
    df_list = []
    plt.figure(figure_index) # to differentiate figures
    plt.grid(axis='y', alpha=0.75)
    for f in file_list:
        df = pd.read_csv(f, sep='\t')
        # df = df[column]
        df_counts = pd.cut(df[column], bins=bins).value_counts().sort_index().to_frame().reset_index()
        df_counts.columns = ['bin','count']
        # print type(s)
        # print df_counts
        # plt.plot(s)
        df_list.append(df_counts)
    df_concat  = pd.concat(df_list)
    mean_df = df_concat.groupby(df_concat.bin).mean()
    mean_df.plot.bar(legend=None)
    plt.xticks(fontsize=8)
    levels = dir.split('/')[3].split('-')[-4:]
    case = "blockage detour level= " + levels[0] + ", diffusion level= " + levels[2]
    plt.title(title + '\n' + case)
    plt.xlabel(xlabel)
    plt.ylabel(ylabel)
    # print mean_df
    # plt.bar(mean_df.index,mean_df["count"],color='#069AF3')
    outf = file.split('.')[0] + "-avg.png"
    plt.savefig(avgdir + "/" + outf)

def get_mean_blocked_percept_timings(dir,file,figure_index):
    file_list = get_outputfiles_in_matching_subdirs(dir,file)
    df_list = []
    plt.figure(figure_index) # to differentiate figures
    plt.grid(axis='y', alpha=0.75)
    for file in file_list:
        df = pd.read_csv(file, sep='\t')
        plt.plot(df['time'], df['count'],linewidth=0.8, color='black') # add markers: '-ok', markersize=0.3,
        df_list.append(df)
    df_concat  = pd.concat(df_list)
    # print df_concat
    # col = "df_concat." + column
    mean_df = df_concat.groupby(df_concat.index).mean()
    plt.plot(mean_df['time'], mean_df['count'],linewidth=1.5, color='red') # add markers: '-ok', markersize=0.3,
    # print mean_df
    plt.savefig(avgdir + "/" + 'blocked_percept_times_avg.png')
    avg_dif_out_file = avgdir + "/" + "blocked_percept_times_avg.csv"
    print "averaging done, writing file",avg_dif_out_file
    mean_df.to_csv(avg_dif_out_file, sep='\t',index=False) # same delimiter


def get_outputfiles_in_matching_subdirs(dir,file_pattern):
    print "searching for ",file_pattern
    file_list = []
    # dir_pattern = dir_type #+ "/*/"# compelete the dir regex
    directories = glob(dir)
    # print (directories)
    for dir in directories:
        for path in Path(dir).rglob(file_pattern):
            file_list.append(str(path))
    print "search complete: found",len(file_list),"files..."
    print('\n'.join(file_list))
    return file_list

###------main script---------------
out_file = avgdir + "/avg_script.out"
sys.stdout = open(out_file, 'w')

get_mean_blocked_percept_timings(tt_dir,"blocked_percept_times_sn.csv",1)

#specify blcokage aware dist/time bins, and dist/time bins
bins_dist = [-10,-5,-1,1,5,10,15,20,25,30,35,40,45,50]
bins_time = [-30,-15,-5,5,15, 30, 45, 60, 75]
blockage_bins_time = np.arange(-30,330,30)
blockage_bins_dist = np.arange(0,25,5)

get_mean_diffs_for_given_bins(tt_dir,"blockage_aware_distance_differences.csv",2,"distances",blockage_bins_dist,"Distance difference in blockage awareness:",'Distance (km)','Agent count')
get_mean_diffs_for_given_bins(tt_dir,"blockage_aware_time_differences.csv",3,"diff",blockage_bins_time,"Time difference in blockage awareness:",'Time (minutes)','Agent count')
get_mean_diffs_for_given_bins(tt_dir,"distance_differences_all.csv",4,"dist_diff",bins_dist,"Travel distance differences:",'Distance (km)','Agent count')
get_mean_diffs_for_given_bins(tt_dir,"time_differences_all.csv",5,'time_diff',bins_time,"Travel time differences:",'Time (minutes)','Agent count')


#finally
sys.stdout.close()
