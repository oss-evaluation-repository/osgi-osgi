/*
 * Copyright (c) OSGi Alliance (2000, 2011). All Rights Reserved.
 *
 * Implementation of certain elements of the OSGi
 * Specification may be subject to third party intellectual property
 * rights, including without limitation, patent rights (such a third party may
 * or may not be a member of the OSGi Alliance). The OSGi Alliance is not responsible and shall not be
 * held responsible in any manner for identifying or failing to identify any or
 * all such third party intellectual property rights.
 *
 * This document and the information contained herein are provided on an "AS
 * IS" basis and THE OSGI ALLIANCE DISCLAIMS ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO ANY WARRANTY THAT THE USE OF THE INFORMATION HEREIN WILL
 * NOT INFRINGE ANY RIGHTS AND ANY IMPLIED WARRANTIES OF MERCHANTABILITY OR
 * FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT WILL THE OSGI ALLIANCE BE LIABLE FOR ANY
 * LOSS OF PROFITS, LOSS OF BUSINESS, LOSS OF USE OF DATA, INTERRUPTION OF
 * BUSINESS, OR FOR DIRECT, INDIRECT, SPECIAL OR EXEMPLARY, INCIDENTIAL,
 * PUNITIVE OR CONSEQUENTIAL DAMAGES OF ANY KIND IN CONNECTION WITH THIS
 * DOCUMENT OR THE INFORMATION CONTAINED HEREIN, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH LOSS OR DAMAGE.
 *
 * All Company, brand and product names may be trademarks that are the sole
 * property of their respective owners. All rights reserved.
 */
package org.osgi.test.cases.residentialmanagement;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.dmt.DmtSession;

/**
 * This test case tests that the Framework properties are correctly reflected in 
 * the RMT.
 * 
 * @author Steffen Druesedow (Deutsche Telekom Laboratories)
 */
public class FrameworkContentTestCase extends RMTTestBase {

	
	/**
	 * asserts that StartLevel values from the RMT are the same as the one retrieved from FrameworkStartLevel
	 * @throws Exception 
	 */
	public void testGetFrameworkStartLevels() throws Exception {

		FrameworkStartLevel fwStartLevel = getContext().getBundle(0).adapt(FrameworkStartLevel.class);
		session = dmtAdmin.getSession(FRAMEWORK_ROOT, DmtSession.LOCK_TYPE_SHARED);
		int startLevel = session.getNodeValue(FRAMEWORK_ROOT + "/" + STARTLEVEL ).getInt();
		int initialBundleStartLevel = session.getNodeValue(FRAMEWORK_ROOT + "/" + INITIAL_BUNDLE_STARTLEVEL ).getInt();
		
		assertEquals("The value of the StartLevel is incorrect.", fwStartLevel.getStartLevel(), startLevel);
		assertEquals("The value of the InitialBundleStartLevel is incorrect.", fwStartLevel.getInitialBundleStartLevel(), initialBundleStartLevel);

	}
	
	/**
	 * asserts that the properties of the bundles are correctly reflected in the RMT
	 * NOTE: This just tests the first level children of the bundle in the RMT, 
	 * deeper structures are tested in individual testcases.
	 * Only nodes are tested that can be directly matched against bundle properties (i.e. no RequestedState etc.). 
	 * @throws Exception
	 */
	public void testBundleFields() throws Exception {
		session = dmtAdmin.getSession(FRAMEWORK_ROOT, DmtSession.LOCK_TYPE_SHARED);
		assertNotNull(session);

		String uri = FRAMEWORK_ROOT + "/" + BUNDLE;
		String[] bundleKeys = session.getChildNodeNames(uri);
		for (String bundleKey : bundleKeys) {
			String bundleUri = uri + "/" + bundleKey;
			long id = session.getNodeValue(bundleUri + "/" + BUNDLEID).getLong();
			Bundle bundle = getContext().getBundle(id);

			String value = session.getNodeValue(bundleUri + "/" + STATE ).getString();
			assertEquals("The bundle state doesn't match for bundle: " + id, getBundleStateString(bundle.getState()), value );
			value = session.getNodeValue(bundleUri + "/" + SYMBOLIC_NAME ).getString();
			assertEquals("The bundles symbolic name doesn't match for bundle: " + id, bundle.getSymbolicName(), value );
			value = session.getNodeValue(bundleUri + "/" + VERSION ).getString();
			assertEquals("The bundles version doesn't match for bundle: " + id, bundle.getVersion().toString(), value );
			int rmtLevel = session.getNodeValue(bundleUri + "/" + STARTLEVEL ).getInt();
			int realLevel = bundle.adapt(BundleStartLevel.class).getStartLevel();
			assertEquals("The bundles symbolic name doesn't match for bundle: " + id, realLevel, rmtLevel );
			long rmtLastModified = session.getNodeValue(bundleUri + "/" + LAST_MODIFIED ).getDateTime().getTime();
			assertEquals("The bundles last modification timestamp name doesn't match for bundle: " + id, bundle.getLastModified(), rmtLastModified );
		}
	}
	
