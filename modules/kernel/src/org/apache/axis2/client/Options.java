/*
 * Copyright 2001-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.axis2.client;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.AddressingConstants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.addressing.RelatesTo;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.i18n.Messages;
import org.apache.axis2.transport.TransportListener;
import org.apache.axis2.util.MetaDataEntry;
import org.apache.axis2.util.ObjectStateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.namespace.QName;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Holder for operation client options. This is used by the other classes in
 * this package to configure various aspects of how a client communicates with a
 * service. It exposes a number of predefined properties as part of the API
 * (with specific getXXX and setXXX methods), and also allows for arbitrary
 * named properties to be passed using a properties map with the property name
 * as the key value. Instances of this class can be chained together for
 * property inheritance, so that if a property is not set in one instance it
 * will check its parent for a setting.
 */
public class Options implements Externalizable {

    /*
     * setup for logging
     */
    private static final Log log = LogFactory.getLog(Options.class);

    private static final String myClassName = "Options";

    /**
     * @serial The serialization version ID tracks the version of the class.
     * If a class definition changes, then the serialization/externalization
     * of the class is affected. If a change to the class is made which is
     * not compatible with the serialization/externalization of the class,
     * then the serialization version ID should be updated.
     * Refer to the "serialVer" utility to compute a serialization
     * version ID.
     */
    private static final long serialVersionUID = -8318751890845181507L;

    /**
     * @serial Tracks the revision level of a class to identify changes to the 
     * class definition that are compatible to serialization/externalization.
     * If a class definition changes, then the serialization/externalization
     * of the class is affected. 
     * Refer to the writeExternal() and readExternal() methods.
     */
    // supported revision levels, add a new level to manage compatible changes
    private static final int REVISION_1 = 1;
    // current revision level of this object
    private static final int revisionID = REVISION_1;

    
    /**
     * Default blocking timeout value.
     */
    public static final int DEFAULT_TIMEOUT_MILLISECONDS = 30 * 1000;


    /**
     * @serial parent
     */
    private Options parent;


    /**
     * @serial properties
     */
    private Map properties = new HashMap();


    // ==========================================================================
    //                  Parameters that can be set via Options
    // ==========================================================================

    private String soapVersionURI; // defaults to
    // SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI;

    private Boolean isExceptionToBeThrownOnSOAPFault; // defaults to true;

    private long timeOutInMilliSeconds = -1; // =
    // DEFAULT_TIMEOUT_MILLISECONDS;

    private Boolean useSeparateListener; // defaults to false

    // Addressing specific properties
    private String action;

    private EndpointReference faultTo;

    private EndpointReference from;

    private TransportListener listener;

    private TransportInDescription transportIn;

    private String transportInProtocol;

    private String messageId;

    // Array of RelatesTo objects
    private List relationships;

    private EndpointReference replyTo;

    private ArrayList referenceParameters;

    /**
     * This is used for sending and receiving messages.
     */
    protected TransportOutDescription transportOut;

    private EndpointReference to;

    //To control , session management , default is set to true , if user wants he can set that to true
    // The operation client will manage session using ServiceGroupID if it is there in the response
    private boolean manageSession = false;


    //----------------------------------------------------------------
    // MetaData for data to be restored in activate after readExternal
    //----------------------------------------------------------------
    
    /**
     * Indicates whether this object has been reconstituted
     * and needs to have its object references reconciled
     */
    private transient boolean needsToBeReconciled = false;

    /**
     * The TransportOutDescription metadata will be used during
     * activate to match up with an existing object
     */
    private transient MetaDataEntry metaTransportOut = null;
    
    /**
     * The TransportInDescription metadata will be used during
     * activate to match up with an existing object
     */
    private transient MetaDataEntry metaTransportIn = null;
    
    /**
     * The TransportListener metadata will be used during
     * activate to match up with an existing object, if possible
     */
    private transient MetaDataEntry metaListener = null;
    
    //----------------------------------------------------------------
    // end MetaData section
    //----------------------------------------------------------------



    /**
     * Default constructor
     */
    public Options() {
    }

    /**
     * In normal mode operation, this options will try to fulfil the request
     * from its values. If that is not possible, this options will request those
     * information from its parent.
     *
     * @param parent
     */
    public Options(Options parent) {
        this.parent = parent;
    }

    /**
     * Get WS-Addressing Action / SOAP Action string.
     *
     * @return action
     */
    public String getAction() {
        if (action == null && parent != null) {
            return parent.getAction();
        }
        return action;
    }

    /**
     * Get WS-Addressing FaultTo endpoint reference.
     *
     * @return endpoint
     */
    public EndpointReference getFaultTo() {
        if (faultTo == null && parent != null) {
            return parent.getFaultTo();
        }
        return faultTo;
    }

    /**
     * Set WS-Addressing From endpoint reference.
     *
     * @return endpoint
     */
    public EndpointReference getFrom() {
        if (from == null && parent != null) {
            return parent.getFrom();
        }
        return from;
    }

    /**
     * Get listener used for incoming message.
     *
     * @return listener
     */
    public TransportListener getListener() {
        if (needsToBeReconciled) {
            if (log.isWarnEnabled()) {
                log.warn(myClassName+":getListener(): ****WARNING**** Options.activate(configurationContext) needs to be invoked.");
            }
            //System.out.println(myClassName+":getListener(): ****WARNING**** Options.activate(configurationContext) needs to be invoked.");
        }

        if (listener == null && parent != null) {
            return parent.getListener();
        }
        return listener;
    }

    /**
     * Get transport used for incoming message.
     *
     * @return transport information
     */
    public TransportInDescription getTransportIn() {
        if (needsToBeReconciled) {
            if (log.isWarnEnabled()) {
                log.warn(myClassName+":getTransportIn(): ****WARNING**** Options.activate(configurationContext) needs to be invoked.");
            }
            //System.out.println(myClassName+":getTransportIn(): ****WARNING**** Options.activate(configurationContext) needs to be invoked.");
        }

        if (transportIn == null && parent != null) {
            return parent.getTransportIn();
        }
        return transportIn;
    }

    /**
     * Get transport protocol used for incoming message.
     *
     * @return name protocol name ("http", "tcp", etc.)
     */
    public String getTransportInProtocol() {
        if (transportInProtocol == null && parent != null) {
            return parent.getTransportInProtocol();
        }
        return transportInProtocol;
    }

