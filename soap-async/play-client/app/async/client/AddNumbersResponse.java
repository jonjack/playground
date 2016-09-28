
package async.client;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for addNumbersResponse complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="addNumbersResponse">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="return" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "addNumbersResponse", propOrder = {
    "_return"
})
public class AddNumbersResponse {

    @XmlElement(name = "return")
    protected int _return;

    /**
     * Gets the value of the return property.
     * 
     */
    public int getReturn() {
    	long threadId = Thread.currentThread().getId();
    	System.out.println("In AddNumbersResponse. Getting return value in thread ID " + threadId);
    	return (int) threadId;
        //return _return;
    }

    /**
     * Sets the value of the return property.
     * 
     */
   // public void setReturn(int value) {
    	
    	//long threadId = Thread.currentThread().getId();
    	//
    	//System.out.println("In AddNumbersResponse. Computed int = " + value);
    	//System.out.println("In AddNumbersResponse, setting return value in thread ID " + threadId);
        //this._return = value;
        //this._return = (int) threadId;
   // }

}
