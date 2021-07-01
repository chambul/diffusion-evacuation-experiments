batch running simulations: modify and run is the main script

batch running simulations configuration: run-config.txt
config for batch running different network, blockage, diffusion level combinations of scenarios.
new_dir param selects whether to create a new dir or not.
for network,pass sw,rand-reg(use sw with p=0),random as parameters. becaeuse sw is used for two network types. you cant run them together.

averaging simulations
python average_results_over_run_dirs.py is the main script. finds certain type of files (by seraching in all sub dirs) and averages

analysis
generate_dist_time_blocked_diffusion_analysis_plots.py is the main script. batch-run-generate_dist_time_blocked_diffusion_analysis_plots.py bash script allows
to parrelly execute this for all 9 scenarios of a network type.generate_dist_time_blocked_diffusion_analysis_plots.py script can analyse multiple runs.


analysing/visualing plots
in results-chapter there is a combine-pdf.sh, which combines necessary plots for a given network, timestamp dir and opens them as pdfs
combining can be done based on a single network, or blockage. (this can be easily modified to suit different needs)
