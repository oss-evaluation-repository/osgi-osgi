/*
 * Copyright (c) OSGi Alliance (2014). All Rights Reserved.
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


package org.osgi.test.cases.async.junit;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.ServiceRegistration;
import org.osgi.service.async.Async;
import org.osgi.test.cases.async.junit.services.MyService;
import org.osgi.test.support.OSGiTestCase;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Success;
import org.osgi.util.tracker.ServiceTracker;

public class AsyncTestCase extends OSGiTestCase {
	
	private ServiceTracker<Async, Async>	asyncTracker;
	private Async	async;
	
	private ServiceRegistration<MyService> registration;
	
	private class MyServiceImpl implements MyService {
		private final long timeToSleep = 500;

		@Override
		public void doSlowStuff() throws Exception  {
			Thread.sleep(timeToSleep);
		}

		@Override
		public int countSlowly(int times) throws Exception {
			for(int i = 0; i < times; i++) {
				Thread.sleep(timeToSleep);
			}
			return times;
		}
	}

	protected void setUp() throws InterruptedException {
		registration = getContext().registerService(MyService.class, new MyServiceImpl(), null);
		asyncTracker = new ServiceTracker<>(getContext(), Async.class, null);
		asyncTracker.open();
		async = asyncTracker.waitForService(5000);
		
		assertNotNull("No Async service available within 5 seconds", async);
	}
	
	protected void tearDown() {
		asyncTracker.close();
		registration.unregister();
	}
	
	public void testAsyncCall() throws Exception {
		
		MyService service = async.mediate(registration.getReference());
		
		Promise<Integer> p = async.call(service.countSlowly(2));
		
		assertFalse(p.isDone());
		
		final CountDownLatch latch = new CountDownLatch(1);
		
		p.then(new Success<Integer, Void>() {
			@Override
			public Promise<Void> call(Promise<Integer> resolved)
					throws Exception {
				latch.countDown();
				return null;
			}
		});
		
		assertTrue("Did not complete within a reasonable time", latch.await(3000, TimeUnit.MILLISECONDS));
		
	}

}