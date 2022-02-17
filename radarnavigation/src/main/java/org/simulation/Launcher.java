package org.simulation;

import java.io.IOException;

import org.simulation.radarnavigation.RadarNavigation;
import org.simulation.shipserver.ShipManager;

public class Launcher {

	public static void main(String[] args) throws IOException {
		ShipManager.main(args);
		
		RadarNavigation.main(args);
	}
}





