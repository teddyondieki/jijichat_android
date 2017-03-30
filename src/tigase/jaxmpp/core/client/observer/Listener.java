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
package tigase.jaxmpp.core.client.observer;

import java.util.EventListener;

import tigase.jaxmpp.core.client.exceptions.JaxmppException;

/**
 * Interface for objects that are notified of events.
 * 
 * <pre>
 * moduleManager.getModule(ResourceBinderModule.class).addListener(ResourceBinderModule.ResourceBindSuccess,
 * 		new Listener&lt;ResourceBinderModule.ResourceBindEvent&gt;() {
 * 			public void handleEvent(ResourceBindEvent be) {
 * 				System.out.println(&quot;Binded as &quot; + be.getJid());
 * 			}
 * 		});
 * </pre>
 * 
 * @author bmalkow
 */
public interface Listener<E extends BaseEvent> extends EventListener {

	/**
	 * Execuded when an event happends.
	 * 
	 * @param be
	 *            event
	 */
	public void handleEvent(E be) throws JaxmppException;

}