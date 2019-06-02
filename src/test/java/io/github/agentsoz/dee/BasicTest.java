package io.github.agentsoz.dee;

import io.github.agentsoz.ees.Run;
import io.github.agentsoz.util.TestUtils;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicTest {

    // have tests in separate classes so that they run, at least under maven, in separate JVMs.  kai, nov'17

    private static final Logger log = LoggerFactory.getLogger(BasicTest.class) ;

    //@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;
    public MatsimTestUtils utils = new MatsimTestUtils() ;

    @Test
    public void basicTest() {

        utils.setTestClassAndMethod(this.getClass(),"basicTest"); // set test class and method name
        //	utils.getorSetOutputDirectory(); // set output dir path
        //	utils.createOutputDirectory(); // output dir is created by matsim based on config outdir

        String[] args = {
                "--config", "scenarios/surf-coast-shire/basic-test/social_network_experiments_diffusion.xml",
        };
        Run.main(args);

//        final String actualEventsFilename = utils.getorSetOutputDirectory() + "/output_events.xml.gz";
//        final String primaryExpectedEventsFilename = utils.getInputDirectory() + "/output_events.xml.gz";
//        TestUtils.comparingDepartures(primaryExpectedEventsFilename,actualEventsFilename,10.);
//        TestUtils.comparingArrivals(primaryExpectedEventsFilename,actualEventsFilename,10.);
//        TestUtils.comparingActivityStarts(primaryExpectedEventsFilename,actualEventsFilename, 10.);
//        TestUtils.compareFullEvents(primaryExpectedEventsFilename,actualEventsFilename, false);
    }
}
