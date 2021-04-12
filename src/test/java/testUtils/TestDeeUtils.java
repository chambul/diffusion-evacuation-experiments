package testUtils;

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

import io.github.agentsoz.dee.agents.TrafficAgent;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.agentsoz.util.Location;

public class TestDeeUtils {


    private static final Logger log = LoggerFactory.getLogger(TestDeeUtils.class);

//    @Test
//    public  void testFindBlcokageNames (){

//        Assert.assertEquals(DataTypes.GROSSMANDS, Blockage.findBlockageNameUsingLinkId("11206"));
//        Assert.assertEquals(DataTypes.GREAT_OCEAN_ROAD, Blockage.findBlockageNameUsingLinkId("12340-12338-12336-12334-12332"));
//        Assert.assertEquals(DataTypes.GROSSMANDS, Blockage.findBlockageNameUsingLinkId("11207"));
//        Assert.assertEquals(null, Blockage.findBlockageNameUsingLinkId("12340")); // part of the compound link id
//    }

    @Test
    public void testBlockageInDirectionEstimation(){

        TrafficAgent agent =  new TrafficAgent("1");
        agent.setBlockageAngleThreshold(45);


        //parameters: cur location, blockage location, destiation location

        //0 - 180
        double result1 = agent.getSmallestAngleBetweenTwoLines(new Location("test",0,0),new Location("test",2,0),new Location("test",2,2));
        double result2 = agent.getSmallestAngleBetweenTwoLines(new Location("test",0,0),new Location("test",2,0),new Location("test",0,2));
        double result3 = agent.getSmallestAngleBetweenTwoLines(new Location("test",0,0),new Location("test",2,0),new Location("test",-2,-2));
        double result4 = agent.getSmallestAngleBetweenTwoLines(new Location("test",0,0),new Location("test",2,0),new Location("test",-2,0));

        // 180 - 360
        double result5 = agent.getSmallestAngleBetweenTwoLines(new Location("test",0,0),new Location("test",2,0),new Location("test",-2,-2));
        double result6 = agent.getSmallestAngleBetweenTwoLines(new Location("test",0,0),new Location("test",2,0),new Location("test",0,-2));
        double result7 = agent.getSmallestAngleBetweenTwoLines(new Location("test",0,0),new Location("test",2,0),new Location("test",2,-2));
        double result8 = agent.getSmallestAngleBetweenTwoLines(new Location("test",0,0),new Location("test",2,0),new Location("test",2,0));

        double result9 = agent.getSmallestAngleBetweenTwoLines(new Location("test",5,4),new Location("test",2,0),new Location("test",5,0));


        Assert.assertEquals(45,result1,0.0); // anti-clockwise 45
        Assert.assertEquals(90,result2,0.0); // anti-clockwies 90
        Assert.assertEquals(135,result3,0.0); // blockage and destination are in opposite directions
        Assert.assertEquals(180,result4,0.0); // anti-clockwise/clockwise 180

        Assert.assertEquals(135,result5,0.0); // anti-clockwise 225, clockwise 135
        Assert.assertEquals(90,result6,0.0); // anti-clockwise 270, clockwise 90
        Assert.assertEquals(45,result7,0.0); // anti-clockwise 315, clockwise 45
        Assert.assertEquals(0,result8,0.0); // anti-clockwise 315, clockwise 45

        Assert.assertEquals(36.86989764584401,result9,0); // anti-clockwise 360, clockwise 0
    }


}