    /**
     * Get WS-Addressing MessageId.
     *
     * @return uri string
     */
    public String getMessageId() {
        if (messageId == null && parent != null) {
            return parent.getMessageId();
        }

        return messageId;
    }

    /**
     * Get a copy of the general option properties. Because of the way options
     * are stored this does not include properties with specific get/set
     * methods, only the general properties identified by a text string. The
     * returned map merges properties inherited from parent options, if any, to
     * give a complete set of property definitions as seen by users of this
     * options instance. The returned copy is not "live", so changes you make to
     * the copy are not reflected in the actual option settings. However, you
     * can make the modified values take effect with a call to {@link
     * #setProperties(Map)},
     *
     * @return copy of general properties
     */
    public Map getProperties() {
        // make sure that the Options properties exists
        if (this.properties == null) {
            this.properties = new HashMap();
        }

        if (parent == null) {
            return new HashMap(properties);
        } else {
            Map props = parent.getProperties();
            props.putAll(properties);
            return props;
        }
    }

    /**
     * Get named property value.
     *
     * @param key
     * @return the value related to this key. <code>null</code>, if not found.
     */
    public Object getProperty(String key) {
        // make sure that the Options properties exists
        if (this.properties == null) {
            this.properties = new HashMap();
        }

        Object myPropValue = properties.get(key);
        if (myPropValue == null && parent != null) {
            return parent.getProperty(key);
        }
        return myPropValue;
    }

    /**
     * Get WS-Addressing RelatesTo item with a specified type. If there are
     * multiple RelatesTo items defined with the same type, the one returned
     * by this method is arbitrary - if you need to handle this case, you can
     * instead use the {@link #getRelationships()} to retrieve all the items
     * and check for multiple matches.
     *
     * @param type relationship type (URI)
     * @return item of specified type
     */
    public RelatesTo getRelatesTo(String type) {
        if (relationships == null && parent != null) {
            return parent.getRelatesTo(type);
        }
        if (relationships == null) {
            return null;
        }
        for (int i = 0;i < relationships.size(); i++) {
            RelatesTo relatesTo = (RelatesTo) relationships.get(i);
            String relationshipType = relatesTo.getRelationshipType();
            if (relationshipType.equals(type)) {
                return relatesTo;
            }
        }
        return null;
    }

    /**
     * Return a single instance of WS-Addressing RelatesTo that has a relationship
     * type of either "http://www.w3.org/2005/08/addressing/reply" or "wsa:Reply".
     * If no such instance of RelatesTo can be found then return <code>null</code>.
     *
     * @return an instance of {@link RelatesTo}
     */
    public RelatesTo getRelatesTo() {
        if (relationships == null && parent != null) {
            return parent.getRelatesTo();
        }
        if (relationships == null) {
            return null;
        }
        for (int i = 0;i < relationships.size(); i++) {
            RelatesTo relatesTo = (RelatesTo) relationships.get(i);
            String relationshipType = relatesTo.getRelationshipType();
            if (relationshipType.equals(AddressingConstants.Final.WSA_DEFAULT_RELATIONSHIP_TYPE)
                    || relationshipType.equals(AddressingConstants.Submission.WSA_DEFAULT_RELATIONSHIP_TYPE))
            {
                return relatesTo;
            }
        }
        return null;
    }

    /**
     * Get all WS-Addressing RelatesTo items.
     *
     * @return array of items
     */
    public RelatesTo[] getRelationships() {
        if (relationships == null && parent != null) {
            return parent.getRelationships();
        }
        if (relationships == null) {
            return null;
        }
        return (RelatesTo[]) relationships.toArray(new RelatesTo[relationships.size()]);
    }

    /**
     * Set WS-Addressing RelatesTo items.
     *
     * @param list
     */
    public void setRelationships(RelatesTo[] list) {
        relationships = list == null ? null : Arrays.asList(list);
    }

    /**
     * Get WS-Addressing ReplyTo endpoint reference.
     *
     * @return endpoint
     */
    public EndpointReference getReplyTo() {
        if (replyTo == null && parent != null) {
            return parent.getReplyTo();
        }
        return replyTo;
    }

    /**
     * Get outbound transport description.
     *
     * @return description
     */
    public TransportOutDescription getTransportOut() {
        if (needsToBeReconciled) {
            if (log.isWarnEnabled()) {
                log.warn(myClassName+":getTransportOut(): ****WARNING**** Options.activate(configurationContext) needs to be invoked.");
            }
            //System.out.println(myClassName+":getTransportOut(): ****WARNING**** Options.activate(configurationContext) needs to be invoked.");
        }

        if (transportOut == null && parent != null) {
            return parent.getTransportOut();
        }

        return transportOut;
    }

    /**
     * Get SOAP version being used.
     *
     * @return version
     */
    public String getSoapVersionURI() {
        if (soapVersionURI == null && parent != null) {
            return parent.getSoapVersionURI();
        }

        return soapVersionURI == null ? SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI
                : soapVersionURI;
    }

    /**
     * Gets the wait time after which a client times out in a blocking scenario.
     * The default is Options#DEFAULT_TIMEOUT_MILLISECONDS
     *
     * @return timeOutInMilliSeconds
     */
    public long getTimeOutInMilliSeconds() {
        if (timeOutInMilliSeconds == -1 && parent != null) {
            return parent.getTimeOutInMilliSeconds();
        }

        return timeOutInMilliSeconds == -1 ? DEFAULT_TIMEOUT_MILLISECONDS
                : timeOutInMilliSeconds;
    }

    /**
     * Get WS-Addressing To endpoint reference.
     *
     * @return endpoint
     */
    public EndpointReference getTo() {
        if (to == null && parent != null) {
            return parent.getTo();
        }

        return to;
    }

    /**
     * If there is a SOAP Fault in the body of the incoming SOAP Message, system
     * can be configured to throw an exception with the details extracted from
     * the information from the fault message. This boolean variable will enable
     * that facility. If this is false, the response message will just be
     * returned to the application, irrespective of whether it has a Fault or
     * not.
     *
     * @return <code>true</code> if exception to be thrown
     */
    public boolean isExceptionToBeThrownOnSOAPFault() {
        if (isExceptionToBeThrownOnSOAPFault == null && parent != null) {
            isExceptionToBeThrownOnSOAPFault = parent.isExceptionToBeThrownOnSOAPFault;
        }

        return isExceptionToBeThrownOnSOAPFault == null
                || isExceptionToBeThrownOnSOAPFault.booleanValue();
    }

