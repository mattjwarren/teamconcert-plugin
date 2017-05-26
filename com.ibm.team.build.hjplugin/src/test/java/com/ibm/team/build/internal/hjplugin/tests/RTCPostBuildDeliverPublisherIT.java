/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.team.build.internal.hjplugin.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.jvnet.hudson.test.JenkinsRule;

import com.ibm.team.build.internal.hjplugin.Messages;
import com.ibm.team.build.internal.hjplugin.RTCBuildResultAction;
import com.ibm.team.build.internal.hjplugin.RTCJobProperties;
import com.ibm.team.build.internal.hjplugin.RTCFacadeFactory.RTCFacadeWrapper;
import com.ibm.team.build.internal.hjplugin.RTCLoginInfo;
import com.ibm.team.build.internal.hjplugin.RTCPostBuildDeliverPublisher;
import com.ibm.team.build.internal.hjplugin.RTCScm;
import com.ibm.team.build.internal.hjplugin.tests.utils.AbstractTestCase;
import com.ibm.team.build.internal.hjplugin.tests.utils.LoadOptions;
import com.ibm.team.build.internal.hjplugin.tests.utils.MockPublisher;
import com.ibm.team.build.internal.hjplugin.tests.utils.Utils;
import com.ibm.team.build.internal.hjplugin.util.Tuple;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RTCPostBuildDeliverPublisherIT  extends AbstractTestCase{

	private RTCFacadeWrapper testingFacade;

	@Before
	public void setUp() throws Exception {
		if (Config.DEFAULT.isConfigured()) {
			testingFacade = Utils.getTestingFacade();
			createSandboxDirectory();
		}
	}
	
	@After
	public void tearDown() throws Exception {
		if (Config.DEFAULT.isConfigured()) {
			tearDownSandboxDirectory();
			// Add additional code here
		}
	}
	
	@Rule public JenkinsRule r = new JenkinsRule();

	@Test
	public void testPBDeliverStepDoingNothing() throws Exception {
		if (!Config.DEFAULT.isConfigured()) {
			return;
		}
		helperTestPBDeliverMultileStepsDoingNothing(1);
	}
	
	@Test
	public void testPBDeliverMultipleStepsDoingNothing() throws Exception {
		if (!Config.DEFAULT.isConfigured()) {
			return;
		}
		helperTestPBDeliverMultileStepsDoingNothing(5);
	}
	
	/**
	 * Test whether PostBuildPublisher skips post build deliver when the Jenkins job is not configured 
	 * with a build definition. 
	 * Currently, we test for Repository Workspace, Strewam and Snapshot
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPBDeliverSkippedWhenNonBuildDefinitionConfiguration() throws Exception {
		if (!Config.DEFAULT.isConfigured()) {
			return;
		}
		
		Config defaultC = Config.DEFAULT;
		RTCLoginInfo loginInfo = defaultC.getLoginInfo();
		String rtcNotConfiguredForBDMsg = "Ignoring post build deliver because RTC SCM is not configured with a build definition";
		String pbDeliverSkippedMsg = "Post build deliver skipped";
		// PB Deliver with stream configuration
		String streamName = getStreamUniqueName();
		Map<String, String> setupArtifacts =  Utils.setUpBuildStream(testingFacade, defaultC, streamName);
		
		try {
			// Create a freestyle job with stream configuration
			FreeStyleProject prj = Utils.setupFreeStyleJobForStream(r, defaultC, streamName);
			// Add the post build deliver publisher
			prj.getPublishersList().add(new RTCPostBuildDeliverPublisher(true));
			// Run a build
			FreeStyleBuild build = Utils.runBuild(prj, null);
			
			// Verify that post build deliver is skipped
			assertEquals(Result.SUCCESS, build.getResult());
			File logFile = build.getLogFile();
			assertNotNull(Utils.getMatch(logFile, rtcNotConfiguredForBDMsg));
			assertNotNull(Utils.getMatch(logFile, pbDeliverSkippedMsg));
		} finally {
			Utils.tearDown(testingFacade, Config.DEFAULT, setupArtifacts);
		}
		
		// PB Deliver with repository workspace configuration
	    streamName = getStreamUniqueName();
		setupArtifacts = Utils.setUpBuildStream(testingFacade, defaultC,
							streamName);
		String workspaceName = setupArtifacts.get(Utils.ARTIFACT_WORKSPACE_NAME);
		
		try {
			// Create a freestyle job with repository workspace configuration
			FreeStyleProject prj = Utils.setupFreeStyleJobForWorkspace(r, workspaceName);
			// Add the post build deliver publisher
			prj.getPublishersList().add(new RTCPostBuildDeliverPublisher(true));
			// Run a build
			FreeStyleBuild build = Utils.runBuild(prj, null);
			
			// Verify that post build deliver is skipped
			assertEquals(Result.SUCCESS, build.getResult());
			File logFile = build.getLogFile();
			assertNotNull(Utils.getMatch(logFile, rtcNotConfiguredForBDMsg));
			assertNotNull(Utils.getMatch(logFile, pbDeliverSkippedMsg));
		} finally {
			Utils.tearDown(testingFacade, Config.DEFAULT, setupArtifacts);
		}
		
		// PB Deliver with snapshot configuration
		workspaceName = getRepositoryWorkspaceUniqueName();
		String snapshotName = getSnapshotUniqueName();
		String componentName = getComponentUniqueName();
		setupArtifacts = Utils.setupBuildSnapshot(loginInfo, workspaceName, snapshotName, componentName, testingFacade);
		String snapshotUUID = setupArtifacts.get(Utils.ARTIFACT_BASELINESET_ITEM_ID);
		
		try {
			// Create a freestyle job with snapshot configuration
			FreeStyleProject prj = Utils.setupFreeStyleJobForSnapshot(r, snapshotUUID);
			// Add the post build deliver publisher
			prj.getPublishersList().add(new RTCPostBuildDeliverPublisher(true));
			// Run a build
			FreeStyleBuild build = Utils.runBuild(prj, null);
			
			// Verify that post build deliver is skipped
			assertEquals(Result.SUCCESS, build.getResult());
			File logFile = build.getLogFile();
			assertNotNull(Utils.getMatch(logFile, rtcNotConfiguredForBDMsg));
			assertNotNull(Utils.getMatch(logFile, pbDeliverSkippedMsg));
		} finally {
			Utils.tearDown(testingFacade, Config.DEFAULT, setupArtifacts);
		}
	}
	
	/**
	 * If there are multiple PB deliver steps, subsequent PB deliver step will not attempt 
	 * post build deliver for the build result because the first post build deliver step marks 
	 * the build result action as handled. 
	 * Verify that RTCBuildResultAction has a handled flag which is set to true.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPBDeliverMultipleStepsAvoidHandlingSameBuildResult() throws Exception {
		if (!Config.DEFAULT.isConfigured()) {
			return;
		}
		RTCLoginInfo loginInfo = Config.DEFAULT.getLoginInfo();
		
		// First setup a build definition with Pb deliver configuration
		String componentName = getComponentUniqueName();
		String workspaceName = getRepositoryWorkspaceUniqueName();
		String buildDefinitionId = getBuildDefinitionUniqueName();
		
		Map<String, String> setupArtifacts = Utils.setupBuildDefinitionWithPBDeliver(loginInfo, componentName, 
							workspaceName, buildDefinitionId, null);
		
		try {
			// Create a freestyle project with RTCPostbuilddeliver added twice
			FreeStyleProject prj = Utils.setupFreeStyleJobWithPBDeliver(r, buildDefinitionId);
			// Add another RTCPostBuildDeliverPublisher to the project
			prj.getPublishersList().add(new RTCPostBuildDeliverPublisher(true));
			
			// Run a build and verify that it succeeded
			FreeStyleBuild b = Utils.runBuild(prj, Utils.getPactionsWithEmptyBuildResultUUID());
			Utils.putBuildResultUUIDIntoArtifactIds(b, setupArtifacts);
			assertEquals(Result.SUCCESS, b.getResult());
			
			// Ensure that the post build deliver succeeded message appears only once
			File f = b.getLogFile();
			dumpPBLogFile(b, "testPBDeliverMultipleStepsAvoidHandlingSameBuildResult");
			assertEquals(1, Utils.getMatchCount(f, "Starting post build deliver for build result"));
			assertEquals(1, Utils.getMatchCount(f, ".*Post-build deliver complete"));
			assertEquals(1, Utils.getMatchCount(f, "Summary of Post-build Deliver"));
			assertEquals(1, Utils.getMatchCount(f, "Post build deliver succeeded for build result"));
			
			// Verify that the RTCBuildResultAction is marked handled.
			RTCBuildResultAction action = b.getAction(RTCBuildResultAction.class);
			assertEquals("true", action.getBuildProperties().get(RTCJobProperties.POST_BUILD_DELIVER_HANDLED));
		} finally {
			Utils.tearDown(testingFacade, Config.DEFAULT, setupArtifacts);
		}
	}
	
	/**
	 * Test whether post build deliver is skipped if the build definition is not configured with 
	 * jazz source control but configured with post build deliver
	 */
	@Test
	public void testPBDeliverSkippedIfBuildDefinitionDoesNotHaveJazzScm() throws Exception {
		// TODO not implemented
	}
	
	/**
	 * Test that post build deliver works properly if the build result is controlled by RTC.
	 * The build definition is configured with Jazz SCM and Post Build Deliver.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPBDeliverSuccessOnBuildResultInitiatedFromRTC() throws Exception {
		// TODO not implemented
	}
	
	/**
	 * Test that post build deliver is skipped on a personal build initiated from RTC
	 */
	@Test
	public void testPBDeliverSkippedOnPersonalBuildStartedFromRTC() throws Exception {
		// TODO not implemented
	}
	
	/**
	 * Test the scenario where post build deliver is enabled on a build definition which is configured with jazz 
	 * source control, post build deliver executes successfully
	 * @throws Exception
	 */
	@Test
	public void testPBDeliverSuccess() throws Exception {
		if (!Config.DEFAULT.isConfigured()) {
			return;
		}
		RTCLoginInfo loginInfo = Config.DEFAULT.getLoginInfo();

		// Setup a build definition with Jazz SCM and PB deliver configured
		String componentName = getComponentUniqueName();
		String workspaceName = getRepositoryWorkspaceUniqueName();
		String buildDefinitionId = getBuildDefinitionUniqueName();
		
		Map<String, String> setupArtifacts = Utils.setupBuildDefinitionWithPBDeliver(loginInfo, componentName, 
							workspaceName, buildDefinitionId, null);
		try {
			FreeStyleProject prj = Utils.setupFreeStyleJobWithPBDeliver(r, buildDefinitionId);
			FreeStyleBuild b = Utils.runBuild(prj, Utils.getPactionsWithEmptyBuildResultUUID());
			// Once the build has run, put the build result uuid into artifactIds so that they can be
			// cleaned up
			Utils.putBuildResultUUIDIntoArtifactIds(b, setupArtifacts);
			
			// Verify that the build completed successfully
			assertEquals(Result.SUCCESS, b.getResult());

			// Verify that messages are printed on the console log
			File f = b.getLogFile();
			dumpPBLogFile(b, "testPBDeliverSuccess");
			assertNotNull(Utils.getMatch(f, "Starting post build deliver for build result"));
			assertNotNull(Utils.getMatch(f, ".*Post-build deliver complete"));
			assertNotNull(Utils.getMatch(f, "Summary of Post-build Deliver"));
			assertNotNull(Utils.getMatch(f, "Post build deliver succeeded for build result"));		
		} finally {
			Utils.tearDown(testingFacade, Config.DEFAULT, setupArtifacts);
		}
	}
	
	/**
	 * Test whether post build deliver is skipped on a build definition which is not configured 
	 * with post build deliver.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPBDeliverSkippedDueToNoConfigElement() throws Exception {
		if (!Config.DEFAULT.isConfigured()) {
			return;
		}
		RTCLoginInfo loginInfo = Config.DEFAULT.getLoginInfo();

		// Setup a build definition with Jazz SCM and PB deliver configured
		String componentName = getComponentUniqueName();
		String workspaceName = getRepositoryWorkspaceUniqueName();
		String buildDefinitionId = getBuildDefinitionUniqueName();
		
		@SuppressWarnings("unchecked")
		Map<String, String> setupArtifacts = (Map<String, String>) testingFacade
				.invoke("setupAcceptChanges",
						new Class[] { String.class, // serverURL,
								String.class, // userId,
								String.class, // password,
								int.class, // timeout,
								String.class, // workspaceName,
								String.class, // componentToAddName,
								String.class, //buildDefinitionId
								boolean.class, // createBuildDefinition
								boolean.class}, // createBuildResult
						loginInfo.getServerUri(),
						loginInfo.getUserId(),
						loginInfo.getPassword(),
						loginInfo.getTimeout(), workspaceName,
						componentName, buildDefinitionId, true, false);
		try {
			FreeStyleProject prj = Utils.setupFreeStyleJobWithPBDeliver(r, buildDefinitionId);
			FreeStyleBuild b = Utils.runBuild(prj, Utils.getPactionsWithEmptyBuildResultUUID());
			// Once the build has run, put the build result uuid into artifactIds so that they can be
			// cleaned up
			Utils.putBuildResultUUIDIntoArtifactIds(b, setupArtifacts);
			// Verify that the build completed successfully
			assertEquals(Result.SUCCESS, b.getResult());

			// Verify that messages are printed on the console log
			File f = b.getLogFile();
			dumpPBLogFile(b, "testPBDeliverSkippedDueToNoConfigElement");
			assertNull(Utils.getMatch(f, "Starting post build deliver for build result"));
			assertNotNull(Utils.getMatch(f, "Post build deliver is not configured for build definition"));
		} finally {
			Utils.tearDown(testingFacade, Config.DEFAULT, setupArtifacts);
		}
	}
	
	/**
	 * Test whether post build deliver is skipped when post build deliver is not enabled.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPBDeliverSkippedDueToNotEnabled() throws Exception {
		if (!Config.DEFAULT.isConfigured()) {
			return;
		}
		RTCLoginInfo loginInfo = Config.DEFAULT.getLoginInfo();

		// Setup a build definition with Jazz SCM and PB deliver configured
		String componentName = getComponentUniqueName();
		String workspaceName = getRepositoryWorkspaceUniqueName();
		String buildDefinitionId = getBuildDefinitionUniqueName();
		Map<String, String> configOrGenericProperties = new HashMap<String, String> ();
		configOrGenericProperties.put("team.scm.deliver.enabled", "false");
		Map<String, String> setupArtifacts = Utils.setupBuildDefinitionWithPBDeliver(loginInfo, componentName, workspaceName, buildDefinitionId,
				configOrGenericProperties);
		try {
			FreeStyleProject prj = Utils.setupFreeStyleJobWithPBDeliver(r, buildDefinitionId);
			FreeStyleBuild b = Utils.runBuild(prj, Utils.getPactionsWithEmptyBuildResultUUID());
			// Once the build has run, put the build result uuid into artifactIds so that they can be
			// cleaned up
			Utils.putBuildResultUUIDIntoArtifactIds(b, setupArtifacts);
			// Verify that the build completed successfully
			assertEquals(Result.SUCCESS, b.getResult());

			// Verify that messages are printed on the console log
			File f = b.getLogFile();
			dumpPBLogFile(b, "testPBDeliverSkippedDueToNotEnabled");
			assertNull(Utils.getMatch(f, "Starting post build deliver for build result"));
			assertNotNull(Utils.getMatch(f, "Post build deliver skipped for build result"));
			assertNotNull(Utils.getMatch(f, "Post build deliver is disabled for build definition"));
		} finally {
			Utils.tearDown(testingFacade, Config.DEFAULT, setupArtifacts);
		}
	}

	/**
	 * Test whether post build deliver is skipped if the trigger policy is NO_WARNINGS and the build 
	 * is UNSTABLE.
	 * 
	 * @throws Exception
	 */
	@Test 
	public void testPBDeliverSkippedDueToTriggerPolicyNoWarningsAndBuildUnstable() throws Exception {
		helperTestPBDeliverSkippedDueToTriggerPolicy(Result.UNSTABLE, "NO_WARNINGS");
	}

	/**
	 * Test whether post build deliver is skipped if the trigger policy is NO_ERRORS and the build 
	 * is FAILURE.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testPBDeliverSkippedDueToTriggerPolicyNoErrorsAndBuildFailure() throws Exception {
		helperTestPBDeliverSkippedDueToTriggerPolicy(Result.FAILURE, "NO_ERRORS");
	}
	
	/**
	 * Test whether post build deliver succeeds if the trigger policy is NO_ERRORS and the build 
	 * is UNSTABLE.
	 * 
	 * @throws Exception
	 */
    @Test
    public void testPBDeliverSucceededDueToTriggerPolicyNoErrorsAndBuildUnstable() throws Exception {
        helperTestPBDeliverSucceededDueToRelaxedTriggerPolicy(Result.UNSTABLE, "NO_ERRORS");
    }

	/**
	 * Test whether post build deliver succeeds if the trigger policy is ALWAYS and the build 
	 * is FAILURE.
	 * 
	 * @throws Exception
	 */
    @Test
    public void testPBDeliverSucceededDueToTriggerPolicyAlwaysAndBuildFailure() throws Exception {
        helperTestPBDeliverSucceededDueToRelaxedTriggerPolicy(Result.FAILURE, "ALWAYS");
    }
    
	/**
	 * Test whether post build deliver succeeds if the trigger policy is ALWAYS and the build 
	 * is UNSTABLE.
	 * 
	 * @throws Exception
	 */
    @Test
    public void testPBDeliverSucceededDueToTriggerPolicyAlwaysAndBuildUnstable() throws Exception {
    	helperTestPBDeliverSucceededDueToRelaxedTriggerPolicy(Result.UNSTABLE, "ALWAYS");
    }

    /**
     * This is the closest we can get to verifying post build deliver in pipeline builds.
     * 
     * Create two build definitions and create build result for each.
	 * Create two RTCBuildResultActions, one for each build result.
	 * In the MockPublisher step, add the build result actions created previously.
	 * When RTCPostBuildDeliverPublisher runs, it will perform post build deliver for each build
	 * result.
     * 
     * @throws Exception
     */
	@Test
	public void testPBDeliverSuccessForMultipleRTCBuildResultActions() throws Exception {
		// First create two build definitions with post build deliver configuration along
		// with two build results
		if (!Config.DEFAULT.isConfigured()) {
			return;
		}
		RTCLoginInfo loginInfo = Config.DEFAULT.getLoginInfo();
		Map<String,String> setupArtifacts = null;
		Map<String,String> setupArtifacts2 = null;
		try {
			RTCBuildResultAction action1 = null;
			RTCBuildResultAction action2 = null;
			{
				Tuple<Map<String, String>,RTCBuildResultAction> result = getRTCBuildResultAction(loginInfo);
				setupArtifacts = result.getFirst();
				action1 = result.getSecond();
			}
			{
				Tuple<Map<String, String>,RTCBuildResultAction> result = getRTCBuildResultAction(loginInfo);
				setupArtifacts2 = result.getFirst();
				action2 = result.getSecond();
			}

			// Create a publisher which can add two RTCBuildResultActions that have the required build result UUIDs
			// and RTCScm configurations.
			MockPublisher publisher = new MockPublisher(Result.SUCCESS, new RTCBuildResultAction[] {action1, action2});
			
			// Create a FreeStyleJob with the above publisher and RTCPostBuildDeliverPublisher
			FreeStyleProject prj = r.createFreeStyleProject();
			prj.getPublishersList().add(publisher);
			prj.getPublishersList().add(new RTCPostBuildDeliverPublisher(true));
			
			// Run a build 
			FreeStyleBuild build = Utils.runBuild(prj, null);
			File f = build.getLogFile();
			
			// Verify that there are two successful post build deliver messages.
			dumpPBLogFile(build, "testPBDeliverSuccessForMultipleRTCBuildResultActions");
			assertEquals(Result.SUCCESS, build.getResult());
			assertEquals(2, Utils.getMatchCount(f, "Starting post build deliver for build result"));
			assertEquals(2, Utils.getMatchCount(f, ".*Post-build deliver complete"));
			assertEquals(2, Utils.getMatchCount(f, "Summary of Post-build Deliver"));
			assertEquals(2, Utils.getMatchCount(f, "Post build deliver succeeded for build result"));	
			// twice for Added components and twice for removed components
			assertEquals(4, Utils.getMatchCount(f, ".*<none>"));
			// Build result uuid 1 should be present twice
			assertEquals(1, Utils.getMatchCount(f, "Starting post build deliver for build result .*" + action1.getBuildResultUUID()));
			assertEquals(1, Utils.getMatchCount(f, "Post build deliver succeeded for build result .*" + action1.getBuildResultUUID()));
			// Build result uuid 2 should be present twice
			assertEquals(1, Utils.getMatchCount(f, "Starting post build deliver for build result .*" + action2.getBuildResultUUID()));
			assertEquals(1, Utils.getMatchCount(f, "Post build deliver succeeded for build result .*" + action2.getBuildResultUUID()));
		} finally {
			if (setupArtifacts != null) {
				Utils.tearDown(testingFacade, Config.DEFAULT, setupArtifacts);
			}
			if (setupArtifacts2 != null) {
				Utils.tearDown(testingFacade, Config.DEFAULT, setupArtifacts2);
			}
		}
	}
	
	/**
	 * When there are two build results, if there is a failure in post build for the first one, then second 
	 * one proceeds but post build deliver is skipped because of trigger policy. We should see a skipped message
	 * for the second post build deliver.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testFailureInFirstPBSkipsNextPBDueToTriggerPolicyNoErrors() throws Exception {
		
		// Create a build definition with post build deliver configuration with a build result.
		if (!Config.DEFAULT.isConfigured()) {
			return;
		}
		RTCLoginInfo loginInfo = Config.DEFAULT.getLoginInfo();
		Map<String,String> setupArtifacts = null;
		
		try {
			// Create a fake build result action 
			RTCScm scm = Utils.constructRTCScmForBuildDefinition(r, "unknown build definition");
			RTCBuildResultAction action1 = new RTCBuildResultAction(loginInfo.getServerUri(), java.util.UUID.randomUUID().toString(), false, scm);

			RTCBuildResultAction action2 = null;
			Tuple<Map<String, String>,RTCBuildResultAction> result = getRTCBuildResultAction(loginInfo);
			setupArtifacts = result.getFirst();
			action2 = result.getSecond();

			// Create a publisher that adds a fake RTCBuildResultAction and a proper RTCBuildResultAction with the
			// UUID of the build result.
			MockPublisher publisher = new MockPublisher(Result.SUCCESS, new RTCBuildResultAction[] { action1, action2});
			
			// Create a FreeStyleJob with the above publisher and RTCPostBuildDeliverPublisher
			FreeStyleProject prj = r.createFreeStyleProject();
			prj.getPublishersList().add(publisher);
			prj.getPublishersList().add(new RTCPostBuildDeliverPublisher(true));
			
			// Run a build 
			FreeStyleBuild build = Utils.runBuild(prj, null);
			File f = build.getLogFile();
			// Verify that there are two sets of post build deliver messages. 
			// First for post build deliver that failed.
			// The second says that post build deliver is skipped.
			dumpPBLogFile(build, "testFailureInFirstPBSkipsNextPBDueToTriggerPolicyNoErrors");
			assertEquals(Result.FAILURE, build.getResult());
			// Post build deliver fails for first build result 
			assertNotNull(Utils.getMatch(f, "Setting the build status to FAILURE"));
			assertNotNull(Utils.getMatch(f, "Post build deliver failed for build result .*("+ action1.getBuildResultUUID() + ")"));
			
			// Post build deliver skipped for second build result.
			assertNotNull(Utils.getMatch(f, "Trigger policy prevented post build deliver for build result \""+action2.getBuildResultUUID() + "\""));
			assertNotNull(Utils.getMatch(f, "Post build deliver skipped for build result .*" + action2.getBuildResultUUID()));
		} finally {
			Utils.tearDown(testingFacade, Config.DEFAULT, setupArtifacts);
		}
	}
	
	/**
	 * When the first post build deliver operation fails, RTCPostBuildDeliverPublisher should not set the build status to FAILURE if failOnError
	 * is false. In that case, the second post build deliver will succeed.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testFailureInFirstPBDoesNotPreventNextPBDueToNoFailOnErrorAndTriggerPolicyNoErrors() throws Exception {
		// Create a build definition with post build deliver configuration with a build result.
		if (!Config.DEFAULT.isConfigured()) {
			return;
		}
		RTCLoginInfo loginInfo = Config.DEFAULT.getLoginInfo();
		Map<String,String> setupArtifacts = null;
		
		try {
			// Create a fake build result action 
			RTCScm scm = Utils.constructRTCScmForBuildDefinition(r, "unknown build definition");
			RTCBuildResultAction action1 = new RTCBuildResultAction(loginInfo.getServerUri(), java.util.UUID.randomUUID().toString(), false, scm);

			RTCBuildResultAction action2 = null;
			Tuple<Map<String, String>,RTCBuildResultAction> result = getRTCBuildResultAction(loginInfo);
			setupArtifacts = result.getFirst();
			action2 = result.getSecond();

			// Create a publisher that adds a fake RTCBuildResultAction and a proper RTCBuildResultAction with the
			// UUID of the build result.
			MockPublisher publisher = new MockPublisher(Result.SUCCESS, new RTCBuildResultAction[] { action1, action2});
			
			// Create a FreeStyleJob with the above publisher and RTCPostBuildDeliverPublisher with failOnError set to false.
			FreeStyleProject prj = r.createFreeStyleProject();
			prj.getPublishersList().add(publisher);
			prj.getPublishersList().add(new RTCPostBuildDeliverPublisher(false));

			// Run a build 
			FreeStyleBuild build = Utils.runBuild(prj, null);
			File f = build.getLogFile();

			// Verify that there are two sets of post build deliver messages. First post build deliver failed.
			// The second says that post build deliver succeeded because the build is not set to FAILURE
			dumpPBLogFile(build, "testFailureInFirstPBDoesNotPreventNextPBDueToNoFailOnErrorAndTriggerPolicyNoErrors");
			
			// For the first build result, post build deliver failed.
			assertEquals(Result.SUCCESS, build.getResult());
			assertNotNull(Utils.getMatch(f, "Post build deliver failed for build result .*("+ action1.getBuildResultUUID() + ")"));
			
			// For the second build result, post build deliver succeeded
			assertNotNull(Utils.getMatch(f, "Starting post build deliver for build result"));
			assertNotNull(Utils.getMatch(f, ".*Post-build deliver complete"));
			assertNotNull(Utils.getMatch(f, "Summary of Post-build Deliver"));
			assertNotNull(Utils.getMatch(f, "Post build deliver succeeded for build result"));	
			
		} finally {
			Utils.tearDown(testingFacade, Config.DEFAULT, setupArtifacts);
		}
	}
	
	/**
	 * Helper method to check whether post build deliver is skipped for the given Jenkins build result status and trigger policy
	 * 
	 * @param expectedResult  The result to be set for the Jenkins build
	 * @param triggerPolicy The triggerPolicy for RTC's Post build deliver configuration.
	 * 
	 * @throws Exception
	 */
	private void helperTestPBDeliverSkippedDueToTriggerPolicy(Result expectedResult, String triggerPolicy) throws Exception {
		if (!Config.DEFAULT.isConfigured()) {
			return;
		}
		RTCLoginInfo loginInfo = Config.DEFAULT.getLoginInfo();

		// Setup a build definition with Jazz SCM and PB deliver configured
		String componentName = getComponentUniqueName();
		String workspaceName = getRepositoryWorkspaceUniqueName();
		String buildDefinitionId = getBuildDefinitionUniqueName();
		Map<String, String> configOrGenericProperties = new HashMap<String, String>();
		configOrGenericProperties.put("team.scm.deliver.triggerPolicy", triggerPolicy);
		Map<String, String> setupArtifacts = Utils.setupBuildDefinitionWithPBDeliver(loginInfo, componentName, workspaceName, buildDefinitionId,
										configOrGenericProperties);
		try {
			// Add a Publisher who can fail the build
			FreeStyleProject prj = Utils.setupFreeStyleJobWithPBDeliver(r, buildDefinitionId, Collections.singletonList(new MockPublisher(expectedResult)));
			FreeStyleBuild b = Utils.runBuild(prj, Utils.getPactionsWithEmptyBuildResultUUID());
			
			// Once the build has run, put the build result uuid into artifactIds so that they can be
			// cleaned up
			Utils.putBuildResultUUIDIntoArtifactIds(b, setupArtifacts);
			
			// Verify that the build completed successfully
			assertEquals(expectedResult, b.getResult());
			
			String triggerPolicyString = "Trigger policy prevented post build deliver for build result \"" + Utils.getBuildResultUUID(b) + "\". Current build status is \"" +
					expectedResult.toString() + "\" and trigger policy is \""+ triggerPolicy + "\"";

			// Verify that the text "post build deliver skipped" appears  in the log
			File f = b.getLogFile();
			dumpPBLogFile(b, "helperTestPBDeliverSkippedDueToTriggerPolicy");
			assertNull(Utils.getMatch(f, "Starting post build deliver for build result"));
			assertNotNull(Utils.getMatch(f, "Post build deliver skipped for build result"));
			assertNotNull(Utils.getMatch(f, triggerPolicyString));
			
		} finally {
			Utils.tearDown(testingFacade, Config.DEFAULT, setupArtifacts);
		}	
	}
	
    /**
     * Helper to test whether post build deliver succeeds for the given Jenkins build result status and trigger policy.
     * 
     * @throws Exception
     */
    private void helperTestPBDeliverSucceededDueToRelaxedTriggerPolicy(Result expectedResult, String triggerPolicy) throws Exception {
		if (!Config.DEFAULT.isConfigured()) {
			return;
		}
		RTCLoginInfo loginInfo = Config.DEFAULT.getLoginInfo();

		// Setup a build definition with Jazz SCM and PB deliver configured
		String componentName = getComponentUniqueName();
		String workspaceName = getRepositoryWorkspaceUniqueName();
		String buildDefinitionId = getBuildDefinitionUniqueName();
		Map<String, String> configOrGenericProperties = new HashMap<String, String>();
		configOrGenericProperties.put("team.scm.deliver.triggerPolicy", triggerPolicy);
		Map<String, String> setupArtifacts = Utils.setupBuildDefinitionWithPBDeliver(loginInfo, componentName, workspaceName, buildDefinitionId,
				configOrGenericProperties);
    	
		try {
			// Add a Publisher who can fail the build
			FreeStyleProject prj = Utils.setupFreeStyleJobWithPBDeliver(r, buildDefinitionId, Collections.singletonList(new MockPublisher(expectedResult)));
			FreeStyleBuild b = Utils.runBuild(prj, Utils.getPactionsWithEmptyBuildResultUUID());
			// Once the build has run, put the build result uuid into artifactIds so that they can be
			// cleaned up
			Utils.putBuildResultUUIDIntoArtifactIds(b, setupArtifacts);
			// Verify that the build completed successfully
			assertEquals(expectedResult, b.getResult());
			
			// Verify that the text "post build deliver skipped" appears  in the log
			File f = b.getLogFile();
			dumpPBLogFile(b, "helperTestPBDeliverSucceededDueToRelaxedTriggerPolicy");
			assertEquals(expectedResult, b.getResult());
			assertNotNull(Utils.getMatch(f, "Starting post build deliver for build result"));
			assertNotNull(Utils.getMatch(f, ".*Post-build deliver complete"));
			assertNotNull(Utils.getMatch(f, "Summary of Post-build Deliver"));
			assertNotNull(Utils.getMatch(f, "Post build deliver succeeded for build result"));	
		} finally {
			Utils.tearDown(testingFacade, Config.DEFAULT, setupArtifacts);
		}	
    }
	
    /**
     * 
     * Helper to verify that multiple PB deliver steps without any build definition works 
     * 
     * @param count the number of PB deliver steps to add to the Freestyle project
     * @throws Exception
     */
	private void helperTestPBDeliverMultileStepsDoingNothing(int count) throws Exception {
		// Create a freestyle project and then add RTCPostBuilddeliverPublisher
		FreeStyleProject prj = r.createFreeStyleProject();
		for (int i = 0 ; i < count; i++) {
			prj.getPublishersList().add(new RTCPostBuildDeliverPublisher(true));
		}
		
		// Run a build
		FreeStyleBuild b = Utils.runBuild(prj, Utils.getPactionsWithEmptyBuildResultUUID());
		
		// Verify that the build successfully completed
		assertEquals(Result.SUCCESS, b.getResult());
		assertEquals(count, prj.getPublishersList().size());
		
		// Verify that the log does not contain any text
		File f = b.getLogFile();
		dumpPBLogFile(b, "helperTestPBDeliverMultileStepsDoingNothing");
		assertNull(Utils.getMatch(f, Messages.RTCPostBuildDeliverPublisher_setting_build_to_failure()));
		assertNull(Utils.getMatch(f, "Post build deliver succeeded for build result"));
		assertNull(Utils.getMatch(f,  "Post build deliver skipped"));	
		assertNotNull(Utils.getMatch(f, "Finished: "));
	}
	
	/**
	 * Dump the log file from the build into a temporary file
	 * 
	 * @param b the Jenkins build
	 * @param filename the name of the temporary file
	 * @throws IOException if there is an exception reading or writing files.
	 */
	private void dumpPBLogFile(FreeStyleBuild b, String filename) throws IOException {
		if (Config.DEFAULT.isDumpLogFiles()) {
			File tmpFile = File.createTempFile("pbdeliver-" + filename, ".tmp");
			PrintWriter outputPrintWriter = null;
			Scanner inputLogFileScanner = null;
			try {
				outputPrintWriter = new PrintWriter(tmpFile);
				File logFile = b.getLogFile();
				inputLogFileScanner = new Scanner(new FileInputStream(logFile));
				while(inputLogFileScanner.hasNextLine()) {
					outputPrintWriter.print(inputLogFileScanner.nextLine());
				}
			}
			finally {
				if (outputPrintWriter != null) {
					outputPrintWriter.close();
				}
				if (inputLogFileScanner != null) {
					inputLogFileScanner.close();
				}
			}
		}
	}

	/**
	 * Create a build definition and a build result for the build definition. Construct a {@link RTCBuildResultAction} with {@link RTCScm} 
	 * pointing to the build definition and uuid of the build result. 
	 * 
	 * Perform accept and load on the build result. Get the properties from accept/load and put it into {@link RTCBuildResultAction}.
	 * 
	 * @param loginInfo - The login information for the RTC server
	 * @return a {@link RTCBuildResultAction}
	 * @throws Exception
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private Tuple<Map<String, String>, RTCBuildResultAction> getRTCBuildResultAction(RTCLoginInfo loginInfo)
			throws Exception, FileNotFoundException, IOException {
		Map<String, String> setupArtifacts;
		// Setup a build definition with Jazz SCM and PB deliver configured
		String buildDefinitionId = getBuildDefinitionUniqueName();
		setupArtifacts = Utils.setupBuildDefinitionWithPBDeliver(loginInfo, getComponentUniqueName(), getRepositoryWorkspaceUniqueName(), buildDefinitionId,
				true, null);
		String buildResultUUID = setupArtifacts.get(Utils.ARTIFACT_BUILDRESULT_ITEM_ID);
		
		File changeLogFile = new File(sandboxDir, "RTCChangeLogFile");
		FileOutputStream changeLog = new FileOutputStream(changeLogFile);
		
		// Perform accept on the build result 1.
		Map<String, String> buildProperties1 = Utils.acceptAndLoad(testingFacade, loginInfo.getServerUri(), loginInfo.getUserId(), 
				loginInfo.getPassword(), loginInfo.getTimeout(),
				buildResultUUID, null, null, null, 
				sandboxDir.getCanonicalPath(), changeLog, "Snapshot", null,
				LoadOptions.getDefault(), getTaskListener(), Locale.getDefault());
		
		// Construct RTCScm instance for the above build definition
		RTCScm scm = Utils.constructRTCScmForBuildDefinition(r, buildDefinitionId);
		// Construct RTCBuildResultAction with the buildProperties, build result UUID and scm instance.
		RTCBuildResultAction action = new RTCBuildResultAction(loginInfo.getServerUri(), buildResultUUID, true, scm);
		action.addBuildProperties(buildProperties1);
		return new Tuple<Map<String, String>, RTCBuildResultAction>(setupArtifacts, action);
	}
}
