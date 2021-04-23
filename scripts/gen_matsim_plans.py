#author Chaminda Bulumulla
#from lxml import etree
import xml.etree.ElementTree as etree
from xml.dom import minidom

def create_plan(root,id,dep_time):
    person = etree.SubElement(root, 'person',id=id)
    attributes = etree.SubElement(person, 'attributes')
    agentType = etree.SubElement(attributes, 'attribute',attrib={'name':'BDIAgentType', 'class':'java.lang.String'})
    agentType.text =  "io.github.agentsoz.dee.agents.TrafficAgent"
    distance = etree.SubElement(attributes, 'attribute',attrib={'name':'distanceFromTheBlockageThreshold', 'class':'java.lang.Double'})
    distance.text =  "5.0"
    recency = etree.SubElement(attributes, 'attribute',attrib={'name':'blockageRecencyThreshold', 'class':'java.lang.Integer'})
    recency.text =  "45"
    angle = etree.SubElement(attributes, 'attribute',attrib={'name':'blockageAngleThreshold', 'class':'java.lang.Integer'})
    angle.text =  "60"

    plan = etree.SubElement(person, 'plan',selected='yes')
    act_evac = etree.SubElement(plan, 'activity',type='Evacuation',link="335",x="150.723418843", y="-33.6041740859", start_time="00:00:00", end_time=dep_time)
    leg = etree.SubElement(plan, 'leg',mode="car", dep_time=dep_time)
    act_safe = etree.SubElement(plan, 'activity',type="Safe", link="110",x="150.935009228077", y="-33.8081127291702")


# Create the root element
root = etree.Element('population')
tree = etree.ElementTree(root)

dep="00:40:00"
i=0
while i<300:
    create_plan(root,str(i),dep)
    i=i+1
# Save to XML file
#outFile = open('output.xml', 'w')
xmlstr = minidom.parseString(etree.tostring(root)).toprettyxml(indent="   ")
with open("output.xml", "w") as f:
    f.write('<!DOCTYPE population SYSTEM "http://www.matsim.org/files/dtd/population_v6.dtd">\n')
    f.write(xmlstr.encode('utf-8'))
#tree.write(outFile,encoding='utf-8') # pretty_print=True