    /**
     * Check whether the two SOAP Messages are be sent over same channel or over
     * separate channels. Only duplex transports such as http and tcp support a
     * <code>false</code> value.
     *
     * @return separate channel flag
     */
    public boolean isUseSeparateListener() {
        if (useSeparateListener == null && parent != null) {
            useSeparateListener = parent.useSeparateListener;
        }

        return useSeparateListener != null
                && useSeparateListener.booleanValue();
    }

    /**
     * Get parent instance providing default property values.
     *
     * @return parent (<code>null</code> if none)
     */
    public Options getParent() {
        return parent;
    }

    /**
     * Set parent instance providing default property values.
     *
     * @param parent (<code>null</code> if none)
     */
    public void setParent(Options parent) {
        this.parent = parent;
    }

    /**
     * Set WS-Addressing Action / SOAP Action string.
     *
     * @param action
     */
    public void setAction(String action) {
        this.action = action;
    }

    /**
     * If there is a SOAP Fault in the body of the incoming SOAP Message, system
     * can be configured to throw an exception with the details extracted from
     * the information from the fault message. This boolean variable will enable
     * that facility. If this is false, the response message will just be
     * returned to the application, irrespective of whether it has a Fault or
     * not.
     *
     * @param exceptionToBeThrownOnSOAPFault
     */
    public void setExceptionToBeThrownOnSOAPFault(
            boolean exceptionToBeThrownOnSOAPFault) {
        isExceptionToBeThrownOnSOAPFault = Boolean
                .valueOf(exceptionToBeThrownOnSOAPFault);
    }

    /**
     * Set WS-Addressing FaultTo endpoint reference.
     *
     * @param faultTo endpoint
     */
    public void setFaultTo(EndpointReference faultTo) {
        this.faultTo = faultTo;
    }

    /**
     * Set WS-Addressing From endpoint reference.
     *
     * @param from endpoint
     */
    public void setFrom(EndpointReference from) {
        this.from = from;
    }

    /**
     * Set listener used for incoming message.
     *
     * @param listener
     */
    public void setListener(TransportListener listener) {
        this.listener = listener;
    }

    /**
     * Set transport used for incoming message.
     *
     * @param transportIn
     */
    public void setTransportIn(TransportInDescription transportIn) {
        this.transportIn = transportIn;
    }

    /**
     * Set transport protocol used for incoming message.
     *
     * @param transportInProtocol ("http", "tcp", etc.)
     */
    public void setTransportInProtocol(String transportInProtocol) {
        this.transportInProtocol = transportInProtocol;
    }

    /**
     * Set WS-Addressing MessageId.
     *
     * @param messageId URI string
     */
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    /**
     * Set the general property definitions. Due to the way properties are
     * stored, this will not effect the values of predefined properties with
     * specific get/set methods.
     *
     * @param properties
     */
    public void setProperties(Map properties) {
        this.properties = properties;
    }