	/**
	 * Asserts that the BundleType structure is correctly filled.
	 * Currently only the FRAGMENT type is supported. Thats why a fragment bundle is installed first.
	 * For all other bundles the BundleType node is expected with an empty list.
	 * @throws Exception
	 */
	public void testBundleType() throws Exception {
		
		testBundle1 = installBundle(TESTBUNDLE_FRAGMENT, false);
		
		session = dmtAdmin.getSession(FRAMEWORK_ROOT, DmtSession.LOCK_TYPE_SHARED);
		assertNotNull(session);

		String uri = FRAMEWORK_ROOT + "/" + BUNDLE;
		String[] bundleKeys = session.getChildNodeNames(uri);
		for (String bundleKey : bundleKeys) {
			String bundleUri = uri + "/" + bundleKey;
			long id = session.getNodeValue( bundleUri + "/" + BUNDLEID ).getLong();
			boolean isFragment = getContext().getBundle(id).getHeaders().get("Fragment-Host") != null;
			assertTrue( "The BundleType node must exist for installed bundles.", session.isNodeUri(bundleUri+"/"+BUNDLETYPE));
			String[] types = session.getChildNodeNames(bundleUri + "/" + BUNDLETYPE );
			if ( isFragment ) {
				assertNotNull( "The list of BundleTypes must not be null for a fragment bundle.", types );
				assertEquals( "The list of BundleTypes must have exactly one entry for a fragment bundle.", 1, types.length );
				assertEquals( "The bundle type for a fragment bundle must be 'FRAGMENT'", "FRAGMENT", types[0]);
			}
			else {
				assertNotNull( "The list of BundleTypes must not be null.", types );
				assertEquals( "The list of BundleTypes must be empty for a non-fragment bundle.", 0, types.length );
			}
		}
	}
	
	/**
	 * asserts that the bundle headers are correctly reflected in the RMT 
	 * 
	 * @throws Exception 
	 * 
	 */
	public void testBundleHeaders() throws Exception {

		session = dmtAdmin.getSession(FRAMEWORK_ROOT, DmtSession.LOCK_TYPE_SHARED);
		assertNotNull(session);

		String uri = FRAMEWORK_ROOT + "/" + BUNDLE;
		String[] bundleKeys = session.getChildNodeNames(uri);
		for (String bundleKey : bundleKeys) {
			long id = session.getNodeValue(uri + "/" + bundleKey + "/" + BUNDLEID ).getLong();
			// get headers from real bundle
			Dictionary<String, String> realHeaders = getContext().getBundle(id).getHeaders();
			
			String headersUri = uri + "/" + bundleKey + "/" + HEADERS;
			String[] headers = session.getChildNodeNames(headersUri);
			for (String header : headers ) {
				String value = session.getNodeValue(headersUri + "/" + header).getString();
				assertTrue( "Header '"+header+"' exists in RMT but not in the bundle.", realHeaders.get(header) != null );
				assertEquals( "Values for header '"+header+"' are different in RMT and bundle.", realHeaders.get(header), value);
			} 
		}
	}
	
