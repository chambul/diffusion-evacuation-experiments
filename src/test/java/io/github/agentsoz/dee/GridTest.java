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

import io.github.agentsoz.ees.Run;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GridTest {

    // have tests in separate classes so that they run, at least under maven, in separate JVMs.  kai, nov'17

    private static final Logger log = LoggerFactory.getLogger(GridTest.class);

    //@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    public void gridWorldTest() {

        utils.setTestClassAndMethod(this.getClass(), "gridWorldTest"); // set test class and method name
        //	utils.getorSetOutputDirectory(); // set output dir path
        //	utils.createOutputDirectory(); // output dir is created by matsim based on config outdir

        String[] args = {
                "--config", "scenarios/grid/dee-main.xml",
        };
//        Run.main(args);

//        final String actualEventsFilename = utils.getorSetOutputDirectory() + "/output_events.xml.gz";
//        final String primaryExpectedEventsFilename = utils.getInputDirectory() + "/output_events.xml.gz";
//        TestUtils.comparingDepartures(primaryExpectedEventsFilename,actualEventsFilename,10.);
//        TestUtils.comparingArrivals(primaryExpectedEventsFilename,actualEventsFilename,10.);
//        TestUtils.comparingActivityStarts(primaryExpectedEventsFilename,actualEventsFilename, 10.);
//        TestUtils.compareFullEvents(primaryExpectedEventsFilename,actualEventsFilename, false);
    }
}