    /**
     * General properties you need to pass in to the message context must be set
     * via this method. This method can only be used for properties which do not
     * have specific get/set methods.
     * <p/>
     * Here are some of the properties supported in Axis2.
     * <p/>
     * <a name="GenConst"></a></p>
     * <h3>Generic Constants</h3>
     * <ul>
     * <a name="TRANSPORT_URL"></a></p>
     * <p/>
     * <li><strong>org.apache.axis2.Constants.Configuration.TRANSPORT_URL</strong>
     * <p>Sometimes you want to send your SOAP message through a node, before it reaches to its destination. This means you want to give transport URL different from the URL of the ultimate destination. A typical example would be wanting to send this SOAP (or REST)message through a transparent proxy or through a message monitoring applet. How can that be done using the ServiceClient API?</p>
     * <pre>
     * options.setTo("http://destination.org");
     * options.setProperty(MessageContextConstants.TRANSPORT_URL, "http://myProxy.org");
     * </pre><p>This will send your SOAP message to "http://myProxy.org", but if WS-Addressing is enabled, wsa:To will contain "http://destination.org" as To address.</p>
     * </li>
     * <p>  <a name="CHARACTER_SET_ENCODING"></a></p>
     * <li><b>org.apache.axis2.Constants.Configuration.CHARACTER_SET_ENCODING</b>
     * <p>This will enable user to set the character set encoding scheme to be used when sending the message. Default is set to "UTF-8"</p></li>
     * <p/>
     * <p><a name="ENABLE_MTOM"></a></p>
     * <li><b>org.apache.axis2.Constants.Configuration.ENABLE_MTOM</b>
     * <p>This will enable/disable MTOM support for outgoing messages.</p>
     * <p>Possible values are: </p>
     * <pre>"true"/"false" or Boolean.TRUE/Boolean.FALSE</pre>
     * </li>
     * </ul>
     * <p><a name="Addressing"></a></p>
     * <h3>WS-Addressing Module Specific Constants</h3>
     * <ul>
     * <p/>
     * <a name="WS_ADDRESSING_VERSION"></a></p>
     * <li><b>org.apache.axis2.addressing.AddressingConstants.WS_ADDRESSING_VERSION</b>
     * <p>This will enable to select one of the two WS-Addressing versions available, if WS-Addressing is engaged.</p>
     * <p>Possible values are:</p>
     * <pre>
     * org.apache.axis2.addressing.AddressingConstants.Final.WSA_NAMESPACE
     * and
     * org.apache.axis2.addressing.AddressingConstants.Submission.WSA_NAMESPACE</pre>
     * </li>
     * <p>  <a name="REPLACE_ADDRESSING_HEADERS"></a></p>
     * <li><b>org.apache.axis2.addressing.AddressingConstants.REPLACE_ADDRESSING_HEADERS</b>
     * <p/>
     * <p>AddressingOutHandler picks up the addressing information from the message context and set them to the outgoing message. But someone may have already put some addressing headers, before the AddressingOutHandler. This flag will notify the handler whether to override them or not.</p>
     * <p>Possible values are: </p>
     * <pre>"true"/"false" or Boolean.TRUE/Boolean.FALSE</pre>
     * </li>
     * <p>  <a name="DISABLE_ADDRESSING_FOR_OUT_MESSAGES"></a></p>
     * <li><b>org.apache.axis2.addressing.AddressingConstants.<br />
     * DISABLE_ADDRESSING_FOR_OUT_MESSAGES</b>
     * <p>If WS-Addressing is engaged globally or some how in effect for this particular invocation, this will disable Axis2 from putting WS-Addressing headers in to the out going SOAP message. (Note that Axis2 will not put addressing headers to the outgoing message, irrespective of the above flag, if the incoming message did not contain addressing headers).</p>
     * <p/>
     * <p>Possible values are:</p>
     * <pre>"true"/"false" or Boolean.TRUE/Boolean.FALSE</pre>
     * </li>
     * </ul>
     * <p><a name="HTTPConstants"></a></p>
     * <h3>HTTP Constants</h3>
     * <ul>
     * <a name="CHUNKED"></a></p>
     * <li><b>org.apache.axis2.transport.http.HTTPConstants.CHUNKED</b>
     * <p>This will enable/disable chunking support. </p>
     * <p/>
     * <p>Possible values are:</p>
     * <pre>"true"/"false" or Boolean.TRUE/Boolean.FALSE</pre>
     * </li>
     * <p><a name="NTLM"></a></p>
     * <li><b>org.apache.axis2.transport.http.HTTPConstants.NTLM_AUTHENTICATION</b>
     * <p>This enables the user to pass in NTLM authentication information, such as host, port, realm, username, password to be used with HTTP transport sender. </p>
     * <p>The value should always be an instance of:  </p>
     * <pre>org.apache.axis2.transport.http.HttpTransportProperties.
     * NTLMAuthentication</pre>
     * </li>
     * <p/>
     * <p><a name="PROXY"></a></p>
     * <li><b>org.apache.axis2.transport.http.HTTPConstants.PROXY</b>
     * <p>This enables the user to pass in proxy information, such as proxy host name, port, domain, username, password to be used with HTTP transport sender. </p>
     * <p>The value should always be an instance of:</p>
     * <pre>org.apache.axis2.transport.http.HttpTransportProperties.ProxyProperties</pre>
     * </li>
     * <p>The value should always be an instance of: </p>
     * <pre>org.apache.axis2.transport.http.HttpTransportProperties.BasicAuthentication</pre>
     * </li>
     * <p><a name="SO_TIMEOUT"></a></p>
     * <li><b>org.apache.axis2.transport.http.HTTPConstants.SO_TIMEOUT</b>
     * <p>This enables the user to pass in socket timeout value as an Integer. If nothing is set, the default value is 60000 milliseconds.</p>
     * </li>
     * <p><a name="CON_TIMEOUT"></a></p>
     * <li><b>org.apache.axis2.transport.http.HTTPConstants.CONNECTION_TIMEOUT</b>
     * <p/>
     * <p>This enables the user to pass in connection timeout value as an Integer. If nothing is set, the default value is 60000 milliseconds.</p>
     * </li>
     * <p><a name="USER_AGENT"></a></p>
     * <li><b>org.apache.axis2.transport.http.HTTPConstants.USER_AGENT</b>
     * <p>This enables the user to set the user agent header in the outgoing HTTP request. Default value is "Axis2"</p>
     * </li>
     * <p><a name="GZIP"></a></p>
     * <li><b>org.apache.axis2.transport.http.HTTPConstants.MC_GZIP_REQUEST</b>
     * <p>If set this will GZip your request and send over to the destination. Before doing this, you must make sure that the receiving end supports GZip compressed streams. <br></p>
     * <p/>
     * <p>Possible values are: </p>
     * <pre>"true"/"false" or Boolean.TRUE/Boolean.FALSE</pre>
     * </li>
     * <p><a name="ACCEPT_GZIP"></a></p>
     * <li><b>org.apache.axis2.transport.http.HTTPConstants.MC_ACCEPT_GZIP</b>
     * <p>Whether or not you send a gzip-ped request, you can choose to receive GZIP back from the server using this flag.</p>
     * <p>Possible values are: </p>
     * <pre>"true"/"false" or Boolean.TRUE/Boolean.FALSE</pre>
     * </li>
     * <p/>
     * <p><a name="COOKIE"></a></p>
     * <li><b>org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING</b>
     * <p>This enables the user to set the cookie string header in the outgoing HTTP request.</p>
     * </li>
     * <p><a name="HTTP_PROTOCOL_VERSION"></a></p>
     * <li><b>org.apache.axis2.transport.http.HTTPConstants.HTTP_PROTOCOL_VERSION</b>
     * <p>This will set the HTTP protocol version to be used in sending the SOAP requests. </p>
     * <p>Possible values are :</p>
     * <pre>
     * <p/>
     * HTTP/1.1 - HTTPConstants.HEADER_PROTOCOL_11
     * HTTP/1.0 - HTTPConstants.HEADER_PROTOCOL_10
     * </pre><p>    Default is to use HTTP/1.1.</li>
     * <p><a name="HTTP_HEADERS"></a></p>
     * <li><b>org.apache.axis2.transport.http.HTTPConstants.HTTP_HEADERS</b>
     * <p>You might sometimes want to send your own custom HTTP headers. You can set an ArrayList filled with </p>
     * <pre>org.apache.commons.httpclient.Header</pre><p> objects using the above property. You must not try to override the Headers the Axis2 engine is setting to the outgoing message.</p>
     * </li>
     * <p><a name="REUSE_HTTP_CLIENT"></a></p>
     * <p/>
     * <li><b>org.apache.axis2.transport.http.HTTPConstants.REUSE_HTTP_CLIENT</b>
     * <p>You might want to use the same HTTPClient instance for multiple invocations. This flag will notify the engine to use the same HTTPClient between invocations.</p>
     * </li>
     * <p><a name="CACHED_HTTP_CLIENT"></a></p>
     * <li><b>org.apache.axis2.transport.http.HTTPConstants.CACHED_HTTP_CLIENT</b>
     * <p>If user had requested to re-use an HTTPClient using the above property, this property can be used to set a custom HTTPClient to be re-used.</p>
     * </li>
     * </ul>
     * <p><a name="REST"></a></p>
     * <p/>
     * <h3>Constants to be used in a REST Invocation</h3>
     * <ul>
     * <a name="ENABLE_REST"></a></p>
     * <li><b>org.apache.axis2.transport.http.Constants.Configuration.ENABLE_REST</b>
     * <p>Enabling REST using the above flag will send your request as a REST invocation. </p>
     * <p>Possible values are: </p>
     * <pre>"true"/"false" or Boolean.TRUE/Boolean.FALSE</pre>
     * </li>
     * <p><a name="HTTP_METHOD"></a></p>
     * <li><b>org.apache.axis2.transport.http.Constants.Configuration.HTTP_METHOD</b>
     * <p/>
     * <p>This will help the user to pick the HTTP method to be used during a REST invocation. </p>
     * <p>Possible values are :</p>
     * <pre>
     * org.apache.axis2.Constants.Configuration.HTTP_METHOD_GET
     * and
     * org.apache.axis2.Constants.Configuration.HTTP_METHOD_POST
     * </pre><p>    Default is to use POST method.</li>
     * <p><a name="CONTENT_TYPE"></a>  </p>
     * <li><b>org.apache.axis2.transport.http.Constants.Configuration.CONTENT_TYPE</b>
     * <p>This will help the user to pick the content type to be used during a REST<br />
     * <p/>
     * invocation. </p>
     * <p>Possible values are :</p>
     * <ul>
     * <li>application/xml                   -<br />
     * <pre>HTTPConstants.MEDIA_TYPE_APPLICATION_XML</pre></li>
     * <li>application/x-www-form-urlencoded -<br />
     * <pre>HTTPConstants.MEDIA_TYPE_X_WWW_FORM</pre></li>
     * <li>text/xml                          -<br />
     * <pre>MEDIA_TYPE_TEXT_XML</pre></li>
     * <p/>
     * <li>multipart/related                 -<br />
     * <pre>MEDIA_TYPE_MULTIPART_RELATED</pre></li>
     * </ul>
     * </li>
     * </ul>
     *
     * @param propertyKey
     * @param property
     */
    public void setProperty(String propertyKey, Object property) {
        // make sure that the Options properties exists
        if (this.properties == null) {
            this.properties = new HashMap();
        }
        properties.put(propertyKey, property);
    }

