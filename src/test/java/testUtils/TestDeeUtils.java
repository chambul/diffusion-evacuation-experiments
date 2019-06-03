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

import io.github.agentsoz.dee.blockage.Blockage;
import io.github.agentsoz.dee.blockage.DataTypes;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestDeeUtils {


    private static final Logger log = LoggerFactory.getLogger(TestDeeUtils.class);

    @Test
    public  void testFindBlcokageNames (){

        Assert.assertEquals(DataTypes.GROSSMANDS, Blockage.findBlockageNameUsingLinkId("11206"));
        Assert.assertEquals(DataTypes.GREAT_OCEAN_ROAD, Blockage.findBlockageNameUsingLinkId("12340-12338-12336-12334-12332"));
        Assert.assertEquals(DataTypes.GROSSMANDS, Blockage.findBlockageNameUsingLinkId("11207"));
        Assert.assertEquals(null, Blockage.findBlockageNameUsingLinkId("12340")); // part of the compound link id
    }
}
