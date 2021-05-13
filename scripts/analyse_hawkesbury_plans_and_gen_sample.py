#author Chaminda Bulumulla
# does not sequence agent ids from 0. so use the command in scenarios/hawkesbury-config-info/
from lxml import etree as ET
import sys # for arg handling
import pprint
import random
import math


args = len(sys.argv) - 1
if args != 3:
    print "call the script with following args: matsim output_plans_file, matsim intput population file"
    exit()


def get_percentage_id_list(dict,tot,sample):
    new_list = [ ]
    tot_pop_per = 0
    for key in dict:
        list = dict[key]
        size = float(len(list))
        pop_per = (size/tot) * 100
        tot_pop_per = tot_pop_per + pop_per
        sample_fraction = int(math.ceil(sample * pop_per/100)) # rounds to next higher integer (even if .0 factions ther will be agents)
        print "population percentage: ",pop_per, " sample size: ", sample_fraction
        if sample_fraction > 0:
            l = random.sample(list,sample_fraction)
            for i in l:
                new_list.append(i)
    print "total percentages considered (should be 100%): ", tot_pop_per
    return new_list


#------main script-----
out_plans_file = sys.argv[1]
input_plans_file = sys.argv[2]
sample_size = sys.argv[3]

parser = ET.XMLParser(remove_blank_text=True)
pop_tree = ET.parse(out_plans_file , parser)
pop_root = pop_tree.getroot()

route_based_count = {}
print "analysing output plans.. "

for person in pop_root.findall("person"):
    # print "person found", person.attrib["id"]
    for plan in person.findall("plan"):
        for leg in plan.findall("leg"):
            for route in leg.findall("route"):
                # print "route found: ",route.text
                key = str(route.text.strip())
                id = str(person.attrib['id'])
                # print " key: ",key, " | id: ",id
                if key not in route_based_count:
                    new_list = [id]
                    route_based_count[key] = new_list
                else:
                    list = route_based_count.get(key)
                    list.append(id)




# route_dict = {i:route_list.count(i) for i in route_list}
print "total routes found: ",len(route_based_count)

tot_agents = 0
for key in route_based_count:
    # print key," : ", len(route_based_count[key]), "\n"
    tot_agents = tot_agents + len(route_based_count[key])

print "total agents founds: ", tot_agents
# pprint.pprint(route_based_count)

print "selecting agent ids randomly based on percentages...  ", sample_size
sample_id_list = get_percentage_id_list(route_based_count,int(tot_agents),int(sample_size))
print "expected sample input plan size ", sample_size
print "actual selected sample size ", len(sample_id_list)
# print sample_id_list
#generating sample input file
parser = ET.XMLParser(remove_blank_text=True)
pop_tree = ET.parse(input_plans_file, parser)
pop_root = pop_tree.getroot()


new_root = ET.Element('population')
sample_tree = ET.ElementTree(new_root)

i=0
for person in pop_root.findall("person"):
    id = str(person.attrib["id"])
    if id in sample_id_list:
        new_root.insert(i,person)
        i=i+1

print "sample input plan size: ", i
# print(ET.tostring(sample_tree.getroot(), pretty_print=True).decode("utf-8"))
outFile = 'sample.xml'
with open(outFile, "w") as f:
    f.write('<?xml version="1.0" encoding="utf-8"?> \n <!DOCTYPE population SYSTEM "http://www.matsim.org/files/dtd/population_v6.dtd"> \n')
    f.write(ET.tostring(sample_tree.getroot(), pretty_print=True,encoding="utf-8"))