	/**
	 * This test asserts that the bundle-wiring is correctly reflected in the RMT
	 * The order of the Wires per bundle in the RMT is not guaranteed to be the same as in a Wiring-Api snapshot 
	 * and there is no common identifier for a wire in RMT and API.
	 * Therefore the strategy is to loop through the RMT-Wires and to find matches in the API snapshot by doing a subtree comparision.
	 * The test is successful, if each RMT wire has exactly one matching counterpart in the snapshot. 
	 */
	public void testBundleWires() throws Exception {
		// make sure, that there is some content in the wiring
		testBundle1 = installAndStartBundle(TESTBUNDLE_REGISTERING_SERVICES);
		testBundle2 = installAndStartBundle(TESTBUNDLE_USING_SERVICE2);
		
		session = dmtAdmin.getSession(".", DmtSession.LOCK_TYPE_SHARED);
		assertNotNull(session);
		
		String uri = FRAMEWORK_ROOT + "/" + BUNDLE;
		String[] bundleKeys = session.getChildNodeNames(uri);
		for (String bundleKey : bundleKeys) {
			String bundleUri = uri + "/" + bundleKey;
			long id = session.getNodeValue( bundleUri + "/" + BUNDLEID ).getLong();

			String[] nameSpaces = session.getChildNodeNames( bundleUri + "/" + WIRES );
			for ( String nameSpace : nameSpaces ) {
				if ( ! "osgi.wiring.rmt.service".equals(nameSpace) )
					assertStandardNameSpaceTree(nameSpace, id, session, bundleUri + "/" + WIRES + "/" + nameSpace);
				else 
					assertServiceNameSpaceTree(id, session, bundleUri + "/" + WIRES + "/" + nameSpace);
			}
		}
	}

	/**
	 * Asserts that the Bundle.Signers structure is correctly filled.
	 * @throws Exception
	 */
	public void testBundleSigners() throws Exception {
		
		testBundle1 = installAndStartBundle(TESTBUNDLE_TRUSTED);
		testBundle2 = installAndStartBundle(TESTBUNDLE_NON_TRUSTED);
		
		session = dmtAdmin.getSession(".", DmtSession.LOCK_TYPE_SHARED);
		assertNotNull(session);

		String uri = FRAMEWORK_ROOT + "/" + BUNDLE;
		String[] bundleKeys = session.getChildNodeNames(uri);
		for (String bundleKey : bundleKeys) {
			String bundleUri = uri + "/" + bundleKey;
			long id = session.getNodeValue( bundleUri + "/" + BUNDLEID ).getLong();
			Bundle bundle = getContext().getBundle(id);
			
			// get real signer DN list from the bundle
			Map<X509Certificate, List<X509Certificate>> allSignerCerts = bundle.getSignerCertificates(Bundle.SIGNERS_ALL);
			Map<X509Certificate, List<X509Certificate>> trustedSignerCerts = bundle.getSignerCertificates(Bundle.SIGNERS_TRUSTED);
			// we just need a list of certificate DN's
			Map<X509Certificate, List<String>> allSignerDNs = new HashMap<X509Certificate, List<String>>();
			Map<X509Certificate, List<String>> trustedSignerDNs = new HashMap<X509Certificate, List<String>>();
			for (X509Certificate cert : allSignerCerts.keySet() ) {
				List<String> dnList = new ArrayList<String>();
				for (X509Certificate chainCert : allSignerCerts.get(cert) )
					dnList.add(chainCert.getIssuerDN().getName());
				allSignerDNs.put(cert, dnList);
			}
			for (X509Certificate cert : trustedSignerCerts.keySet() ) {
				List<String> dnList = new ArrayList<String>();
				for (X509Certificate chainCert : trustedSignerCerts.get(cert) ) 
					dnList.add(chainCert.getIssuerDN().getName());
				trustedSignerDNs.put(cert, dnList);
			}
			
			List<String> unknownSignerIds = new ArrayList<String>();
			
			
			String[] signers = session.getChildNodeNames(bundleUri + "/" + SIGNERS);
			for ( String signer : signers )
				// compare Signer nodes in RMT with the ones from API and remove them from the maps, if matches are found 
				matchAndRemoveSignerCertificateChain(session, bundleUri + "/" + SIGNERS, signer, allSignerDNs, trustedSignerDNs, unknownSignerIds);
			
			// now both maps should be empty
			assertTrue("Some signer certificates are missing in the RMT.", allSignerDNs.size() > 0 );
			assertTrue("Some trusted signer certificates are missing in the RMT.", trustedSignerDNs.size() > 0 );

			assertTrue("There are unknown Signers in the RMT: " + unknownSignerIds, unknownSignerIds.size() == 0 );
		}
	}
	
