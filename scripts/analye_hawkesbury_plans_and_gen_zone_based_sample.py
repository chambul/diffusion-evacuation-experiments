#author Chaminda Bulumulla
# does not sequence agent ids from 0. so use the command in scenarios/hawkesbury-config-info/
from lxml import etree as ET
import sys # for arg handling
import pprint
import random
import math


args = len(sys.argv) - 1
if args != 2:
    print "call the script with following args: matsim intput population file"
    exit()



#------main script-----
input_plans_file = sys.argv[1]
zones = sys.argv[2]

parser = ET.XMLParser(remove_blank_text=True)
pop_tree = ET.parse(input_plans_file , parser)
pop_root = pop_tree.getroot()

route_locs = {}
print "analysing  matsim input plans.. "
for person in pop_root.findall("person"):
    # print "person found",
    for plan in person.findall("plan"):
        id = person.attrib["id"]
        for activity in plan.findall("activity"):
            if activity.attrib['type'] == "Evacuation":
                 x = activity.attrib['x']
                 y = activity.attrib['y']
                 if x not in route_locs:
                    id_list = [id]
                    route_locs[x] = id_list
                 else:
                    list = route_locs.get(x)
                    list.append(id)

# print route_locs
print "collected route locations", len(route_locs)
tot = 0
for x in route_locs:
    list = route_locs[x]
    tot = tot + len(list)
    print "location ",x," size ",len(list)

print "total agent ids added ", tot


print "searching for the zone in the route location dictionary.."

zone_list = zones.split(",")
zone_ids = []
print "zone list: ", zone_list
for zone in zone_list:
    if zone not in route_locs:
        print "zone specified by ", zone, " is not found, aborting"
        exit()
    else:
        ids = route_locs[zone]
        zone_ids.extend(ids) # add all zone ids
        print "zone found, has ",len(ids), " agents"





#generating sample input file
new_root = ET.Element('population')
sample_tree = ET.ElementTree(new_root)

i=0
for person in pop_root.findall("person"):
    id = str(person.attrib["id"])
    if id in zone_ids:
        new_root.insert(i,person)
        i=i+1

# change id sequence
id=0
for person in new_root.findall("person"):
    person.attrib["id"] = str(id)
    id=id+1

print "sample input plan size: ", id
# print(ET.tostring(sample_tree.getroot(), pretty_print=True).decode("utf-8"))
outFile = 'matsim-plans-gk4-zones-' + str(len(zone_list))  + '.xml'
with open(outFile, "w") as f:
    f.write('<?xml version="1.0" encoding="utf-8"?> \n <!DOCTYPE population SYSTEM "http://www.matsim.org/files/dtd/population_v6.dtd"> \n')
    f.write(ET.tostring(sample_tree.getroot(), pretty_print=True,encoding="utf-8"))
