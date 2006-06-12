/*
 * Copyright  1999-2004 The Apache Software Foundation.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.savan.eventing;


import java.util.Calendar;
import java.util.Date;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.databinding.types.Duration;
import org.apache.axis2.databinding.utils.ConverterUtil;
import org.apache.axis2.util.UUIDGenerator;
import org.apache.savan.SavanException;
import org.apache.savan.SavanMessageContext;
import org.apache.savan.subscribers.Subscriber;
import org.apache.savan.subscription.ExpirationBean;
import org.apache.savan.subscription.SubscriptionProcessor;
import org.apache.savan.util.CommonUtil;

public class EventingSubscriptionProcessor extends SubscriptionProcessor {

	public void init (SavanMessageContext smc) throws SavanException {
		//setting the subscriber_id as a property if possible.
		
		String id = getSubscriberID(smc);
		if (id!=null) {
			smc.setProperty(EventingConstants.TransferedProperties.SUBSCRIBER_UUID,id);
		}
		
	}
	
	public Subscriber getSubscriberFromMessage(SavanMessageContext smc) throws SavanException {

		SOAPEnvelope envelope = smc.getEnvelope();
		if (envelope==null)
			return null;
		
		EventingSubscriber subscriber = new EventingSubscriber ();  //eventing only works on leaf subscriber for now.
		
		String id = UUIDGenerator.getUUID();
		smc.setProperty(EventingConstants.TransferedProperties.SUBSCRIBER_UUID,id);
	
		subscriber.setId(id);
		
		SOAPBody body = envelope.getBody();
		OMElement subscribeElement = body.getFirstChildWithName(new QName (EventingConstants.EVENTING_NAMESPACE,EventingConstants.ElementNames.Subscribe));
		if (subscribeElement==null)
			throw new SavanException ("'Subscribe' element is not present");
		
		OMElement endToElement = subscribeElement.getFirstChildWithName(new QName (EventingConstants.EVENTING_NAMESPACE,EventingConstants.ElementNames.EndTo));
		if (endToElement!=null) {
			EndpointReference endToEPR = new EndpointReference ("");
			endToEPR.fromOM(endToElement);
			
			subscriber.setEndToEPr(endToEPR);
		}
		
		OMElement deliveryElement = subscribeElement.getFirstChildWithName(new QName (EventingConstants.EVENTING_NAMESPACE,EventingConstants.ElementNames.Delivery));
		if (deliveryElement==null)
			throw new SavanException ("Delivery element is not present");
		
		OMElement notifyToElement = deliveryElement.getFirstChildWithName(new QName (EventingConstants.EVENTING_NAMESPACE,EventingConstants.ElementNames.NotifyTo));
		if (notifyToElement==null)
			throw new SavanException ("NotifyTo element is null");
		
		EndpointReference notifyToEPr = new EndpointReference ("");
		notifyToEPr.fromOM(notifyToElement);
		
		OMAttribute deliveryModeAttr = deliveryElement.getAttribute(new QName (EventingConstants.ElementNames.Mode));
		String deliveryMode = null;
		if (deliveryModeAttr!=null) {
			deliveryMode = deliveryModeAttr.getAttributeValue().trim();
		} else {
			deliveryMode = EventingConstants.DEFAULT_DELIVERY_MODE;
		}
		
		Delivery delivery = new Delivery ();
		delivery.setDeliveryEPR(notifyToEPr);
		delivery.setDeliveryMode(deliveryMode);
		
		subscriber.setDelivery(delivery);
		
		OMElement expiresElement = subscribeElement.getFirstChildWithName(new QName (EventingConstants.EVENTING_NAMESPACE,EventingConstants.ElementNames.Expires));
		if (expiresElement!=null) {
			String expiresText = expiresElement.getText();

			if (expiresText==null){
				String message = "Expires Text is null";
				throw new SavanException (message);
			}
			
			expiresText = expiresText.trim();
			
			ExpirationBean expirationBean = getExpirationBeanFromString(expiresText);
			Date expiration = null;
			if (expirationBean.isDuration()) {
				Calendar calendar = Calendar.getInstance();
				CommonUtil.addDurationToCalendar(calendar,expirationBean.getDurationValue());
				expiration = calendar.getTime();
			} else
				expiration = expirationBean.getDateValue();
			
			
			if (expiration==null) {
				String message = "Cannot understand the given date-time value for the Expiration";
				throw new SavanException (message);
			}
			
			subscriber.setSubscriptionEndingTime(expiration);
		}
		
		OMElement filterElement = subscribeElement.getFirstChildWithName(new QName (EventingConstants.EVENTING_NAMESPACE,EventingConstants.ElementNames.Filter));
		if (filterElement!=null) {
			Filter filter = new Filter ();
			
			OMNode filterNode = filterElement.getFirstOMChild();
			filter.setFilter(filterNode);
			
			OMAttribute dialectAttr = filterElement.getAttribute(new QName (EventingConstants.ElementNames.Dialect));
			
			if (dialectAttr!=null) {
				String dilect = dialectAttr.getAttributeValue().trim();
				filter.setFilterType(dilect);
			} else {
				//setting the default finter dialect.
				filter.setFilterType(EventingConstants.DEFAULT_FILTER_DIALECT);
			}
			
			subscriber.setFilter(filter);
		}
		
		return subscriber;
	}

	public void pauseSubscription(SavanMessageContext pauseSubscriptionMessage) throws SavanException {
		throw new UnsupportedOperationException ("Eventing specification does not support this type of messages");
	}

	public void resumeSubscription(SavanMessageContext resumeSubscriptionMessage) throws SavanException {
		throw new UnsupportedOperationException ("Eventing specification does not support this type of messages");
	}

	public ExpirationBean getExpirationBean(SavanMessageContext renewMessage) throws SavanException {

		SOAPEnvelope envelope = renewMessage.getEnvelope();
		SOAPBody body = envelope.getBody();
		
		ExpirationBean expirationBean = null;
		
		OMElement renewElement = body.getFirstChildWithName(new QName (EventingConstants.EVENTING_NAMESPACE,EventingConstants.ElementNames.Renew));
		if (renewElement==null) {
			String message = "Renew element not present in the assumed Renew Message";
			throw new SavanException (message);
		}
		
		OMElement expiresElement = renewElement.getFirstChildWithName(new QName (EventingConstants.EVENTING_NAMESPACE,EventingConstants.ElementNames.Expires));
		if (expiresElement!=null) {
			String expiresText = expiresElement.getText().trim();
			expirationBean = getExpirationBeanFromString(expiresText);
		}
		
		String subscriberID = getSubscriberID(renewMessage);
		if (subscriberID==null) {
			String message = "Cannot find the subscriber ID";
			throw new SavanException (message);
		}
		
		renewMessage.setProperty(EventingConstants.TransferedProperties.SUBSCRIBER_UUID,subscriberID);
		
		expirationBean.setSubscriberID(subscriberID);
		return expirationBean;
	}

	public String getSubscriberID(SavanMessageContext smc) throws SavanException {
		SOAPEnvelope envelope = smc.getEnvelope();
		SOAPHeader header = envelope.getHeader();
		if (header==null) {
			return null;
		}
		
		OMElement ideltifierElement = envelope.getHeader().getFirstChildWithName(new QName (EventingConstants.EVENTING_NAMESPACE,EventingConstants.ElementNames.Identifier));
		if (ideltifierElement==null) {
			return null;
		}
		
		String identifier = ideltifierElement.getText().trim();
		return identifier;
	}
	
	private ExpirationBean getExpirationBeanFromString (String expiresStr) throws SavanException {

		ExpirationBean bean = new ExpirationBean ();
		
		//expires can be a duration or a date time.
		//Doing the conversion using the ConverUtil helper class.
		
		Date date = null;
		boolean isDuration = CommonUtil.isDuration(expiresStr);
		
		if (isDuration) {
			try {
				bean.setDuration(true);
				Duration duration = ConverterUtil.convertToDuration(expiresStr);
				bean.setDurationValue(duration);
			} catch (IllegalArgumentException e) {
				String message = "Cannot convert the Expiration value to a valid duration";
				throw new SavanException (message,e);
			}
		} else {
			try {
			    Calendar calendar = ConverterUtil.convertTodateTime(expiresStr);
			    date = calendar.getTime();
			    bean.setDateValue(date);
			} catch (Exception e) {
				String message = "Cannot convert the Expiration value to a valid DATE/TIME";
				throw new SavanException (message,e);
			}
		}
		
		return bean;
	}
	
	

}
