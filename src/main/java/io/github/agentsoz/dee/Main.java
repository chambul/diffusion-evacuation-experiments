package io.github.agentsoz.dee;

/*-
 * #%L
 * Diffusion Evacuation Experiments
 * %%
 * Copyright (C) 2014 - 2019 by its authors. See AUTHORS file.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import io.github.agentsoz.dataInterface.DataServer;
import io.github.agentsoz.ees.Config;
import io.github.agentsoz.ees.JillBDIModel;
import io.github.agentsoz.ees.Run;
import io.github.agentsoz.ees.util.Utils;
import io.github.agentsoz.util.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static io.github.agentsoz.ees.Run.DATASERVER;

public class Main {

    static final String OPT_CONFIG = "--config";
    // Model IDs in XML
    static final String eModelFire = "phoenix";
    static final String eModelDisruption = "disruption";
    static final String eModelMessaging = "messaging";
    static final String eModelMatsim = "matsim";
    static final String eModelBdi = "bdi";
    static final String eModelDiffusion = "diffusion";
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static double[] getRandomUTMCoords(double easting, double northing) //x,y
    {
        double[] utm={-1,-1};

        double radius = Math.sqrt(2776/170) *1000.0 ;  //sqrt of (Hawkesbury region Area/#SA 1 areas) * 1000(km->m)

        double max_northing = northing + radius ;
        double min_northing = northing - radius ;
        double max_easting = easting + radius ;
        double min_easting = easting  -radius ;

        Random r = new Random();
        double randNorthing = min_northing + (max_northing - min_northing) * r.nextDouble();
        double randEasting = min_easting + (max_easting - min_easting) * r.nextDouble();



        log.debug("random northing : "+randNorthing);
        log.debug("random easting : "+randEasting);

        if(randNorthing <= 0 || randEasting <= 0)
        {
            log.warn("generated UTM coords contain negative values, aborting with -1 -1 ");
            return utm;

        }

        else
        {
            utm[0]=randEasting;
            utm[1]=randNorthing;

            return utm;
        }
    }

    public static void main(String[] args) {

        Thread.currentThread().setName("dee");

        // Read the config
        Config cfg = new Config();
      //  Map<String,String> opts = cfg.parse(args);
        String configFile = args[1];
        cfg.loadFromFile(configFile); // e.g.  "--config", "scenarios/surf-coast-shire/dee-main-diffusion-test.xml",




        // Get BDI agents map from the MATSim population file
        log.info("Reading BDI agents from MATSim population file");
        Map<Integer, List<String[]>> bdiMap = Utils.getAgentsFromMATSimPlansFile(cfg.getModelConfig(eModelMatsim).get("configXml"));
        JillBDIModel.removeNonBdiAgentsFrom(bdiMap);
        Map<Integer,double[]> agentCordsMap = new HashMap<>();


        for(int id : bdiMap.keySet()){
            List<String[]> planElements = bdiMap.get(id);
            String[] cds = planElements.get(4)[1].split(",");
            Double[] cords = {Double.valueOf(cds[0]),Double.valueOf(cds[1])};
            double[] new_loc = getRandomUTMCoords(cords[0],cords[1]);
            Location evac_location = new Location("evac_location",cords[0],cords[1]);
//            log.info("distance = " + Location.distanceBetween(evac_location,new Location("home",new_loc[0],new_loc[1]))); //FIXME remove these variants
            agentCordsMap.put(id,new_loc);

        }
        log.info("extracted ABM agent locations, map size "+agentCordsMap.size());


        // Run it
        new Run()
                .withModel(DataServer.getInstance(DATASERVER))
                .withModel(new BlockageInformationDiffusionModel(cfg.getModelConfig(eModelDiffusion),
                        DataServer.getInstance(DATASERVER),
//                        new ArrayList<>(Arrays.asList(Utils.getAsSortedStringArray(bdiMap.keySet())))
                        agentCordsMap
                ))
                .start(cfg, bdiMap);
    }
}
