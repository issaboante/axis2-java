
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.axis2.jaxws.nonanonymous.complextype.sei;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * This class was generated by the JAXWS SI.
 * JAX-WS RI 2.0_01-b15-fcs
 * Generated source version: 2.0
 * 
 */
@WebServiceClient(name = "EchoMessageService", targetNamespace = "http://nonanonymous.complextype.test.org", wsdlLocation = "EchoMessage.wsdl")
public class EchoMessageService
    extends Service
{

    private final static URL ECHOMESSAGESERVICE_WSDL_LOCATION;

    private static String wsdlLocation="/test/org/apache/axis2/jaxws/nonanonymous/complextype/META-INF/EchoMessage.wsdl";
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
        ECHOMESSAGESERVICE_WSDL_LOCATION = url;
    }

    public EchoMessageService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public EchoMessageService() {
        super(ECHOMESSAGESERVICE_WSDL_LOCATION, new QName("http://nonanonymous.complextype.test.org", "EchoMessageService"));
    }

    /**
     * 
     * @return
     *     returns EchoMessagePortType
     */
    @WebEndpoint(name = "EchoMessagePort")
    public EchoMessagePortType getEchoMessagePort() {
        return (EchoMessagePortType)super.getPort(new QName("http://nonanonymous.complextype.test.org", "EchoMessagePort"), EchoMessagePortType.class);
    }

}
