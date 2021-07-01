
events="output/matsim/ITERS/it.0/0.events.xml.gz"
jill_out="output/jill.out"
diff_out="output/diffusion.out"
diff_log="output/diffusion.log"
stdout="stdout.out"

#outputs
blocked_out="./blocked_times.out"
basic_stat="./basic_stats.out"
info_received_times="./info_received_times.out"
info_received_distences="./info_received_distance_from_blockage.out"



function basic_stats() {
  printf "extracting basic stats of ${PWD##*/}. Extrcted values depend on log messages!  \n"

  #stdout
  ct0=$(grep "error" $stdout)
  printf " checking for errors in stdout (printed below if any): \n $ct0 \n \n"

  ct01=$(grep "shutdown completed" $stdout)
  printf " checking for for a successful shutdown: \n $ct01 \n "

  #matsim outputs
  ct011=$(zgrep "stuckAndAbort" $events)
  printf " checking for any stuck agents: \n $ct011 \n "

  ct1=$(zgrep "person" $events | cut -d '"' -f 6 | sort | uniq | wc -l)
  printf "total_matsim_agents: $ct1 \n " > $basic_stat

  ct2=$( zgrep "Evacuation" $events | wc -l)
  printf "evacuation_activity_ends: $ct2 \n" >> $basic_stat

  ct3=$( zgrep "Safe" $events | wc -l)
  printf "safe_activity_starts: $ct3 \n" >> $basic_stat

  ct4=$( zgrep "stuckAndAbort" $events | wc -l)
  printf "stuck_agents: $ct4 \n" >> $basic_stat

  ct66=$(zless $events | tail -2 | head -1 | cut -d '"' -f 2)
  printf "evac_time: $ct66 \n" >> $basic_stat

  # Jill/BDI outputs
  #same agent can receive multiple congestion percepts, so not removing duplicates
  ct5=$(grep "congestion" $jill_out | wc -l)
  printf "congestion_percepts: $ct5 \n" >> $basic_stat


  ct10=$(grep "blocked" $jill_out | cut -d ' ' -f 4 | sort | uniq | wc -l)
  printf "blocked_agents: $ct10 \n" >> $basic_stat


  ct6=$(grep "proactive" $stdout | cut -d ' ' -f 13)
  printf "proactive_reroutes: $ct6 \n" >> $basic_stat

  ct7=$(grep "reactive" $stdout | cut -d ' ' -f 18)
  printf "reactive_reroutes: $ct7 \n" >> $basic_stat

  # grep "Initialising blockage" $stdout | cut -d ' ' -f 11,12,13 > $blockage_info

  #diffusion outputs
  if [ -f $diff_log ];
  then
        printf "diffusion.log found.."
        ct8=$(grep "inactive agents" $diff_log | cut -d ':' -f 2)
        printf "inactive_agents: $ct8 \n" >> $basic_stat

        # no sorting done assuming that there wont be any duplicates
        grep "diffusion_content" $jill_out | cut -d ' ' -f 2,4 > $info_received_times

        ct88=$(cat $info_received_times | wc -l)
        printf "info_recevied_agents: $ct88 \n" >> $basic_stat

        grep "received blockage info, blockage driving distance is" $stdout | cut -d ' ' -f9,17 > $info_received_distences

        printf " final active agents : \n"
        head -1 $diff_out
        tail -1 $diff_out

 else
    printf "file diffusion log not found !!! \n"
  fi


  # extract 2nd and 4th cols, sort them based on col4 (ids) remove duplicates (-u) compare according to string numerical value (-n)
  grep "blocked" $jill_out | cut -d ' ' -f 2,4 | sort -k2 -n -u > $blocked_out

  # grep "diffusion_content" output/jill.out |  cut -d ' ' -f 4,14,15 > $info_received_locations

}

basic_stats