	/**
	 * Tests that the bundle entries are correctly reflected in the RMT.
	 * @throws Exception
	 */
	public void testFrameworkBundleEntries() throws Exception {

		session = dmtAdmin.getSession(FRAMEWORK_ROOT, DmtSession.LOCK_TYPE_SHARED);
		assertNotNull(session);

		String uri = FRAMEWORK_ROOT + "/" + BUNDLE;
		String[] bundleKeys = session.getChildNodeNames(uri);
		for (String bundleKey : bundleKeys) {
			String bundleUri = uri + "/" + bundleKey;
			long id = session.getNodeValue( bundleUri + "/" + BUNDLEID ).getLong();
			Bundle bundle = getContext().getBundle(id);
			// get encoded pathes of all file entries of the bundle
			Set<String> expectedPathes = getBundleEntries(bundle, true);

			List<String> unknownPathes = new ArrayList<String>();
			List<String> wrongContent = new ArrayList<String>();
			
			String[] entries = session.getChildNodeNames(bundleUri + "/" + ENTRIES);
			for (String path : entries ) {
				if ( expectedPathes.contains(path)) {
					expectedPathes.remove(path);
					// compare content
					byte[] content = session.getNodeValue(bundleUri + "/" + ENTRIES + "/" + path + "/" + CONTENT).getBinary();
					BufferedInputStream bis = new BufferedInputStream(bundle.getEntry(path).openStream());
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					int b = -1;
					while ( (b = bis.read()) != -1 ) 
						bos.write(b);
					bis.close();
					if ( ! content.equals(bos.toByteArray()))
						wrongContent.add(path);
					bos.close();
				}
				else
					unknownPathes.add(path);
			}
			assertEquals("There are Bundle entries missing in the RMT for bundle '" + id + "': " + expectedPathes, 0, expectedPathes.size());
			assertEquals("There are unknown Bundle entries in the RMT for bundle '" + id + "': " + unknownPathes, 0, unknownPathes.size());
			assertEquals("There are Bundle entries with wrong values in the RMT for bundle '" + id + "': "+ wrongContent, 0, wrongContent.size());
		}
	}

	/**
	 * tests that the framework properties (system.properties) are correctly 
	 * reflected in the RMT
	 * @throws Exception
	 */
	public void testFrameworkProperties() throws Exception {
		Properties expectedProps = System.getProperties();
		
		String uri = FRAMEWORK_ROOT;
		session = dmtAdmin.getSession(uri, DmtSession.LOCK_TYPE_ATOMIC);
		String[] children = session.getChildNodeNames(uri + "/" + PROPERTY);
		List<String> unknownProps = new ArrayList<String>();
		List<String> wrongValue = new ArrayList<String>();
		for (String key : children ) {
			String value = session.getNodeValue(uri + "/" + PROPERTY + "/" + key).getString();
			if ( expectedProps.get(key) == null )
				unknownProps.add(key);
			else
			if ( value.equals(expectedProps.get(key)))
				expectedProps.remove(key);
			else
				wrongValue.add(key);
		}
		
		assertEquals("There are Framework properties missing in the RMT: " + expectedProps, 0, expectedProps.size());
		assertEquals("There are unknown Framework properties in the RMT: " + unknownProps, 0, unknownProps.size());
		assertEquals("There are Framework properties with wrong values in the RMT: " + wrongValue, 0, wrongValue.size());
	}
	
	
	// ************ Utility 
	
