/*
 * Tigase XMPP Client Library
 * Copyright (C) 2006-2012 "Bartosz Małkowski" <bartosz.malkowski@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */
package tigase.jaxmpp.core.client.xmpp.modules.jingle;

import java.util.ArrayList;
import java.util.List;

import tigase.jaxmpp.core.client.JID;
import tigase.jaxmpp.core.client.PacketWriter;
import tigase.jaxmpp.core.client.SessionObject;
import tigase.jaxmpp.core.client.XMPPException;
import tigase.jaxmpp.core.client.XmppModule;
import tigase.jaxmpp.core.client.criteria.Criteria;
import tigase.jaxmpp.core.client.criteria.ElementCriteria;
import tigase.jaxmpp.core.client.exceptions.JaxmppException;
import tigase.jaxmpp.core.client.observer.BaseEvent;
import tigase.jaxmpp.core.client.observer.EventType;
import tigase.jaxmpp.core.client.observer.Observable;
import tigase.jaxmpp.core.client.xml.DefaultElement;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;
import tigase.jaxmpp.core.client.xmpp.modules.ObservableAware;
import tigase.jaxmpp.core.client.xmpp.modules.PacketWriterAware;
import tigase.jaxmpp.core.client.xmpp.modules.ResourceBinderModule;
import tigase.jaxmpp.core.client.xmpp.stanzas.IQ;
import tigase.jaxmpp.core.client.xmpp.stanzas.StanzaType;

public class JingleModule implements XmppModule, PacketWriterAware, ObservableAware {

	public static class JingleSessionAcceptEvent extends JingleSessionEvent {

		private static final long serialVersionUID = 1L;

		private final Element description;
		private final List<Transport> transports;

		public JingleSessionAcceptEvent(SessionObject sessionObject, JID sender, String sid, Element description,
				List<Transport> transports) {
			super(JingleSessionAccept, sessionObject, sender, sid);

			this.description = description;
			this.transports = transports;
		}

		public Element getDescription() {
			return description;
		}

		public List<Transport> getTransport() {
			return transports;
		}

	}

	public static class JingleSessionEvent extends BaseEvent {

		private static final long serialVersionUID = 1L;

		private final JID sender;
		private final String sid;

		private boolean handled = false;
		
		public JingleSessionEvent(EventType type, SessionObject sessionObject, JID sender, String sid) {
			super(type, sessionObject);

			this.sender = sender;
			this.sid = sid;
		}

		public JID getSender() {
			return sender;
		}

		public String getSid() {
			return sid;
		}

		public boolean isJingleHandled() {
			return handled;
		}
		
		public void setJingleHandled(boolean value) {
			this.handled = value;
		}
		
	}

	public static class JingleSessionInfoEvent extends JingleSessionEvent {

		private static final long serialVersionUID = 1L;

		private final List<Element> content;

		public JingleSessionInfoEvent(SessionObject sessionObject, JID sender, String sid, List<Element> content) {
			super(JingleTransportInfo, sessionObject, sender, sid);

			this.content = content;
		}

		public List<Element> getContent() {
			return content;
		}

	}

	public static class JingleSessionInitiationEvent extends JingleSessionEvent {

		private static final long serialVersionUID = 1L;

		private final Element description;
		private final List<Transport> transports;

		public JingleSessionInitiationEvent(SessionObject sessionObject, JID sender, String sid, Element description,
				List<Transport> transports) {
			super(JingleSessionInitiation, sessionObject, sender, sid);

			this.description = description;
			this.transports = transports;
		}

		public Element getDescription() {
			return description;
		}

		public List<Transport> getTransports() {
			return transports;
		}

	}

	public static class JingleSessionTerminateEvent extends JingleSessionEvent {

		private static final long serialVersionUID = 1L;

		public JingleSessionTerminateEvent(SessionObject sessionObject, JID sender, String sid) {
			super(JingleSessionTerminate, sessionObject, sender, sid);
		}

	}

	public static class JingleTransportInfoEvent extends JingleSessionEvent {

		private static final long serialVersionUID = 1L;

		private final Element content;

		public JingleTransportInfoEvent(SessionObject sessionObject, JID sender, String sid, Element content) {
			super(JingleTransportInfo, sessionObject, sender, sid);

			this.content = content;
		}

		public Element getContent() {
			return content;
		}

	}

	public static final String JINGLE_RTP1_XMLNS = "urn:xmpp:jingle:apps:rtp:1";

	public static final String JINGLE_XMLNS = "urn:xmpp:jingle:1";

	public static final Criteria CRIT = ElementCriteria.name("iq").add(ElementCriteria.name("jingle", JINGLE_XMLNS));

	public static final String[] FEATURES = { JINGLE_XMLNS, JINGLE_RTP1_XMLNS };

	public static final EventType JingleSessionAccept = new EventType();
	public static final EventType JingleSessionInfo = new EventType();
	public static final EventType JingleSessionInitiation = new EventType();
	public static final EventType JingleSessionTerminate = new EventType();
	public static final EventType JingleTransportInfo = new EventType();

	private final SessionObject sessionObject;
	private PacketWriter writer;
	private Observable observable;
	
	public JingleModule(SessionObject sessionObject) {
		this.sessionObject = sessionObject;
	}

