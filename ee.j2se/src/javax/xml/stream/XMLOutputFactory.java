/*
 * Copyright (c) OSGi Alliance (2001, 2009). All Rights Reserved.
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

package javax.xml.stream;
public abstract class XMLOutputFactory {
	public final static java.lang.String IS_REPAIRING_NAMESPACES = "javax.xml.stream.isRepairingNamespaces";
	protected XMLOutputFactory() { } 
	public abstract javax.xml.stream.XMLEventWriter createXMLEventWriter(java.io.OutputStream var0) throws javax.xml.stream.XMLStreamException;
	public abstract javax.xml.stream.XMLEventWriter createXMLEventWriter(java.io.OutputStream var0, java.lang.String var1) throws javax.xml.stream.XMLStreamException;
	public abstract javax.xml.stream.XMLEventWriter createXMLEventWriter(java.io.Writer var0) throws javax.xml.stream.XMLStreamException;
	public abstract javax.xml.stream.XMLEventWriter createXMLEventWriter(javax.xml.transform.Result var0) throws javax.xml.stream.XMLStreamException;
	public abstract javax.xml.stream.XMLStreamWriter createXMLStreamWriter(java.io.OutputStream var0) throws javax.xml.stream.XMLStreamException;
	public abstract javax.xml.stream.XMLStreamWriter createXMLStreamWriter(java.io.OutputStream var0, java.lang.String var1) throws javax.xml.stream.XMLStreamException;
	public abstract javax.xml.stream.XMLStreamWriter createXMLStreamWriter(java.io.Writer var0) throws javax.xml.stream.XMLStreamException;
	public abstract javax.xml.stream.XMLStreamWriter createXMLStreamWriter(javax.xml.transform.Result var0) throws javax.xml.stream.XMLStreamException;
	public abstract java.lang.Object getProperty(java.lang.String var0);
	public abstract boolean isPropertySupported(java.lang.String var0);
	public static javax.xml.stream.XMLOutputFactory newInstance() throws javax.xml.stream.FactoryConfigurationError { return null; }
	public static javax.xml.stream.XMLInputFactory newInstance(java.lang.String var0, java.lang.ClassLoader var1) throws javax.xml.stream.FactoryConfigurationError { return null; }
	public abstract void setProperty(java.lang.String var0, java.lang.Object var1);
}
