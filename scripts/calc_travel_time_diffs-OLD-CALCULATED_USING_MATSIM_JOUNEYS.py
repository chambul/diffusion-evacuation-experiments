#author Chaminda Bulumulla

from lxml import etree as ET
import sys # for arg handling
import gzip
import csv
import pandas as pd # for binning time differences into time intervals
import matplotlib.pyplot as plt # plot bin data
import numpy as np

#need to clearly specify act and type - does not consider conflicts
# use activity act that distiguish actend and actstart types.
def store_start_end_travel_times(tree,dict,act,type):
    root = tree.getroot()
    path = "//events/event[@type='{}' and @actType='{}']".format(type,act)
    for event in root.xpath(path):
        #print ET.tostring(event)
        time = event.get("time")
        id = event.get("person")
        dict[id] = time

#    print("printing dictionary  of ",type)
#    for x in dict.keys():
#        print x +" => " + dict[x]

#-------------main script-------------
## remove script name from args
args = len(sys.argv) - 1
if args != 4:
    print("call the script with following args: baseline matsim events file(.gz), sn matsim events file (.gz), travel start activity, travel end activity")
    exit()

# global variables
basefile = sys.argv[1]
snfile = sys.argv[2]
start_act = sys.argv[3]
end_act = sys.argv[4]

# dictionaries
baseStartTs = {}
baseEndTs = {}
snStartTs = {}
snEndTs = {}
timeDiffs = {}

#gunzip events xml files and parse
base_events  = gzip.open(basefile, 'r')
base_tree = ET.parse(base_events)
sn_events = gzip.open(snfile, 'r')
sn_tree = ET.parse(sn_events)

#print(base_events)
#print(sn_events)
print "extracting and populating start and end travel times"
store_start_end_travel_times(base_tree,baseStartTs,start_act,"actend")
store_start_end_travel_times(base_tree,baseEndTs,end_act,"actstart")
store_start_end_travel_times(sn_tree,snStartTs,start_act,"actend")
store_start_end_travel_times(sn_tree,snEndTs,end_act,"actstart")

print("extraction done. Lenghts (check if equal) of baseStart baseEnd snStart snEnd  dictionaries are: ",
len(baseStartTs), len(baseEndTs),len(snStartTs), len(snEndTs))
#cacluations
if len(baseStartTs) != len(baseEndTs) or len(baseStartTs) != len(snStartTs) or len(snStartTs) != len(snEndTs):
    print "dictionary lengths are unequal, aborting"
    exit()

for id in baseStartTs:
    baseline_start_time = baseStartTs[id] #baseline travel times
    baseline_end_time = baseEndTs[id]
    baseline_travel_time = float(baseline_end_time) - float(baseline_start_time)

    sn_start_time = snStartTs[id] #sn travel times
    sn_end_time = snEndTs[id]
    sn_travel_time = float(sn_end_time) - float(sn_start_time)

    diff = baseline_travel_time - sn_travel_time #delays with respect to the baseline
    timeDiffs[id] = diff/60 # to make it in minutes


print "time difference calculation completed:"
for x in timeDiffs.keys():
        print "agent",x,": ", timeDiffs[x]

#output to file now
out_file = "time-differences.csv"
with open(out_file, 'w') as f:
    f.write("id,time_difference\n")
    for key in timeDiffs.keys():
        f.write("%s,%s\n"%(key,timeDiffs[key]))


#group differences into 30 mins bins
#d = { 'percentage': [46,44,100,42]} #dictionary
#df = pd.DataFrame(data=d)
df = pd.DataFrame(data=timeDiffs.values()) # this is ordered, although the timeDiffs is not ordered.
bins = [0, 15, 30, 45, 60, 75] #FIXME can pass a value to bin. also needs to consider negatives.

df['binned'] = pd.cut(timeDiffs.values(), bins)
#print df
s = pd.cut(timeDiffs.values(), bins=bins).value_counts()
print (s)

time_bins_count = pd.DataFrame({'bins': s.index, 'value_counts': s.values})
y_counts = [10, 50, 70, 30, 100] ## FIXME replace this with time differnces



plt.xlabel("Time bins")
plt.ylabel("Agent count")
plt.title("Time differences (binned into intervals)")


bins.pop(0) # remove first element
plt.bar(bins,y_counts,width=1.5,color="blue",align='center')

plt.xticks(bins,time_bins_count ['bins']) # use bins values to position x values
#plt.show()
plt.savefig('time-differences-binned.png', bbox_inches='tight')