    /**
     * Add WS-Addressing RelatesTo item.
     *
     * @param relatesTo
     */
    public void addRelatesTo(RelatesTo relatesTo) {
        if (relationships == null) {
            relationships = new ArrayList(5);
        }
        relationships.add(relatesTo);
    }

    /**
     * Set WS-Addressing ReplyTo endpoint.
     *
     * @param replyTo endpoint
     */
    public void setReplyTo(EndpointReference replyTo) {
        this.replyTo = replyTo;
    }

    /**
     * Set transport used for outgoing message.
     *
     * @param transportOut
     */
    public void setTransportOut(TransportOutDescription transportOut) {
        this.transportOut = transportOut;
    }

    /**
     * Set transport used for outgoing message.
     *
     * @param senderTransport   transport name in Axis2 configuration
     *                          ("http", "tcp", etc.)
     * @param axisConfiguration
     * @throws AxisFault if the transport is not found
     */
    public void setSenderTransport(String senderTransport,
                                   AxisConfiguration axisConfiguration) throws AxisFault {
        this.transportOut = axisConfiguration.getTransportOut(new QName(
                senderTransport));

        if (senderTransport == null) {
            throw new AxisFault(Messages.getMessage("unknownTransport",
                    senderTransport));
        }
    }

    /**
     * Set the SOAP version to be used.
     *
     * @param soapVersionURI
     * @see org.apache.axis2.namespace.Constants#URI_SOAP11_ENV
     * @see org.apache.axis2.namespace.Constants#URI_SOAP12_ENV
     */
    public void setSoapVersionURI(String soapVersionURI) {
        this.soapVersionURI = soapVersionURI;
    }

    /**
     * This is used in blocking scenario. Client will time out after waiting
     * this amount of time. The default is 2000 and must be provided in
     * multiples of 100.
     *
     * @param timeOutInMilliSeconds
     */
    public void setTimeOutInMilliSeconds(long timeOutInMilliSeconds) {
        this.timeOutInMilliSeconds = timeOutInMilliSeconds;
    }

    /**
     * Set WS-Addressing To endpoint.
     *
     * @param to endpoint
     */
    public void setTo(EndpointReference to) {
        this.to = to;
    }

    /**
     * Sets transport information to the call. The scenarios supported are as
     * follows: <blockquote>
     * <p/>
     * <pre>
     *  [senderTransport, listenerTransport, useSeparateListener]
     *  http, http, true
     *  http, http, false
     *  http, smtp, true
     *  smtp, http, true
     *  smtp, smtp, true
     *  tcp,  tcp,  true
     *  tcp,  tcp,  false
     *  etc.
     * </pre>
     * <p/>
     * </blockquote>
     *
     * @param senderTransport
     * @param listenerTransport
     * @param useSeparateListener
     * @throws AxisFault
     * @deprecated Use setTransportInProtocol(String) and
     *             useSeparateListener(boolean) instead. You do not need to
     *             setSenderTransportProtocol(String) as sender transport can be
     *             inferred from the to EPR. But still you can
     *             setTransportOut(TransportOutDescription).
     */
    public void setTransportInfo(String senderTransport,
                                 String listenerTransport, boolean useSeparateListener)
            throws AxisFault {

        // here we check for a legal combination, for and example if the
        // sendertransport is http and listener
        // transport is smtp the invocation must using separate transport
        if (!useSeparateListener) {
            boolean isTransportsEqual = senderTransport
                    .equals(listenerTransport);
            boolean isATwoWaytransport = Constants.TRANSPORT_HTTP
                    .equals(senderTransport)
                    || Constants.TRANSPORT_TCP.equals(senderTransport);

            if ((!isTransportsEqual || !isATwoWaytransport)) {
                throw new AxisFault(Messages
                        .getMessage("useSeparateListenerLimited"));
            }
        } else {
            setUseSeparateListener(useSeparateListener);
        }

        setTransportInProtocol(listenerTransport);
    }

    /**
     * Used to specify whether the two SOAP Messages are be sent over same
     * channel or over separate channels. The value of this variable depends on
     * the transport specified. For e.g., if the transports are different this
     * is true by default. HTTP transport supports both cases while SMTP
     * transport supports only two channel case.
     *
     * @param useSeparateListener
     */
    public void setUseSeparateListener(boolean useSeparateListener) {
        this.useSeparateListener = Boolean.valueOf(useSeparateListener);
    }