	public void acceptSession(JID jid, String sid, String name, Element description, List<Transport> transports)
			throws JaxmppException {
		IQ iq = IQ.create();

		iq.setTo(jid);
		iq.setType(StanzaType.set);

		Element jingle = new DefaultElement("jingle");
		jingle.setXMLNS(JINGLE_XMLNS);
		jingle.setAttribute("action", "session-accept");
		jingle.setAttribute("sid", sid);

		jingle.setAttribute("initiator", jid.toString());

		JID initiator = sessionObject.getProperty(ResourceBinderModule.BINDED_RESOURCE_JID);
		jingle.setAttribute("responder", initiator.toString());

		iq.addChild(jingle);

		Element content = new DefaultElement("content");
		content.setXMLNS(JINGLE_XMLNS);
		content.setAttribute("creator", "initiator");
		content.setAttribute("name", name);

		jingle.addChild(content);

		content.addChild(description);
		if (transports != null) {
			for (Element transport : transports) {
				content.addChild(transport);
			}
		}

		writer.write(iq);
	}

	@Override
	public Criteria getCriteria() {
		return CRIT;
	}

	@Override
	public String[] getFeatures() {
		return FEATURES;
	}

	@Override
	public void setPacketWriter(PacketWriter packetWriter) {
		writer = packetWriter;
	}

	@Override
	public void setObservable(Observable observable) {
		this.observable = observable;
	}

	
	public void initiateSession(JID jid, String sid, String name, Element description, List<Transport> transports)
			throws JaxmppException {
		IQ iq = IQ.create();

		iq.setTo(jid);
		iq.setType(StanzaType.set);

		Element jingle = new DefaultElement("jingle");
		jingle.setXMLNS(JINGLE_XMLNS);
		jingle.setAttribute("action", "session-initiate");
		jingle.setAttribute("sid", sid);

		JID initiator = sessionObject.getProperty(ResourceBinderModule.BINDED_RESOURCE_JID);
		jingle.setAttribute("initiator", initiator.toString());

		iq.addChild(jingle);

		Element content = new DefaultElement("content");
		content.setXMLNS(JINGLE_XMLNS);
		content.setAttribute("creator", "initiator");
		content.setAttribute("name", name);

		jingle.addChild(content);

		content.addChild(description);
		for (Element transport : transports) {
			content.addChild(transport);
		}

		writer.write(iq);
	}

	@Override
	public void process(Element element) throws XMPPException, XMLException, JaxmppException {
		if ("iq".equals(element.getName())) {
			IQ iq = new IQ(element);
			processIq(iq);
		}
	}	
	
	protected void processIq(IQ iq) throws JaxmppException {
		Element jingle = iq.getChildrenNS("jingle", JINGLE_XMLNS);

		List<Element> contents = jingle.getChildren("content");
		// if (contents == null || contents.isEmpty()) {
		// // no point in parsing this any more
		// return;
		// }

		JID from = iq.getFrom();
		String sid = jingle.getAttribute("sid");

		String action = jingle.getAttribute("action");
		
		JingleSessionEvent event = null;
		
		if ("session-terminate".equals(action)) {
			event = new JingleSessionTerminateEvent(sessionObject, from, sid);
			observable.fireEvent(JingleSessionTerminate, event);
		} else if ("session-info".equals(action)) {
			event = new JingleSessionInfoEvent(sessionObject, from, sid, jingle.getChildren());
			observable.fireEvent(JingleSessionInfo, event);
		} else if ("transport-info".equals(action)) {
			event = new JingleTransportInfoEvent(sessionObject, from, sid, contents.get(0));
			observable.fireEvent(JingleTransportInfo, event);
		} else {
			Element content = contents.get(0);
			List<Element> descriptions = content.getChildren("description");

			Element description = descriptions.get(0);
			List<Element> transportElems = content.getChildren("transport");
			List<Transport> transports = new ArrayList<Transport>();
			for (Element transElem : transportElems) {
				if ("transport".equals(transElem.getName())) {
					transports.add(new Transport(transElem));
				}
			}

			if ("session-initiate".equals(action)) {
				event = new JingleSessionInitiationEvent(sessionObject, from, sid, description, transports);
				observable.fireEvent(JingleSessionInitiation, event);
			} else if ("session-accept".equals(action)) {
				event = new JingleSessionAcceptEvent(sessionObject, from, sid, description, transports);
				observable.fireEvent(JingleSessionAccept, event);
			}
		}

		if (event != null && event.isHandled() && event.isJingleHandled()) {
			// sending result - here should be always ok
			IQ response = IQ.create();
			response.setTo(iq.getFrom());
			response.setId(iq.getId());
			response.setType(StanzaType.result);			
			writer.write(response);
		}
		else {
			throw new XMPPException(XMPPException.ErrorCondition.feature_not_implemented);
		}
	}

	public void terminateSession(JID jid, String sid, JID initiator) throws JaxmppException {
		IQ iq = IQ.create();

		iq.setTo(jid);
		iq.setType(StanzaType.set);

		Element jingle = new DefaultElement("jingle");
		jingle.setXMLNS(JINGLE_XMLNS);
		jingle.setAttribute("action", "session-terminate");
		jingle.setAttribute("sid", sid);

		jingle.setAttribute("initiator", initiator.toString());

		iq.addChild(jingle);

		Element reason = new DefaultElement("result");
		jingle.addChild(reason);

		Element success = new DefaultElement("success");
		reason.addChild(success);

		writer.write(iq);
	}

	public void transportInfo(JID recipient, JID initiator, String sid, Element content) throws JaxmppException {
		IQ iq = IQ.create();

		iq.setTo(recipient);
		iq.setType(StanzaType.set);

		Element jingle = new DefaultElement("jingle");
		jingle.setXMLNS(JINGLE_XMLNS);
		jingle.setAttribute("action", "transport-info");
		jingle.setAttribute("sid", sid);
		jingle.setAttribute("initiator", initiator.toString());

		iq.addChild(jingle);

		jingle.addChild(content);

		writer.write(iq);
	}
}