	/**
	 * This method performs a comparision of the wires for a given bundle and namespace.
	 * @param nameSpace ... the current namespace
	 * @param bundleId ... the bundle id to get the corresponding bundle for the Wiring-API check
	 * @param session ... the session to use
	 * @param nameSpaceUri ... the DMT uri of the bundles wiring namespace subtree
	 * @throws Exception
	 */
	private void assertStandardNameSpaceTree( String nameSpace, long bundleId, DmtSession session, String nameSpaceUri ) throws Exception {

		// first get a snapshot of the wiring for the given bundle 
		Bundle bundle = getContext().getBundle(bundleId);
		BundleWiring wiring = bundle.adapt(BundleWiring.class);
		
		// create a list that holds all wires (provided and required) for the current bundle 
		List<BundleWire> allApiWires = new ArrayList<BundleWire>();
		allApiWires.addAll(wiring.getProvidedWires(nameSpace));
		allApiWires.addAll(wiring.getRequiredWires(nameSpace));

		// get Wires from RMT
		String[] wires = session.getChildNodeNames(nameSpaceUri);
		for (String wire : wires ) {
			String wireUri = nameSpaceUri + "/" + wire;
			// check for match
			int matchIndex = matchWireTree(session, wireUri, nameSpace, allApiWires);
			if ( matchIndex >= 0 ) {
				pass("Found match for wire: " + wireUri );
				// remove matching wire
				allApiWires.remove(matchIndex);
			}
			else 
				fail("Found no matching wire in the wiring API snapshot for " + wireUri );
		}
		
		assertEquals("Did not find all Wires in RMT that are reported by the Wiring API!", 0, allApiWires.size());
		
	}
	
	/**
	 * This method performs a comparision of the service-wires for a given bundle
	 * It gets the registered and the used services of that bundle and compares the data with the structure in the RMT.
	 * @param bundleId
	 * @param session
	 * @param nameSpaceUri
	 * @throws Exception
	 */
	private void assertServiceNameSpaceTree( long bundleId, DmtSession session, String nameSpaceUri ) throws Exception {
		// TODO: implement this
	}

	/**
	 * This method tries to find a match for the given RMT wire subtree in the list of all wires from the API snapshot.
	 * @param session ... the session to access the RMT
	 * @param uri ... the root uri of the wire subtree
	 * @param nameSpace ... the current namespace
	 * @param allApiWires ... the list of wires from the API snapshot 
	 * @return the index of the matching API wire or -1, in case of no match
	 */
	private int matchWireTree(DmtSession session, String uri, String nameSpace, List<BundleWire> allApiWires) throws Exception {
		int index = -1;
		boolean match = false;
		String rmtProvider = session.getNodeValue(uri + "/" + PROVIDER ).getString();
		String rmtRequirer = session.getNodeValue(uri + "/" + REQUIRER ).getString();
		String rmtNameSpace = session.getNodeValue(uri + "/" + NAMESPACE ).getString();

		String reqUri = uri + "/" + REQUIREMENT;
		String[] children = session.getChildNodeNames( reqUri + "/" + DIRECTIVE);
		Map<String, String> rmtReqDirectivesMap = new HashMap<String, String>();
		for (String rmtKey : children)
			rmtReqDirectivesMap.put(rmtKey, session.getNodeValue(reqUri + "/" + DIRECTIVE + "/" + rmtKey ).getString());

		children = session.getChildNodeNames(reqUri + "/" + ATTRIBUTE);
		Map<String, Object> rmtReqAttributeMap = new HashMap<String, Object>();
		for (String rmtKey : children)
			// we must add the object values as String, because they are also Strings in the RMT
			rmtReqAttributeMap.put(rmtKey, session.getNodeValue(reqUri + "/" + ATTRIBUTE + "/" + rmtKey ).getString());
		
		String capUri = uri + "/" + CAPABILITY;
		children = session.getChildNodeNames(capUri + "/" + DIRECTIVE);
		Map<String, String> rmtCapDirectivesMap = new HashMap<String, String>();
		for (String rmtKey : children)
			rmtCapDirectivesMap.put(rmtKey, session.getNodeValue(capUri + "/" + DIRECTIVE + "/" + rmtKey ).getString());

		children = session.getChildNodeNames(capUri + "/" + ATTRIBUTE);
		Map<String, Object> rmtCapAttributeMap = new HashMap<String, Object>();
		for (String rmtKey : children)
			// we must add the object values as String, because they are also Strings in the RMT
			rmtCapAttributeMap.put(rmtKey, session.getNodeValue(capUri + "/" + ATTRIBUTE + "/" + rmtKey ).getString());

		for (BundleWire wire : allApiWires) {
			index++;
			String provider = wire.getProviderWiring().getBundle().getLocation();
			String requirer = wire.getRequirerWiring().getBundle().getLocation();

			
			if ( ! provider.equals(rmtProvider) )
				continue;
			if ( ! requirer.equals(rmtRequirer) )
				continue;
			if ( ! nameSpace.equals(rmtNameSpace) )
				continue;

			// ******* REQUIREMENT part *********
			BundleRequirement requirement = wire.getRequirement();
			// directives
			Map<String, String> directivesMap = requirement.getDirectives();
			if ( ! directivesMap.equals(rmtReqDirectivesMap) ) 
				continue;
			
			// attributes
			Map<String, Object> attributeMap = requirement.getAttributes();
			if ( ! attributeMap.equals(rmtReqAttributeMap) ) 
				continue;
			
			// FILTER
			// TODO: ?? Is this the correct source of the filter?
			// TODO: ?? Must filter still be present in the directive map?
			String filter = directivesMap.get(FILTER);
			String rmtFilter = session.getNodeValue(reqUri + "/" + FILTER ).getString();
			if ( ! filter.equals(rmtFilter) )
				continue;
			

			// ******* CAPABILITY part *********
			BundleCapability capability = wire.getCapability();
			// directives
			directivesMap = capability.getDirectives();
			if ( ! directivesMap.equals(rmtCapDirectivesMap) ) 
				continue;
			
			// attributes
			attributeMap = requirement.getAttributes();
			if ( ! attributeMap.equals(rmtCapAttributeMap) ) 
				continue;

			// if we reach this point then we have a match
			match = true;
			break;
		}
		return match ? index : -1;
	}

