package io.github.agentsoz.dee.blockage;

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

import io.github.agentsoz.util.Location;
import java.util.*;

public class Blockage extends Location{

   // private String name;


    private double lastUpdatedTime;
    private double distToBlockage = -1.0;
    private boolean congestionNearBlockage;
    private double latestObservedTime; // latest time that the blockage was observed
    private double latestInfoReceivedTime; // latest time that an information is received about the blockage from SN
    private recency  recencyOfBlockage=recency.OLD;
    private boolean blockageInCurrentDirection;
    private boolean noBlockageImpact;
    private double reconsiderTime=-1.0;

    public enum recency{
        RECENT,
        OLD
    }


    // contains link ids of all blockage points for referencing; so that we can include the names of the blockages in the SN information
    private static  Map<String, ArrayList<String>> allBlockagePointsWithLinks = new HashMap<String, ArrayList<String>>() {{  //#FIXME  Move this initialisation to configuration level
        put(DataTypes.GROSSMANDS, new ArrayList<String>( Arrays.asList("11206","11207") ));
        put(DataTypes.GREAT_OCEAN_ROAD, new ArrayList<String>( Arrays.asList("12340-12338-12336-12334-12332","12331-12333-12335-12337-12339")) );
        put("grid_network_blockage", new ArrayList<String>( Arrays.asList("2-3","4-5r","12-18")) ); //#FIXME grid network blockage
        put("hawkesbury_blockage_link352", new ArrayList<String>( Arrays.asList("352")));
        put("hawkesbury_blockage_link221", new ArrayList<String>( Arrays.asList("221")));

    }};


    // blockages stored as Locations
    private static  List<Location> allBlockageLocations= new ArrayList<Location>() {{  //#FIXME  get the mid point of to from nodes as coords?
        add( new Location(DataTypes.GROSSMANDS,783437.6291368047,5748322.732670321) ); // from node of link 11207 - distnace is arond 550m
        add(new Location(DataTypes.GREAT_OCEAN_ROAD,788600.3567188493,5753735.008117045)); // from node of 12340-12338-12336-12334-12332 -distance is around 450m
        add(new Location("grid_network_blockage",2000.0,0));
        add(new Location("hawkesbury_blockage_link352",150.892076947,-33.7752260091277)); // nodes 250: closest to where the agent is stuck
        add(new Location("hawkesbury_blockage_link221",150.76031966978286,-33.69374727323568)); // node x,y coords as stuck at node 162, start of the link
    }};


    public Blockage(String name, double x, double y){
          super(name,x,y);
    }

    public static  Blockage createBlockageFromName(String name) { // find cords for given blockage name and return blockage object
        Blockage newBlockage = null;
        for (Location blockage : allBlockageLocations) {
            if (blockage.getName().equals(name)) {
                newBlockage =  new Blockage(name, blockage.getX(), blockage.getY());
            }

        }
        return newBlockage;
    }


    //given a location from the blocked percept, find and return closest blockage point name based on beeline distance
//    @Deprecated
//    public static String getBlockageNameBasedOnBlockedPerceptCords(Location curLoc){
//        String name=null;
//        double minDistance = Double.POSITIVE_INFINITY;
//
//        for(Location blockage:allBlockageLocations){
//            double dist = Location.distanceBetween(curLoc,blockage);
//            if(dist <= minDistance) {
//                name = blockage.getName();
//            }
//        }
//
//        return name;
//    }


    // get name of the blockage using linkid
    public static String findBlockageNameFromBlockedLink(String linkId){
        String blockageName=null;
        for(Map.Entry<String, ArrayList<String>> entry: allBlockagePointsWithLinks.entrySet()){
            String  name=  entry.getKey();
            ArrayList<String> linksList =  entry.getValue();

            if(linksList.contains(linkId)){
                blockageName = name;
                break;
            }
        }

        return blockageName;
    }

    // find location when given blockage name
    public static double[] findAndGetLocationOfBlockage(String name){
        Location loc=null;
        for(Location blockage: allBlockageLocations){
            if(blockage.getName().equals(name)){
                        loc = blockage;
                        break;
            }

        }
        return  loc.getCoordinates();

    }

    public double getLastUpdatedTime() {
        return lastUpdatedTime;
    }

    public boolean isCongestionNearBlockage() {
        return congestionNearBlockage;
    }

    public boolean isBlockageInCurrentDirection() {
        return blockageInCurrentDirection;
    }

    public void setBlockageInCurrentDirection(boolean blockageInCurrentDirection) {
        this.blockageInCurrentDirection = blockageInCurrentDirection;
    }

    public void setCongestionNearBlockage(boolean congestionNearBlockage) {
        this.congestionNearBlockage = congestionNearBlockage;
    }


    public recency getRecencyOfBlockage() {
        return recencyOfBlockage;
    }

    public void setRecencyOfBlockage(recency recencyOfBlockage) {
        this.recencyOfBlockage = recencyOfBlockage;
    }

    public double getDistToBlockage() {
        return distToBlockage;
    }

    public void setDistToBlockage(double distToBlockage) {
        this.distToBlockage = distToBlockage;
    }


    public double getLatestObservedTime() {

        return latestObservedTime;
    }

    public void setLatestBlockageObservedTime(double time) {
           this.latestObservedTime = time;
    }

    public double getLatestInfoReceivedTime() {
        return latestInfoReceivedTime;
    }

    public void setLatestInfoReceivedTime(double latestInfoReceivedTime) {
        this.latestInfoReceivedTime = latestInfoReceivedTime;
    }

    public void setLastUpdatedTime(double lastUpdatedTime) {

        this.lastUpdatedTime = lastUpdatedTime;

    }

    public double getReconsiderTime() {
        return reconsiderTime;
    }

    public void setReconsiderTime(double reconsiderTime) {
        this.reconsiderTime = reconsiderTime;
    }

    public boolean isNoBlockageImpact() {
        return noBlockageImpact;
    }

    public void setNoBlockageImpact(boolean noBlockageImpact) {
        this.noBlockageImpact = noBlockageImpact;
    }





}
