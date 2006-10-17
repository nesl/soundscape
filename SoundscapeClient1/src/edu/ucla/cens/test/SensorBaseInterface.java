/**
 * This interface represents something that can be instantiated to and from an XML snippet, 
 * representing a row in a SensorBase table.
 */
package edu.ucla.cens.test;

/**
 * @author Andrew
 *
 */
public interface SensorBaseInterface {
	/**
	 * Converts the given object to an XML snippet, containing <row>...</row>
	 * @return this object as an XML String.
	 */
	public String toXML();

}
