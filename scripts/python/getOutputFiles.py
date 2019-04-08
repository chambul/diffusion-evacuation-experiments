#!/usr/bin/python
from xml.dom import minidom

def getTagAttNames(xmlFile):
        xmldoc = minidom.parse(xmlFile)
        jars = xmldoc.getElementsByTagName('jar')
        for jar in jars:
               print "%s" % jar.getAttribute("name")
        return ''


def getJarChildTagValues(xmlFile,jarName,childTag):
        xmldoc = minidom.parse(xmlFile)
        jars = xmldoc.getElementsByTagName('jar')
        for jar in jars:
		
                if jar.getAttribute("name") == jarName:
			tags = jar.getElementsByTagName(childTag)
			for tag in tags:
        	               # print "%s" % jar.getAttribute("name")
	                       print "%s" % tag.childNodes[0].data
        
        return ''

def getXMLTagValue(xmlFile,tagType):
	xmldoc = minidom.parse(xmlFile)
	tag = xmldoc.getElementsByTagName(tagType)[0]
	print "%s" % tag.childNodes[0].data
	
	return ''


def getScenarioTagValue(xmlFile,scenario,tagType):
        xmldoc = minidom.parse(xmlFile)
        tags = xmldoc.getElementsByTagName(tagType)
	for tag in tags:
		if tag.getAttribute("scenario") == scenario:
	
			#print "%s" % tag.getAttribute("scenario")
	     		print "%s" % tag.childNodes[0].data
       
        return ''


