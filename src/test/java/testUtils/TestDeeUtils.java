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
import io.github.agentsoz.dee.blockage.Blockage;
import io.github.agentsoz.dee.blockage.DataTypes;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.agentsoz.util.Location;

import java.awt.*;

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

        Location line1P1 = new Location("cur", 0,0);
        Location line1P2 = new Location("cur", 1,0);
        Location line2P1 = new Location("cur", 1,0);
        Location line2P2 = new Location("cur", 1,4);

        double result = agent.estimateBlockageInCurrentDirectionOrNot(line1P1,line1P2,line2P1,line2P2);


        //method2
        double a1= agent.getAngle(new Point(0,0),new Point(1,0));
        double a2 = agent.getAngle(new Point(1,0), new Point(1,4));

        double res = a1-a2;

    }


}
