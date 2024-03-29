package io.github.agentsoz.dee.agents;

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

public class SocialTrafficAgent extends io.github.agentsoz.socialnetwork.SocialAgent {

    private double lastKnownBlockageTime = 0.0;
    public SocialTrafficAgent(int id){
        super(id);
    }

    public SocialTrafficAgent(int id, double x, double y){
        super(id,x,y);
    }

    public double getLastKnownBlockageTime() {
        return lastKnownBlockageTime;
    }

    public void setLastKnownBlockageTime(double lastKnownBlockageTime) {
        this.lastKnownBlockageTime = lastKnownBlockageTime;
    }




}
