
events="output/matsim/ITERS/it.0/0.events.xml.gz"
jill_out="output/jill.out"
diff_out="output/diffusion.out"
diff_log="output/diffusion.log"
stdout="stdout.out"

#outputs
blocked_out="./blocked_times.out"
basic_stat="./basic_stats.out"


function basic_stats() {
  printf "extracting basic stats of ${PWD##*/}. Extrcted values depend on log messages!  \n"

  #stdout
  ct0=$(grep "error" $stdout)
  printf " checking for errors in stdout (printed below if any): \n $ct0 \n "


  #matsim outputs
  ct1=$(zgrep "person" $events | cut -d '"' -f 6 | sort | uniq | wc -l)
  printf "total_matsim_agents: $ct1 \n " > $basic_stat

  ct2=$( zgrep "Evacuation" $events | wc -l)
  printf "evacuation_activity_ends: $ct2 \n" >> $basic_stat

  ct3=$( zgrep "Safe" $events | wc -l)
  printf "safe_activity_starts: $ct3 \n" >> $basic_stat

  ct4=$( zgrep "tuck" $events | wc -l)
  printf "stuck_agents: $ct4 \n" >> $basic_stat


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


  #diffusion outputs
  if [ -f $diff_log ];
  then
        printf "diffusion.log found.."
        ct8=$(grep "inactive agents" $diff_log | cut -d ':' -f 2)
        printf "inactive_agents: $ct8 \n" >> $basic_stat

        printf " final active agents : \n"
        head -1 $diff_out
        tail -1 $diff_out

 else
    printf "file diffusion log not found !!! \n"
  fi


  # extract 2nd and 4th cols, sort them based on col4 (ids) remove duplicates (-u) compare according to string numerical value (-n)
  grep "blocked" $jill_out | cut -d ' ' -f 2,4 | sort -k2 -n -u > $blocked_out

}

basic_stats