	/**
	 * This method tries to find a match for the given RMT wire subtree in the list of all wires from the API snapshot.
	 * @param session ... the session to access the RMT
	 * @param uri ... the root uri of the wire subtree
	 * @param actualBundleId ... the id of the bundle that is currently checked
	 * @param allApiWires ... the list of wires (or ServiceReferences in this case) from the API
	 * @return the index of the matching API wire or -1, in case of no match
	 */
	private int matchServiceWireTree(DmtSession session, String uri, long actualBundleId, List<ServiceReference> allApiWires) throws Exception {
		int index = -1;
		boolean match = false;
		String provider = null;
		String requirer = null;
		for (ServiceReference ref : allApiWires) {
			index++;
			// if we reach this point then we have a match
			match = true;
			break;
		}
		return match ? index : -1;
	}

	/**
	 * This method reads the certificate chain for the given signer-uri from the RMT and compares it
	 * with the ones in the given maps (all and trusted). 
	 * If there is a match, then the entries are removed from the map.
	 * @param session ... the session to access the RMT
	 * @param uri ... the uri of the bundles signers node
	 * @param signer ... the id of the signer in the RMT list
	 * @param allSignerDNs ... the map of all signers
	 * @param trustedSignerDNs ... the map of trusted signers
	 * @param unknownSigners ... if a RMT signer was not found in the maps it is added to this list
	 * @throws Exception
	 */
	private void matchAndRemoveSignerCertificateChain(DmtSession session, String uri, String signer, 
			Map<X509Certificate, List<String>> allSignerDNs,
			Map<X509Certificate, List<String>> trustedSignerDNs,
			List<String> unknownSigners) throws Exception {
		
		boolean isTrusted = session.getNodeValue(uri + "/" + signer + "/" + ISTRUSTED ).getBoolean();
		String[] children = session.getChildNodeNames(uri + "/" + signer + "/" + CERTIFICATECHAIN );
		List<String> rmtCertChain = Arrays.asList(children);
		
		Object matchKeyAll = null;
		// check against allSigners
		for (X509Certificate cert : allSignerDNs.keySet() ) {
			if ( ! rmtCertChain.equals(allSignerDNs.get(cert) )) 
				continue;
			matchKeyAll = cert;
			break;		
		}
		if ( matchKeyAll != null )
			allSignerDNs.remove(matchKeyAll);

		Object matchKeyTrusted = null;
		// check against trustedSigners
		for (X509Certificate cert : trustedSignerDNs.keySet() ) {
			if ( ! rmtCertChain.equals(trustedSignerDNs.get(cert) )) 
				continue;
			matchKeyTrusted = cert;
			assertTrue( "This signer must have IsTrusted = true.", isTrusted);
			break;		
		}
		if ( matchKeyTrusted != null )
			trustedSignerDNs.remove(matchKeyAll);
		
		if ( matchKeyTrusted == null && matchKeyAll == null )
			unknownSigners.add(signer);
	}
}