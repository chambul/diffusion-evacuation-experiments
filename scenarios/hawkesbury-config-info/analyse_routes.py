import itertools
from collections import Counter
import operator
import collections
import re

file = "route_links.txt"
route_agent_counts="agent_counts_for_routes.txt"

counts = {}
routes = {}
i=0
with open(route_agent_counts) as f:
    content = f.readlines()
    for line in content:
        route = line.split(':')[0]
        count = line.split(':')[1]
        # print route,count
        route = re.findall(r'\b\d+\b',route)
        count = re.findall(r'\b\d+\b',count)
        # route = [int(s) for s in route.split() if s.isdigit()]
        # count = [int(s) for s in count.split() if s.isdigit()]
        # print route, " count is :",count
        routes[i] = route
        counts[i] = count
        i = i+1
    # content = [x.strip() for x in content]
print "number of routes found", len(routes)
print "number of agent counts found", len(counts)

result=[]
with open(file) as f:
    content = f.readlines()
    content = [x.strip() for x in content]
    # print content
    for line in content:
        for number in line.split(' '):
            result.append(number)

print "splitted all route links, found links", len(result)
# print result
cts = Counter(result)
# print counts
sorted_counts = sorted(cts.items(), key=operator.itemgetter(1)) #sort by value,links
sorted_dict = collections.OrderedDict(sorted_counts)
with open("route_counts_for_links.txt", "w") as f:
    for key,value in sorted_dict.items():
        st = key, str(value)
        f.write('{}\n'.format(st))
f.close()


link_list = list(dict.fromkeys(result)) #
print "counting routes for each link is done, removing duplicate link ids. Size ", len(link_list)
# def is_link_part_of_route(list,target_link):
#     res = False
#     for link in route:
#         # print type(link)
#         # print type(target_link)
#         if target_link is link:
#             res = True
#     return res

agent_counts = {}
for link in link_list:
    agent_counts[link] = 0
    for key,value in routes.items():
        # res = is_link_part_of_route(value,link)
        if str(link) in value:
            tot = agent_counts[link]
            val = counts[key]
            tot = tot + int(val[0])
            agent_counts[link] = tot

print "done counting agents for links", len(agent_counts)

sorted_agent_counts = sorted(agent_counts.items(), key=operator.itemgetter(1)) #sort by value, i.e. counts
sorted_agent_counts_dict = collections.OrderedDict(sorted_agent_counts)
# print sorted_agent_counts_dict
with open("agents_counts_for_links.txt", "w") as f:
    for key,value in sorted_agent_counts_dict.items():
        st = key, str(value)
        f.write('{}\n'.format(st))
f.close()
# print routes[0]

# print is_link_part_of_route(str(100),routes[0])
# print agent_counts
