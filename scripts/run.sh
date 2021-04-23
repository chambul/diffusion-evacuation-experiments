#!/bin/sh

###
# #%L
# BDI-ABM Integration Package
# %%
# Copyright (C) 2014 - 2015 by its authors. See AUTHORS file.
# %%
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Lesser General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Lesser Public License for more details.
#
# You should have received a copy of the GNU General Lesser Public
# License along with this program.  If not, see
# <http://www.gnu.org/licenses/lgpl-3.0.html>.
# #L%
###


[ -d output ] && { printf "run.sh:removing MATSim output dir.." ; rm -r output; }


DIR=`dirname "$0"`
PROGRAM='java -Xmx6g -cp social_network_integration-0.0.1-SNAPSHOT.jar bushfire.BushfireMain'

#DEFAULT_ARGS='-c scenarios/nicta_original/nicta.xml -l hawkesbury.log -level DEBUG'
#DEFAULT_ARGS='-c scenarios/halls_gap/halls_gap.xml -l halls-gap.log -level DEBUG'
#DEFAULT_ARGS='-c scenarios/hawkesbury/hawkesbury.xml -l hawkesbury.log -level DEBUG'
#DEFAULT_ARGS='-c scenarios/test/hawkesbury.xml -l hawkesbury.log -level DEBUG'
DEFAULT_ARGS='-c case_studies/hawkesbury/hawkesbury.xml -outfile hawkesbury.out -logfile hawkesbury.log -loglevel DEBUG'
#DEFAULT_ARGS='-c scenarios/test_HG/halls_gap.xml -l halls-gap.log -level DEBUG'

# Print usage
$PROGRAM -h

# print args in use
printf "default args:\n  $DEFAULT_ARGS\n\n"

# print user args
UARGS="none"
USER_ARGS=""
if [ $# -ne 0 ]; then
  UARGS=$@
  USER_ARGS="$UARGS"
fi
printf "user args (will override defaults):\n  $UARGS\n\n"

# print full command
CMD="$PROGRAM $DEFAULT_ARGS $USER_ARGS"
printf "running:\n  "
printf "started on `date +"%B %d, %Y at %r"` \n  "
#printf "$CMD\n  "
#$CMD # prints all the logs to the console.
#$CMD >/dev/null 2>&1 #does not print all
printf "finished on `date +"%B %d, %Y at %r"` \n\n"