    /**
     * Add WS-Addressing ReferenceParameter child element. Multiple child
     * may be used.
     * TODO Add get method, implement handling.
     *
     * @param referenceParameter
     * @deprecated
     */
    public void addReferenceParameter(OMElement referenceParameter) {
        if (referenceParameters == null) {
            referenceParameters = new ArrayList(5);
        }

        referenceParameters.add(referenceParameter);
    }

    /**
     * Check if session management is enabled.
     *
     * @return <code>true</code> if enabled
     */
    public boolean isManageSession() {
        return manageSession;
    }

    /**
     * Set session management enabled state. When session management is enabled,
     * the engine will automatically send session data (such as the service
     * group id, or HTTP cookies) as part of requests.
     *
     * @param manageSession <code>true</code> if enabling sessions
     */
    public void setManageSession(boolean manageSession) {
        this.manageSession = manageSession;
    }



    /* ===============================================================
     * Externalizable support 
     * ===============================================================
     */
    
    /**
     * Save the contents of this object.
     * <p>
     * NOTE: Transient fields and static fields are not saved.
     *
     * @param out    The stream to write the object contents to
     * 
     * @exception IOException
     */
    public void writeExternal(ObjectOutput out) throws IOException
    {
        // write out contents of this object

        // NOTES: For each item, where appropriate,
        //        write out the following information, IN ORDER:
        //           the class name
        //           the active or empty flag
        //           the data length, if appropriate
        //           the data   

        //---------------------------------------------------------
        // in order to handle future changes to the message 
        // context definition, be sure to maintain the 
        // object level identifiers
        //---------------------------------------------------------
        // serialization version ID
        out.writeLong(serialVersionUID);

        // revision ID
        out.writeInt(revisionID);

        //---------------------------------------------------------
        // various simple fields
        //---------------------------------------------------------
        out.writeLong(timeOutInMilliSeconds);

        out.writeBoolean(manageSession);

        // the following objects could be null
        ObjectStateUtils.writeObject(out, isExceptionToBeThrownOnSOAPFault, "Options.isExceptionToBeThrownOnSOAPFault");
        ObjectStateUtils.writeObject(out, useSeparateListener, "Options.useSeparateListener");


        //---------------------------------------------------------
        // various strings
        //---------------------------------------------------------

        // String soapVersionURI
        ObjectStateUtils.writeString(out, soapVersionURI, "Options.soapVersionURI");

        // String action
        ObjectStateUtils.writeString(out, action, "Options.action");

        // String transportInProtocol
        ObjectStateUtils.writeString(out, transportInProtocol, "Options.transportInProtocol");

        // String messageId
        ObjectStateUtils.writeString(out, messageId, "Options.messageId");


        //---------------------------------------------------------
        // various objects
        //---------------------------------------------------------

        // EndpointReference faultTo
        ObjectStateUtils.writeObject(out, faultTo, "Options.faultTo");

        // EndpointReference from
        ObjectStateUtils.writeObject(out, from, "Options.from");

        // EndpointReference replyTo
        ObjectStateUtils.writeObject(out, replyTo, "Options.replyTo");

        // EndpointReference to
        ObjectStateUtils.writeObject(out, to, "Options.to");


        // TransportListener listener
        if (listener != null)
        {
            metaListener = new MetaDataEntry(listener.getClass().getName(), null);
        }
        else
        {
            metaListener = null;
        }
        ObjectStateUtils.writeObject(out, metaListener, "Options.listener");


        // TransportInDescription transportIn
        if (transportIn != null)
        {
            metaTransportIn = new MetaDataEntry(null, transportIn.getName().toString());
        }
        else
        {
            metaTransportIn = null;
        }
        ObjectStateUtils.writeObject(out, metaTransportIn, "Options.transportIn");


        // TransportOutDescription transportOut
        if (transportOut != null)
        {
            metaTransportOut = new MetaDataEntry(null, transportOut.getName().toString());
        }
        else
        {
            metaTransportOut = null;
        }
        ObjectStateUtils.writeObject(out, metaTransportOut, "Options.transportOut");




        //---------------------------------------------------------
        // collections and lists
        //---------------------------------------------------------

        // List relationships, which is an array of RelatesTo objects 
        ArrayList tmp = null;

        if (relationships != null)
        {
            // make sure this is an array list
            tmp = new ArrayList(relationships);
        }

        ObjectStateUtils.writeArrayList(out, tmp, "Options.relationships");

        // ArrayList referenceParameters
        ObjectStateUtils.writeArrayList(out, referenceParameters, "Options.referenceParameters");

        //---------------------------------------------------------
        // properties
        //---------------------------------------------------------
                                     
        // HashMap properties
        HashMap tmpHM = new HashMap(properties);

        ObjectStateUtils.writeHashMap(out, tmpHM, "Options.properties");


        //---------------------------------------------------------
        // "nested"
        //---------------------------------------------------------

        // Options parent
        ObjectStateUtils.writeObject(out, parent, "Options.parent");


    }


