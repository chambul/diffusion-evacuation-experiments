#author Chaminda Bulumulla
# call the script with matsim plans file to modify, disntance threshold, recency threshold and angle threshold

#import xml.etree.ElementTree as ET # fastest parser as matsim population files are huge
from lxml import etree as ET
import sys # for arg handling
import os # finding filename without extension

args = len(sys.argv) - 1
if args != 4:
    print("call the script with following args: file to modify, distanceT,recencyT,angleT")
    exit()

popfile = sys.argv[1]
distT = sys.argv[2]
recencyT = sys.argv[3]
angleT = sys.argv[4]

print("modifying matsim population file ", popfile)

parser = ET.XMLParser(remove_blank_text=True) # by default, pretty_print does not work for new elements. so parsing needs to happen with removing blank space
pop_tree = ET.parse(popfile,parser)
root = pop_tree.getroot()

#remove existing attribute elements
for  attribute in root.xpath("//person/attributes/attribute"):
    attribute.getparent().remove(attribute)

for person in root.xpath("//person"):
    #atts=person.findall('//attributes') #delete existing attributes
    # root.remove(person[0])

    attributes = person.find("attributes")

    #BDIAgentType
    type = ET.SubElement(attributes,'attribute')
    type.text = "io.github.agentsoz.dee.agents.TrafficAgent"
    type.attrib["name"] = "BDIAgentType"
    type.attrib["class"] = "java.lang.String"

    #distamce threshold
    distance = ET.SubElement(attributes,'attribute')
    distance.text = distT # #FIXME replace
    distance.attrib["name"] = "distanceFromTheBlockageThreshold"
    distance.attrib["class"] = "java.lang.Double"


    #recency threshold
    recency = ET.SubElement(attributes,'attribute')
    recency.text = recencyT # #FIXME replace
    recency.attrib["name"] = "blockageRecencyThreshold"
    recency.attrib["class"] = "java.lang.Integer"

    #angle threshold
    angle = ET.SubElement(attributes,'attribute')
    angle.text = angleT # #FIXME replace
    angle.attrib["name"] = "blockageAngleThreshold"
    angle.attrib["class"] = "java.lang.Integer"

new_file = os.path.splitext(popfile)[0] + "_modified.xml"
print("modification done! creating new xml file ", popfile)
pop_tree.write(new_file,  encoding='utf-8', xml_declaration=True, pretty_print=True)
