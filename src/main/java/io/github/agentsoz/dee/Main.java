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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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

        // Run it
        new Run()
                .withModel(DataServer.getInstance(DATASERVER))
                .withModel(new BlockageInformationDiffusionModel(cfg.getModelConfig(eModelDiffusion),
                        DataServer.getInstance(DATASERVER),
                        new ArrayList<>(Arrays.asList(Utils.getAsSortedStringArray(bdiMap.keySet())))))
                .start(cfg, bdiMap);
    }
}
