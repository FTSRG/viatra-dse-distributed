package org.eclipse.viatra.dse.cluster.test;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.viatra.dse.cluster.host.ClusteredDesignSpaceExplorer;
import org.eclipse.viatra.dse.cluster.interfaces.IProblemServerPrivate;
import org.eclipse.viatra.dse.cluster.interfaces.IProcessingClient.ExplorationNodeState;
import org.eclipse.viatra.dse.cluster.test.factories.AllocateRuleFactory;
import org.eclipse.viatra.dse.cluster.test.factories.CreateCostsObjectiveFactory;
import org.eclipse.viatra.dse.cluster.test.factories.CreateDFS1Strategy;
import org.eclipse.viatra.dse.cluster.test.factories.CreateGuidanceObjectiveFactory;
import org.eclipse.viatra.dse.cluster.test.factories.CreateResourceRuleFactory;
import org.eclipse.viatra.dse.cluster.test.factories.MakeParallelRuleFactory;
import org.eclipse.viatra.dse.cluster.test.factories.MakeSequentialRuleFactory;
import org.eclipse.viatra.dse.cluster.test.factories.ResourceUtilizationObjectiveFactory;
import org.eclipse.viatra.dse.cluster.test.factories.ResponseTimeObjectiveFactory;
import org.eclipse.viatra.dse.designspace.api.ITransition;
import org.eclipse.viatra.dse.designspace.impl.pojo.ConcurrentDesignSpace;
import org.eclipse.viatra.dse.designspace.impl.pojo.State;
import org.eclipse.viatra.dse.examples.bpmn.problems.BpmnProblems;
import org.eclipse.viatra.dse.examples.bpmn.statecoder.BpmnStateCoderFactory;
import org.eclipse.viatra.dse.examples.simplifiedbpmn.SimplifiedbpmnPackage;

public class DSEServer implements IApplication {

	@Override
	public Object start(IApplicationContext context) throws Exception {

		long processStart = System.currentTimeMillis();

		List<String> nodes = new ArrayList<String>();
		nodes.add("192.168.0.11:2552");

		ClusteredDesignSpaceExplorer cdse = new ClusteredDesignSpaceExplorer(nodes);

		cdse.setInitialModel(BpmnProblems.createWebShopProblem());
		cdse.setStateCoderFactory(new BpmnStateCoderFactory());

		cdse.addMetaModelPackage(SimplifiedbpmnPackage.eINSTANCE);

		cdse.registerTransformationRuleFactory(new AllocateRuleFactory());
		cdse.registerTransformationRuleFactory(new CreateResourceRuleFactory());
		cdse.registerTransformationRuleFactory(new MakeParallelRuleFactory());
		cdse.registerTransformationRuleFactory(new MakeSequentialRuleFactory());

		cdse.registerGlobalConstraintFactory(new CreateMyOwnConstraintFactory());

		cdse.registerObjectiveFactory(new CreateGuidanceObjectiveFactory());
		cdse.registerObjectiveFactory(new CreateCostsObjectiveFactory());
		cdse.registerObjectiveFactory(new ResourceUtilizationObjectiveFactory());
		cdse.registerObjectiveFactory(new ResponseTimeObjectiveFactory());
		
		cdse.setDesignspace(new ConcurrentDesignSpace());
		cdse.getGlobalContext().getThreadPool().setMaximumPoolSize(1);
		long exploreStart = System.currentTimeMillis();
		cdse.startExploration(new CreateDFS1Strategy());
		cdse.fixedThreads = 3;

		while (true) {
			Thread.sleep(1000);

			IProblemServerPrivate server = cdse.getServer();

			int completed = 0;
			for (String node : cdse.getNodes()) {
				ExplorationNodeState nodeState = server.getNodeState(node);
				if (nodeState == ExplorationNodeState.COMPLETED) {
					completed++;
				}
			}

			if (completed == nodes.size()) {
				break;
			}
			if (completed > 0) {
				System.out.println("should do recovery for some nodes");
			}
		}
		long processEnd = System.currentTimeMillis();
		System.out.println("Config time: " + (exploreStart - processStart));
		System.out.println("Explore time: " + (processEnd - exploreStart));

		// dump the whole DS for debug
//		dumpFullDSToConsole(cdse);
		return null;
	}

	@Override
	public void stop() {
		System.out.println("Stopped");
	}

	private void dumpFullDSToConsole(ClusteredDesignSpaceExplorer cdse) {
		ConcurrentDesignSpace designSpace = (ConcurrentDesignSpace) cdse.getGlobalContext().getDesignSpace();
		Enumeration<State> states = designSpace.getStates();
		System.out.println("============================ BEGIN");
		while (states.hasMoreElements()) {
			State nextElement = states.nextElement();
			System.out.println("State:\t" + nextElement.getId());
			for (ITransition tr : nextElement.getOutgoingTransitions()) {
				System.out.println("Transition:\t from:" + tr.getFiredFrom().getId() + "\tusing: " + tr.getId()
						+ "\tending: " + (tr.getResultsIn() == null ? "null" : tr.getResultsIn().getId()));
			}
		}
		System.out.println("============================ END");
	}

}
