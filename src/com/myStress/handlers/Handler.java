package com.myStress.handlers;
/**
 * Interface for sensor handlers
 */
public interface Handler 
{
	/**
	 * Method to acquire sensor symbol
	 * @param sensor String of the sensor symbol to be acquired
	 * @param query String of the query to be executed
	 */
    public byte[] Acquire(String sensor, String query);
    /**
     * Method to discover sensor symbols supported by a handler 
     */
    public void   Discover();
    /**
     * Method to destroy resources of handler
     */
    public void   destroyHandler();
    /**
     * Method to share the current value of the given sensor
     * @param sensor String of the sensor to be shared
     * @return human-readable String representation of last value
     */
    public String Share(String sensor);
    /**
     * Method to provide a historical view of the given sensor
     * @param sensor String ot the sensor to be shown
     */
    public void History(String sensor);
}
