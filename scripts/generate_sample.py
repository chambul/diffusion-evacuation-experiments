#author Chaminda Bulumulla
from lxml import etree as ET
import sys # for arg handling


args = len(sys.argv) - 1
if args != 2:
    print "call the script with following args: matsim population file,sample size"
    exit()


file = sys.argv[1]
sample_size = int(sys.argv[2])
parser = ET.XMLParser(remove_blank_text=True)
pop_tree = ET.parse(file, parser)
pop_root = pop_tree.getroot()

print "generating a sample size of ", sample_size

new_root = ET.Element('population')
sample_tree = ET.ElementTree(new_root)

i=0
for person in pop_root.findall("person"):
    if i < sample_size:
        person.attrib["id"] = str(i)
        new_root.insert(i,person)
        i=i+1


#print(ET.tostring(sample_tree.getroot(), pretty_print=True).decode("utf-8"))
outFile = 'sample.xml'
with open(outFile, "w") as f:
    f.write('<!DOCTYPE population SYSTEM "http://www.matsim.org/files/dtd/population_v6.dtd"> \n<?xml version="1.0" encoding="utf-8"?> \n')
    f.write(ET.tostring(sample_tree.getroot(), pretty_print=True,encoding="utf-8"))
