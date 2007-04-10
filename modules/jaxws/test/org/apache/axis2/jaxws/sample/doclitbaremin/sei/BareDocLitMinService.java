
package org.apache.axis2.jaxws.sample.doclitbaremin.sei;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;

/**
 * This class was generated by the JAXWS SI.
 * JAX-WS RI 2.0_01-b15-fcs
 * Generated source version: 2.0
 * 
 */
@WebServiceClient(name = "BareDocLitMinService", targetNamespace = "http://org.test.sample.doclitbaremin", wsdlLocation = "doclitbaremin.wsdl")
public class BareDocLitMinService
    extends Service
{

    private final static URL BAREDOCLITMINSERVICE_WSDL_LOCATION;

    private static String wsdlLocation="/test/org/apache/axis2/jaxws/sample/doclitbaremin/META-INF/doclitbaremin.wsdl";
    static {
        URL url = null;
        try {
        	try{
	        	String baseDir = new File(System.getProperty("basedir",".")).getCanonicalPath();
	        	wsdlLocation = new File(baseDir + wsdlLocation).getAbsolutePath();
        	}catch(Exception e){
        		e.printStackTrace();
        	}
        	File file = new File(wsdlLocation);
        	url = file.toURL();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        BAREDOCLITMINSERVICE_WSDL_LOCATION = url;
    }

    public BareDocLitMinService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public BareDocLitMinService() {
        super(BAREDOCLITMINSERVICE_WSDL_LOCATION, new QName("http://doclitbaremin.sample.test.org", "BareDocLitMinService"));
    }

    /**
     * 
     * @return
     *     returns DocLitBarePortType
     */
    @WebEndpoint(name = "BareDocLitMinPort")
    public DocLitBareMinPortType getBareDocLitMinPort() {
        return (DocLitBareMinPortType)super.getPort(new QName("http://doclitbaremin.sample.test.org", "BareDocLitMinPort"), DocLitBareMinPortType.class);
    }

}
