/*
 * Copyright 2004,2005 The Apache Software Foundation.
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

package org.apache.rampart;

import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.rampart.policy.RampartPolicyData;
import org.apache.rampart.util.Axis2Util;
import org.apache.rampart.util.RampartUtil;
import org.apache.ws.secpolicy.WSSPolicyException;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityEngine;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.message.token.Timestamp;
import org.apache.ws.security.util.WSSecurityUtil;

import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

public class RampartEngine {


    public Vector process(MessageContext msgCtx) throws WSSPolicyException,
    RampartException, WSSecurityException, AxisFault {
        
        RampartMessageData rmd = new RampartMessageData(msgCtx, false);
        RampartPolicyData rpd = rmd.getPolicyData();
        if(rpd == null) {
            SOAPEnvelope env = Axis2Util.getSOAPEnvelopeFromDOOMDocument(rmd.getDocument());

            //Convert back to llom since the inflow cannot use llom
            msgCtx.setEnvelope(env);
            Axis2Util.useDOOM(false);
            return null;
        }
        Vector results = null;
        
        WSSecurityEngine engine = new WSSecurityEngine();
        
        ValidatorData data = new ValidatorData(rmd);
        
        if(rpd.isSymmetricBinding()) {
            //Here we have to create the CB handler to get the tokens from the 
            //token storage
            
            results = engine.processSecurityHeader(rmd.getDocument(), 
                                null, 
                                new TokenCallbackHandler(rmd.getTokenStorage(), RampartUtil.getPasswordCB(rmd)),
                                RampartUtil.getSignatureCrypto(rpd.getRampartConfig(), 
                                        msgCtx.getAxisService().getClassLoader()));
        } else {
            results = engine.processSecurityHeader(rmd.getDocument(),
                      null, 
                      new TokenCallbackHandler(rmd.getTokenStorage(), RampartUtil.getPasswordCB(rmd)),
                      RampartUtil.getSignatureCrypto(rpd.getRampartConfig(), 
                              msgCtx.getAxisService().getClassLoader()), 
                      RampartUtil.getEncryptionCrypto(rpd.getRampartConfig(), 
                              msgCtx.getAxisService().getClassLoader()));
        }
        

        SOAPEnvelope env = Axis2Util.getSOAPEnvelopeFromDOOMDocument(rmd.getDocument());

        //Convert back to llom since the inflow cannot use DOOM
        msgCtx.setEnvelope(env);
        Axis2Util.useDOOM(false);

        PolicyBasedResultsValidator validator = new PolicyBasedResultsValidator();
        validator.validate(data, results);
        
        return results;
    }



    

}
