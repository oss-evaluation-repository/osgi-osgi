/*
 * Copyright (c) OSGi Alliance (2008, 2009). All Rights Reserved.
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
package org.osgi.test.cases.remoteserviceadmin.tb2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import junit.framework.Assert;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.EndpointDescription;
import org.osgi.service.remoteserviceadmin.ExportReference;
import org.osgi.service.remoteserviceadmin.ExportRegistration;
import org.osgi.service.remoteserviceadmin.RemoteConstants;
import org.osgi.service.remoteserviceadmin.RemoteServiceAdmin;
import org.osgi.test.cases.remoteserviceadmin.common.A;
import org.osgi.test.cases.remoteserviceadmin.common.B;
import org.osgi.test.cases.remoteserviceadmin.common.RemoteServiceConstants;

/**
 * @author <a href="mailto:tdiekman@tibco.com">Tim Diekmann</a>
 * @version 1.0.0
 */
public class Activator implements BundleActivator, A, B {
	ServiceRegistration registration;
	BundleContext       context;
	RemoteServiceAdmin  rsa;

	/**
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		this.context = context;
		
		Hashtable<String, String> dictionary = new Hashtable<String, String>();
		dictionary.put("mykey", "will be overridden");
		dictionary.put("myprop", "myvalue");
		dictionary.put(RemoteServiceConstants.SERVICE_EXPORTED_INTERFACES, A.class.getName());

		registration = context.registerService(new String[]{A.class.getName()}, this, dictionary);
		
		test();
	}

	/**
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		registration.unregister();
		
		teststop();
	}

	/**
	 * @see org.osgi.test.cases.remoteserviceadmin.common.A#getA()
	 */
	public String getA() {
		return "this is A";
	}

	/**
	 * @see org.osgi.test.cases.remoteserviceadmin.common.B#getB()
	 */
	public String getB() {
		return "this is B";
	}
	
	public void test() throws Exception {
		// lookup RemoteServiceAdmin service 
		ServiceReference rsaRef = context.getServiceReference(RemoteServiceAdmin.class.getName());
		Assert.assertNotNull(rsaRef);
		rsa = (RemoteServiceAdmin) context.getService(rsaRef);
		Assert.assertNotNull(rsa);
		
		//
		// export the service
		//
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("mykey", "has been overridden");
		properties.put(RemoteConstants.SERVICE_INTENTS, "my_intent_is_for_this_to_work");
		
		Collection<ExportRegistration> registrations = rsa.exportService(registration.getReference(), properties);
		Assert.assertNotNull(registrations);
		Assert.assertFalse(registrations.isEmpty());
		
		for (Iterator<ExportRegistration> it = registrations.iterator(); it.hasNext();) {
			ExportRegistration er = it.next();
			
			Assert.assertNull(er.getException());
			ExportReference ref = er.getExportReference();
			Assert.assertNotNull(ref);
			
			Assert.assertEquals(registration.getReference(), ref.getExportedService());
			
			EndpointDescription ed = ref.getExportedEndpoint();
			Assert.assertNotNull(ed);
			Assert.assertTrue(ed.getInterfaces().contains(A.class.getName()));
			Assert.assertFalse(ed.getInterfaces().contains(B.class.getName()));
			
			Assert.assertNotNull(ed.getRemoteURI());
			Assert.assertNotNull(ed.getConfigurationTypes());
			Assert.assertFalse(ed.getConfigurationTypes().isEmpty());
			Assert.assertTrue(ed.getIntents().contains("my_intent_is_for_this_to_work"));
			Assert.assertEquals(context.getProperty("org.osgi.framework.uuid"), ed.getRemoteFrameworkUUID());
			
			exportEndpointDescription(ed);
		}
		
	}

	/**
	 * 
	 */
	private void teststop() throws Exception {
		// TODO Auto-generated method stub
		
	}

	/**
	 * Write the contents of the EndpointDescription into System properties for the parent framework to
	 * read and then import.
	 * 
	 * @param ed
	 * @throws IOException 
	 */
	private void exportEndpointDescription(EndpointDescription ed) throws IOException {
		Properties props = new Properties();
		
		for (Iterator<String> it = ed.getProperties().keySet().iterator(); it.hasNext();) {
			String key = it.next();
			props.put(key, ed.getProperties().get(key));
		}
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		props.store(bos, null);
		System.getProperties().put("RSA_TCK.EndpointDescription_" + registrationCounter++, bos.toString());
	}
	
	private int registrationCounter = 0;
}