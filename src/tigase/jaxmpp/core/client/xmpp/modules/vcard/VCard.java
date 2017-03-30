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
package tigase.jaxmpp.core.client.xmpp.modules.vcard;

import java.io.Serializable;
import java.util.List;

import tigase.jaxmpp.core.client.xml.DefaultElement;
import tigase.jaxmpp.core.client.xml.Element;
import tigase.jaxmpp.core.client.xml.XMLException;

public class VCard implements Serializable {

	private static final long serialVersionUID = 1L;

	private static void add(Element vcard, String name, String value)
			throws XMLException {
		if (value != null)
			vcard.addChild(new DefaultElement(name, value, null));
	}

	private static void add(Element vcard, String name, String[] childNames,
			String[] values) throws XMLException {
		Element x = new DefaultElement(name);
		vcard.addChild(x);

		for (int i = 0; i < childNames.length; i++) {
			x.addChild(new DefaultElement(childNames[i], values[i], null));
		}

	}

	private static boolean match(final Element it, final String elemName,
			final String... children) throws XMLException {
		if (!elemName.equals(it.getName()))
			return false;

		for (String string : children) {
			List<Element> l = it.getChildren(string);
			if (l == null || l.size() == 0)
				return false;
		}

		return true;
	}

	private String description;
	private String fullName;
	private String bday;
	private String gender;
	private String homeAddressLocality;
	private String nickName;
	private String jabberID;
	private String relationshipStatus;

	public String getDescription() {
		return description;
	}

	public String getFullName() {
		return fullName;
	}

	public String getBday() {
		return bday;
	}

	public String getGender() {
		return gender;
	}

	public String getNickName() {
		return nickName;
	}

	public String getHomeAddressLocality() {
		return homeAddressLocality;
	}

	public String getRelationshipStatus() {
		return relationshipStatus;
	}

	public String getJabberID() {
		return jabberID;
	}

	void loadData(final Element element) throws XMLException {
		if (!element.getName().equals("vCard")
				|| !element.getXMLNS().equals("vcard-temp"))
			throw new RuntimeException(
					"Element isn't correct <vCard xmlns='vcard-temp'> vcard element");
		for (final Element it : element.getChildren()) {
			if (match(it, "ADR", "LOCALITY")) {
				for (Element e : it.getChildren()) {
					if ("LOCALITY".equals(e.getName())) {
						this.homeAddressLocality = e.getValue();
					}
				}
			} else if ("FN".equals(it.getName())) {
				this.fullName = it.getValue();
			} else if ("RS".equals(it.getName())) {
				this.relationshipStatus = it.getValue();
			} else if ("NICKNAME".equals(it.getName())) {
				this.nickName = it.getValue();
			} else if ("BDAY".equals(it.getName())) {
				this.bday = it.getValue();
			} else if ("GENDER".equals(it.getName())) {
				this.gender = it.getValue();
			} else if ("DESC".equals(it.getName())) {
				this.description = it.getValue();
			} else if ("JIJIID".equals(it.getName())) {
				this.jabberID = it.getValue();
			}

		}
	}

	public Element makeElement() throws XMLException {
		Element vcard = new DefaultElement("vCard", null, "vcard-temp");
		add(vcard, "FN", this.fullName);
		add(vcard, "NICKNAME", this.nickName);
		add(vcard, "BDAY", this.bday);
		add(vcard, "GENDER", this.gender);

		add(vcard, "ADR", new String[] { "LOCALITY" },
				new String[] { this.homeAddressLocality });

		add(vcard, "DESC", this.description);

		add(vcard, "RS", this.relationshipStatus);
		add(vcard, "JIJIID", this.jabberID);

		return vcard;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public void setBday(String bday) {
		this.bday = bday;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	public void setHomeAddressLocality(String homeAddressLocality) {
		this.homeAddressLocality = homeAddressLocality;
	}

	public void setNickName(String nickName) {
		this.nickName = nickName;
	}

	public void setRelationshipStatus(String relationshipStatus) {
		this.relationshipStatus = relationshipStatus;
	}

	public void setJabberID(String jabberID) {
		this.jabberID = jabberID;
	}
}