    /**
     * Restore the contents of the MessageContext that was 
     * previously saved. 
     * <p> 
     * NOTE: The field data must read back in the same order and type
     * as it was written.  Some data will need to be validated when 
     * resurrected.
     *
     * @param in    The stream to read the object contents from 
     * 
     * @exception IOException
     * @exception ClassNotFoundException
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        // serialization version ID
        long suid = in.readLong();

        // revision ID
        int  revID = in.readInt();

        // make sure the object data is in a version we can handle
        if (suid != serialVersionUID)
        {
            throw new ClassNotFoundException(ObjectStateUtils.UNSUPPORTED_SUID);
        }

        // make sure the object data is in a revision level we can handle
        if (revID != REVISION_1)
        {
            throw new ClassNotFoundException(ObjectStateUtils.UNSUPPORTED_REVID);
        }


        //---------------------------------------------------------
        // various simple fields
        //---------------------------------------------------------
        timeOutInMilliSeconds = in.readLong();

        manageSession = in.readBoolean();

        // the following objects could be null
        Object tmp1 = ObjectStateUtils.readObject(in, "Options.isExceptionToBeThrownOnSOAPFault"); 
        if (tmp1 != null)
        {
            isExceptionToBeThrownOnSOAPFault = (Boolean) tmp1;
        }

        Object tmp2 = ObjectStateUtils.readObject(in, "Options.useSeparateListener"); 
        if (tmp2 != null)
        {
            useSeparateListener = (Boolean) tmp2;
        }


        //---------------------------------------------------------
        // various strings
        //---------------------------------------------------------

        // String soapVersionURI
        soapVersionURI = ObjectStateUtils.readString(in,"Options.soapVersionURI");

        // String action
        action = ObjectStateUtils.readString(in, "Options.action");

        // String transportInProtocol
        transportInProtocol = ObjectStateUtils.readString(in, "Options.transportInProtocol");

        // String messageId
        messageId = ObjectStateUtils.readString(in, "Options.messageId");


        //---------------------------------------------------------
        // various objects
        //---------------------------------------------------------

        // EndpointReference faultTo
        faultTo = (EndpointReference) ObjectStateUtils.readObject(in, "Options.faultTo");

        // EndpointReference from
        from = (EndpointReference) ObjectStateUtils.readObject(in, "Options.from");

        // EndpointReference replyTo
        replyTo = (EndpointReference) ObjectStateUtils.readObject(in, "Options.replyTo");

        // EndpointReference to
        to = (EndpointReference) ObjectStateUtils.readObject(in, "Options.to");


        // TransportListener listener
        // is not usable until the meta data has been reconciled
        listener = null;
        metaListener = (MetaDataEntry) ObjectStateUtils.readObject(in, "Options.metaListener");

        // TransportInDescription transportIn
        // is not usable until the meta data has been reconciled
        transportIn = null;
        metaTransportIn = (MetaDataEntry) ObjectStateUtils.readObject(in, "Options.metaTransportIn");

        // TransportOutDescription transportOut
        // is not usable until the meta data has been reconciled
        transportOut = null;
        metaTransportOut = (MetaDataEntry) ObjectStateUtils.readObject(in, "Options.metaTransportOut");


        //---------------------------------------------------------
        // collections and lists
        //---------------------------------------------------------

        // List relationships, which is an array of RelatesTo objects 
        ArrayList tmpAL1 = ObjectStateUtils.readArrayList(in, "Options.relationships");
        if (tmpAL1 != null)
        {
            relationships = new ArrayList(tmpAL1);
        }
        else
        {
            relationships = null;
        }

        // ArrayList referenceParameters
        ArrayList tmpAL2 = ObjectStateUtils.readArrayList(in, "Options.referenceParameters");
        if (tmpAL2 != null)
        {
            referenceParameters = new ArrayList(tmpAL2);
        }
        else
        {
            referenceParameters = null;
        }

        //---------------------------------------------------------
        // properties
        //---------------------------------------------------------
                                      
        // HashMap properties
        HashMap tmpHM = ObjectStateUtils.readHashMap(in, "Options.properties"); 

        if (tmpHM != null)
        {
            properties = new HashMap(tmpHM);
        }
        else
        {
            properties = new HashMap();
        }


        //---------------------------------------------------------
        // "nested"
        //---------------------------------------------------------

        // Options parent
        Object tmpParent = ObjectStateUtils.readObject(in, "Options.parent"); 

        if (tmpParent != null)
        {
            parent = (Options) tmpParent;
        }
        else
        {
            parent = null;
        }

        
    }

    /**
     * This method checks to see if additional work needs to be
     * done in order to complete the object reconstitution.
     * Some parts of the object restored from the readExternal()
     * cannot be completed until we have a configurationContext
     * from the active engine. The configurationContext is used
     * to help this object to plug back into the engine's
     * configuration and deployment objects.
     * 
     * @param cc     The configuration context object representing the active configuration
     */
    public void activate(ConfigurationContext cc)
    {
        // see if there's any work to do
        if (needsToBeReconciled == false)
        {
            // return quick
            return;
        }

        // do a trace point
        if (log.isTraceEnabled())
        {
            log.trace(myClassName+":activate():  BEGIN");
        }
        //System.out.println(myClassName+":activate():  BEGIN");


        // use the supplied configuration context

        // get the axis configuration 
        AxisConfiguration axisConfig = cc.getAxisConfiguration();

        // We previously saved metaTransportIn; restore it
        if (metaTransportIn != null)
        {
            QName qin = metaTransportIn.getQName();
            TransportInDescription tmpIn = null;
            try
            {
                tmpIn = axisConfig.getTransportIn(qin);
            }
            catch (Exception exin)
            {
                // if a fault is thrown, log it and continue
                log.warn(myClassName+"activate():  exception caught when getting the TransportInDescription ["+qin.toString()+"]  from the AxisConfiguration ["+exin.getClass().getName()+" : "+exin.getMessage()+"]");

                //System.out.println(myClassName+"activate():  exception caught when getting the TransportInDescription ["+qin.toString()+"]  from the AxisConfiguration ["+exin.getClass().getName()+" : "+exin.getMessage()+"]");
            }

            if (tmpIn != null)
            {
                transportIn = tmpIn;
            }
            else
            {
                if (log.isTraceEnabled())
                {
                    log.trace(myClassName+"activate():  No TransportInDescription found for ["+qin.toString()+"]");
                }

                //System.out.println(myClassName+"activate():  No TransportInDescription found for ["+qin.toString()+"]");

                transportIn = null;
            }
        }
        else
        {
            if (log.isTraceEnabled())
            {
                log.trace(myClassName+"activate():  No TransportInDescription ");
            }

            //System.out.println(myClassName+"activate():  No TransportInDescription ");

            transportIn = null;
        }

        // We previously saved metaTransportOut; restore it
        if (metaTransportOut != null)
        {
            QName qout = metaTransportOut.getQName();
            TransportOutDescription tmpOut = null;
            try
            {
                tmpOut = axisConfig.getTransportOut(qout);
            }
            catch (Exception exout)
            {
                // if a fault is thrown, log it and continue
                log.warn(myClassName+"activate():  exception caught when getting the TransportOutDescription ["+qout.toString()+"]  from the AxisConfiguration ["+exout.getClass().getName()+" : "+exout.getMessage()+"]");

                //System.out.println(myClassName+"activate():  exception caught when getting the TransportOutDescription ["+qout.toString()+"]  from the AxisConfiguration ["+exout.getClass().getName()+" : "+exout.getMessage()+"]");
            }

            if (tmpOut != null)
            {
                transportOut = tmpOut;
            }
            else
            {
                if (log.isTraceEnabled())
                {
                    log.trace(myClassName+"activate():  No TransportOutDescription found for ["+qout.toString()+"]");
                }

                //System.out.println(myClassName+"activate():  No TransportOutDescription found for ["+qout.toString()+"]");


                transportOut = null;
            }
        }
        else
        {
            if (log.isTraceEnabled())
            {
                log.trace(myClassName+"activate():  No TransportOutDescription ");
            }

            //System.out.println(myClassName+"activate():  No TransportOutDescription ");

            transportOut = null;
        }



        // We previously saved metaListener; restore it
        if (metaListener != null)
        {
            // see if we can find an existing object
            String listenerClass = metaListener.getClassName();
            TransportListener tmpListener = ObjectStateUtils.findTransportListener(axisConfig, listenerClass);

            if (log.isTraceEnabled())
            {
                log.trace(myClassName+"activate():  TransportListener found for ["+listenerClass+"] ");
            }

            //System.out.println(myClassName+"activate():  TransportListener found for ["+listenerClass+"] ");

        }
        else
        {
            listener = null;

            if (log.isTraceEnabled())
            {
                log.trace(myClassName+"activate():  No TransportListener ");
            }

            //System.out.println(myClassName+"activate():  No TransportListener ");
        }


        //-------------------------------------------------------
        // done, reset the flag
        //-------------------------------------------------------
        needsToBeReconciled = false;

        if (log.isTraceEnabled())
        {
            log.trace(myClassName+":activate():  END");
        }

        //System.out.println(myClassName+":activate():  END");
    }


