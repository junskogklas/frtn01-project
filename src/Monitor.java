package src;

/** 
	Class that ensures synchronized getting and setting of variables used in
	the distance control
*/
public class Monitor {

	// Instance variables
	private double dist;
	private double desiredDist = 20.0; // cm
	private double tolerance = 5.0; // cm

	public Monitor() {}

	public synchronized double getDist() {
		return dist;
	}
	public synchronized void setDist(double d) {
		dist = d;
	}
	public synchronized double getDesiredDist() {
		return desiredDist;
	}
	public synchronized void setDesiredDist(double newDesiredDist){
		desiredDist = newDesiredDist;
	}

	// This class also contains a method to calculate the approriate signal to be sent
	// to the MinSeg, depending on the MinSegs' relative positions to each other
	public synchronized char calculateOutPut() {
		if (dist > desiredDist + tolerance) {
			return '+';
		} else if (dist < desiredDist - tolerance) {
			return '-';
		}
		return '0'; 
	}
}

