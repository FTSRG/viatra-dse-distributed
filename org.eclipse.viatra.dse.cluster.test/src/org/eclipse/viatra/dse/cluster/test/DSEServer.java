package org.eclipse.viatra.dse.cluster.test;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.viatra.dse.cluster.host.ClusteredDesignSpaceExplorer;
import org.eclipse.viatra.dse.cluster.host.RemoteIncrementalStateCoderFactory;
import org.eclipse.viatra.dse.cluster.test.factories.AllocateRuleFactory;
import org.eclipse.viatra.dse.cluster.test.factories.CreateCostsObjectiveFactory;
import org.eclipse.viatra.dse.cluster.test.factories.CreateDFS4Strategy;
import org.eclipse.viatra.dse.cluster.test.factories.CreateGlobalConstraintFactory;
import org.eclipse.viatra.dse.cluster.test.factories.CreateGuidanceObjectiveFactory;
import org.eclipse.viatra.dse.cluster.test.factories.CreateResourceRuleFactory;
import org.eclipse.viatra.dse.cluster.test.factories.MakeParallelRuleFactory;
import org.eclipse.viatra.dse.cluster.test.factories.MakeSequentialRuleFactory;
import org.eclipse.viatra.dse.cluster.test.factories.ResourceUtilizationObjectiveFactory;
import org.eclipse.viatra.dse.cluster.test.factories.ResponseTimeObjectiveFactory;
import org.eclipse.viatra.dse.designspace.impl.pojo.ConcurrentDesignSpace;
import org.eclipse.viatra.dse.examples.bpmn.problems.BpmnProblems;
import org.eclipse.viatra.dse.examples.bpmn.statecoder.BpmnStateCoderFactory;
import org.eclipse.viatra.dse.examples.simplifiedbpmn.SimplifiedbpmnPackage;
import org.eclipse.viatra.dse.statecode.graph.GraphHasherFactory;
import org.eclipse.viatra.dse.statecode.incrementalgraph.IncrementalGraphHasherFactory;
import org.eclipse.viatra.dse.statecode.incrementalgraph.impl.IncrementalGraphHasher;

public class DSEServer implements IApplication {

	@Override
	public Object start(IApplicationContext context) throws Exception {

		List<String> nodes = new ArrayList<String>();
		nodes.add("192.168.0.11:2552");

		ClusteredDesignSpaceExplorer cdse = new ClusteredDesignSpaceExplorer(nodes);

		cdse.setInitialModel(BpmnProblems.createWebShopProblem());
		cdse.setStateCoderFactory(new BpmnStateCoderFactory());
		
		cdse.setStateCoderFactory(new GraphHasherFactory());

		cdse.addMetaModelPackage(SimplifiedbpmnPackage.eINSTANCE);

		cdse.registerTransformationRuleFactory(new AllocateRuleFactory());
		cdse.registerTransformationRuleFactory(new CreateResourceRuleFactory());
		cdse.registerTransformationRuleFactory(new MakeParallelRuleFactory());
		cdse.registerTransformationRuleFactory(new MakeSequentialRuleFactory());

//		cdse.registerGlobalConstraintFactory(new CreateGlobalConstraintFactory());
		
		cdse.registerObjectiveFactory(new CreateGuidanceObjectiveFactory());
		cdse.registerObjectiveFactory(new CreateCostsObjectiveFactory());
		cdse.registerObjectiveFactory(new ResourceUtilizationObjectiveFactory());
		cdse.registerObjectiveFactory(new ResponseTimeObjectiveFactory());
		
		cdse.setDesignspace(new ConcurrentDesignSpace());
		
		cdse.startExploration(new CreateDFS4Strategy());
		//
		//
		// List<String> nodes = new ArrayList<String>();
		// nodes.add("192.168.0.11:2552");
		//
		// ClusteredDesignSpaceExplorer cdse = new
		// ClusteredDesignSpaceExplorer(nodes);
		// cdse.addMetaModelPackage(SimplifiedbpmnPackage.eINSTANCE);
		//
		// cdse.setInitialModel(BpmnProblems.createWebShopProblem());
		//
		// System.out.println("Starting");
		// cdse.startExploration(Strategies.createDFSStrategy(4));
		// System.out.println("Started");

		while (true) {
			Thread.sleep(1000);
		}

		// return null;
	}

	@Override
	public void stop() {
		System.out.println("Stopped");
	}

}