    /**
     * Compares key parts of the state from the current instance of 
     * this class with the specified instance to see if they are 
     * equivalent. 
     * <P>
     * This differs from the java.lang.Object.equals() method in
     * that the equals() method generally looks at both the 
     * object identity (location in memory) and the object state
     * (data).
     * <P>
     * 
     * @param  obj  The object to compare with
     * @return TRUE if this object is equivalent with the specified object
     *              that is, key fields match
     *         FALSE, otherwise
     */
    public boolean isEquivalent(Options obj)
    {
        // NOTE: the input object is expected to exist (ie, be non-null)

        if (this.timeOutInMilliSeconds != obj.getTimeOutInMilliSeconds())
        {
            return false;
        }

        if (this.isExceptionToBeThrownOnSOAPFault.booleanValue() != obj.isExceptionToBeThrownOnSOAPFault())
        {
            return false;
        }

        if (this.useSeparateListener.booleanValue() != obj.isUseSeparateListener())
        {
            return false;
        }

        if (this.manageSession != obj.isManageSession())
        {
            return false;
        }

        // --------------------------------------------------------------------

        if ((this.soapVersionURI != null) && (obj.getSoapVersionURI() != null))
        {
            if (!this.soapVersionURI.equals(obj.getSoapVersionURI()))
            {
                return false;
            }
        }
        else if ((this.soapVersionURI == null) && (obj.getSoapVersionURI() == null))
        {
            // continue
        }
        else
        {
            // mismatch
            return false;
        }
        
        // --------------------------------------------------------------------

        if ((this.action != null) && (obj.getAction() != null))
        {
            if (!this.action.equals(obj.getAction()))
            {
                return false;
            }
        }
        else if ((this.action == null) && (obj.getAction() == null))
        {
            // continue
        }
        else
        {
            // mismatch
            return false;
        }
        
        // --------------------------------------------------------------------

        if ((this.transportInProtocol != null) && (obj.getTransportInProtocol() != null))
        {
            if (!this.transportInProtocol.equals(obj.getTransportInProtocol()))
            {
                return false;
            }
        }
        else if ((this.transportInProtocol == null) && (obj.getTransportInProtocol() == null))
        {
            // continue
        }
        else
        {
            // mismatch
            return false;
        }
        
        // --------------------------------------------------------------------

        if ((this.messageId != null) && (obj.getMessageId() != null))
        {
            if (!this.messageId.equals(obj.getMessageId()))
            {
                return false;
            }
        }
        else if ((this.messageId == null) && (obj.getMessageId() == null))
        {
            // continue
        }
        else
        {
            // mismatch
            return false;
        }
        
        // --------------------------------------------------------------------

        if ((this.faultTo != null) && (obj.getFaultTo() != null))
        {
            if (!this.faultTo.isEquivalent(obj.getFaultTo()))
            {
                return false;
            }
        }
        else if ((this.faultTo == null) && (obj.getFaultTo() == null))
        {
            // continue
        }
        else
        {
            // mismatch
            return false;
        }
        
        // --------------------------------------------------------------------

        if ((this.from != null) && (obj.getFrom() != null))
        {
            if (!this.from.isEquivalent(obj.getFrom()))
            {
                return false;
            }
        }
        else if ((this.from == null) && (obj.getFrom() == null))
        {
            // continue
        }
        else
        {
            // mismatch
            return false;
        }
        
        // --------------------------------------------------------------------

        if ((this.replyTo != null) && (obj.getReplyTo() != null))
        {
            if (!this.replyTo.isEquivalent(obj.getReplyTo()))
            {
                return false;
            }
        }
        else if ((this.replyTo == null) && (obj.getReplyTo() == null))
        {
            // continue
        }
        else
        {
            // mismatch
            return false;
        }
        
        // --------------------------------------------------------------------

        if ((this.to != null) && (obj.getTo() != null))
        {
            if (!this.to.isEquivalent(obj.getTo()))
            {
                return false;
            }
        }
        else if ((this.to == null) && (obj.getTo() == null))
        {
            // continue
        }
        else
        {
            // mismatch
            return false;
        }
        
        // --------------------------------------------------------------------

        if ((this.properties != null) && (obj.getProperties() != null))
        {
            if (!this.properties.equals(obj.getProperties()))
            {
                // This is a strict test.
                // Returns true if the given object is also a map 
                // and the two maps represent the same mappings. 
                return false;
            }
        }
        else if ((this.properties == null) && (obj.getProperties() == null))
        {
            // continue
        }
        else
        {
            // mismatch
            return false;
        }
        
        // --------------------------------------------------------------------

        // TODO: consider checking the following objects for equivalency
        //        List relationships;
        //        ArrayList referenceParameters;
        //        TransportListener listener;
        //        TransportInDescription transportIn;
        //        TransportOutDescription transportOut;

        // TODO: consider checking the parent objects for equivalency


        return true;
    }

}
