#author Chaminda Bulumulla
# takes in scenario sn/bl and averages  diffusion.out, output_matsim_journeys, blocked_percepts
# only replaces certain files in avg directry.
# run from timestamp directry

import sys # for arg handling
import os
from glob import glob
import pandas as pd
import fnmatch
from pathlib import Path

args = len(sys.argv) - 1
if args != 1:
    print("argmument mismatch! call with correct args. E.g.:python script.py sn")
    exit()

scenario = str(sys.argv[1])
if scenario != "bl" and scenario != "sn":
    print "wrong input scenario, pass sn for social or bl for baseline"
    exit()

blocked_out_file = "blocked_times.out"
diffusion_out_file = "diffusion.out"
basic_stat_file = "basic_stats.out"
mat_file = "output_matsim_journeys.txt"
print "target scenario: ",scenario

avgdir =  'avg-' + scenario # having a different pattern than scenario-run; otherwise mavg dir will also be considered for avg

if not os.path.exists(avgdir):  # if avg dir doesnt exist, create one
    os.mkdir(avgdir)

def get_outputfiles_in_matching_subdirs(dir_type,file_pattern,file_list):
    #directories = glob("sn-*/")
    dir_pattern = dir_type + "-*/"# compelete the dir regex
    directories = glob(dir_pattern)
    for dir in directories:
        for path in Path(dir).rglob(file_pattern):
            file_list.append(str(path))


print "\n searching for",blocked_out_file,"files..."
blocked_out_matches = []
get_outputfiles_in_matching_subdirs(scenario,blocked_out_file, blocked_out_matches)
print "search complete: found",len(blocked_out_matches),"files..."
print('\n'.join(blocked_out_matches))
blk_df_list = []
for file in blocked_out_matches:
    bdf = pd.read_csv(file, header=None, sep=' ')
    blk_df_list.append(bdf)

bdf_concat  = pd.concat(blk_df_list)
#print bdf_concat
mean_bdf = bdf_concat.groupby(bdf_concat.index).mean()
mean_bdf = mean_bdf.astype(int) # coverting agent counts to int
#print mean_bdf
avg_dif_out_file = avgdir + "/" + blocked_out_file
print "averaging done, writing file",avg_dif_out_file
mean_bdf.to_csv(avg_dif_out_file, sep=' ',index=False) # same delimiter



#------matsim journey file--------
print "\n searching output_matsim_journeys.txt files..."
matFiles = []
get_outputfiles_in_matching_subdirs(scenario,mat_file, matFiles)
print "search complete: found",len(matFiles),"files..."
print("\n".join(matFiles))
mat_df_list = []
for file in matFiles:
    mdf = pd.read_csv(file, sep='\t')
    mat_df_list.append(mdf)

#print mat_df_list
mdf_concat  = pd.concat(mat_df_list)
#print mdf_concat
mean_mdf = mdf_concat.groupby(mdf_concat.person_id).mean()
#mean_mdf = mean_mdf.astype(int) # coverting agent counts to int
mean_mdf = mean_mdf.astype({"person_id": int, "journey_id": int})
#print mean_mdf
avg_matsim_out_file = avgdir + "/" + mat_file
print "averaging done, writing file",avg_matsim_out_file
mean_mdf.to_csv(avg_matsim_out_file, sep='\t',index=False) # same delimiter




 #------ diffusion.out files.......
if scenario == "sn":
 print "\n searching for",diffusion_out_file,"files..."
 dif_out_matches = []
 get_outputfiles_in_matching_subdirs(scenario,diffusion_out_file, dif_out_matches)
 print "search complete: found",len(dif_out_matches),"files..."
 print('\n'.join(dif_out_matches))
 dif_df_list = []
 for file in dif_out_matches:
     df = pd.read_csv(file, sep='\t')
     dif_df_list.append(df)


 df_concat  = pd.concat(dif_df_list)
 mean_df = df_concat.groupby(df_concat.index).mean()
 mean_df = mean_df.astype(int) # coverting agent counts to int
 #print mean_df
 avg_dif_out_file = avgdir + "/" + diffusion_out_file
 print "averaging done, writing file",diffusion_out_file
 mean_df.to_csv(avg_dif_out_file, sep='\t',index=False)

blocked_out_file = "basic_stats.out"
print "\n searching for",blocked_out_file,"files..."
blocked_out_matches = []
get_outputfiles_in_matching_subdirs(scenario,blocked_out_file, blocked_out_matches)
print "search complete: found",len(blocked_out_matches),"files..."
print('\n'.join(blocked_out_matches))
bstat_df_list = []
for file in blocked_out_matches:
    bsdf = pd.read_csv(file, header=None, sep='\s+')
    bstat_df_list.append(bsdf)

bsdf_concat  = pd.concat(bstat_df_list)
#print bsdf_concat
bsdf_concat.columns = ['type','count']
mean_bsdf_concat = bsdf_concat.groupby(bsdf_concat.type).mean()
#mean_bsdf_concat = mean_bsdf_concat.astype({"person_id": int, "journey_id": int})
# print mean_bsdf_concat
avg_blocked_out_file = avgdir + "/" + blocked_out_file
print "averaging done, writing file",avg_blocked_out_file
mean_bsdf_concat.to_csv(avg_blocked_out_file, sep='\t') # same delimiter
