package org.processmining.plugins.HybridMiner;

import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.plugins.HybridMiner.loglist.LogMap;
import org.processmining.plugins.bpmn.plugins.BpmnExportPlugin;
import org.processmining.processtree.ProcessTree;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Map;


public class Main {

	public static void main ( String args[] ) {
		String	outputPath	= "./output/";
		PrintStream	printStream;
		XLog log;

		try {
			if ( args.length == 0 )
				log = XLogReader.openLog( "data/l1.mxml" );
//				log = XLogReader.openLog ( "data/s1_py.xes" );
//				log = XLogReader.openLog ( "data/s2_wo_prefix_events_py.xes" );
			else
				log = XLogReader.openLog( args[ 0 ] );

			printStream	= new PrintStream( outputPath + "output.txt" );

		} catch ( Exception e ) {
			System.out.println( "Exception : " + e.getMessage( ) );
//			e.printStackTrace( );
			return;
		}

		LogProcessor logProcessor = new LogProcessor( log, printStream );
		logProcessor.mine( );

		XLogWriter.saveXesGz( logProcessor.log, outputPath + "root" );
		for ( Map.Entry< String, XLog > entry : logProcessor.getChildLogs().entrySet() )
			XLogWriter.saveXesGz( entry.getValue( ), outputPath + entry.getKey( ) );

		ProcessTree pt = logProcessor.toProcessTree( );
		printStream.println( "\nProcess tree:" + pt.toString() );
		System.out.println( "\nProcess tree:" + pt.toString( ) );

		BPMNDiagram bpmn = ( BPMNDiagram ) new ProcessTree2BPMNConverter( ).convertToBPMN( pt, false )[ 0 ];
		try {
			new BpmnExportPlugin().export(
					logProcessor.pluginContext
					, bpmn, new File( outputPath + "model.bpmn" )
			);
		} catch ( Exception e ) {
			System.out.println( "Exception : " + e.getMessage( ) );
			printStream.println( "Exception : " + e.getMessage( ) );
//			e.printStackTrace( );
		}
		System.out.flush();
		printStream.flush();
		printStream.close();
	}

	@Plugin (
			name = "Mine a ProcessTree with HybridMiner"
			, parameterLabels = { "Log" }
			, returnLabels = { "ProcessTree" }
			, returnTypes = { ProcessTree.class }
			, userAccessible = true
			, help = "Produces the ProcessTree, which could be converted to BPMN or PetriNet"
	)
	@UITopiaVariant (affiliation = "University of Tartu", author = "Bogdan S", email = "bogdan89@ut.ee")
	@PluginVariant (variantLabel = "Mine a Process Tree, dialog", requiredParameterLabels = { 0 })
	public Object mineGuiProcessTree(PluginContext context, XLog log) {
		LogProcessor logProcessor = new LogProcessor( XLogReader.deepcopy( log ), System.out );
		logProcessor.pluginContext	= context;
		logProcessor.mine( );
		return logProcessor.toProcessTree( );
	}

	@Plugin (
			name = "Filter out branch logs with HybridMiner"
			, parameterLabels = { "Log" }
			, returnLabels = { "Root log", "Log Map"
			}
			, returnTypes = { XLog.class, LogMap.class }
			, userAccessible = true
			, help = "Produces the sub-logs, which represent branches of parallel block"
	)
	@UITopiaVariant (affiliation = "University of Tartu", author = "Bogdan S", email = "bogdan89@ut.ee")
	@PluginVariant (variantLabel = "Filter out branches, dialog", requiredParameterLabels = { 0 })
	public Object[] mineGuiLogList(PluginContext context, XLog log) {
		LogProcessor logProcessor = new LogProcessor( XLogReader.deepcopy( log ), System.out );
		logProcessor.pluginContext	= context;
		logProcessor.mine( );

		LogMap x = new LogMap ();
		x.put( "Root", logProcessor.log );
		x.putAll( logProcessor.getChildLogs( ) );
		return new Object[] { logProcessor.log, x };
	}

	@Plugin (
			name = "Filter out branches with HybridMiner"
			, parameterLabels = { "Log" }
			, returnLabels = { "Root log", "Branch_0", "Branch_1", "Branch_2", "Branch_3"
			, "Branch_4", "Branch_5", "Branch_6", "Branch_7"
			, "Branch_8", "Branch_9", "Branch_10", "Branch_11"
			, "Branch_12", "Branch_13", "Branch_14", "Branch_15"
	}
			, returnTypes = { XLog.class, XLog.class, XLog.class, XLog.class, XLog.class
			, XLog.class, XLog.class, XLog.class, XLog.class
			, XLog.class, XLog.class, XLog.class, XLog.class
			, XLog.class, XLog.class, XLog.class, XLog.class
	}
			, userAccessible = true
			, help = "Produces the sub-logs, which represent branches of parallel block"
	)
	@UITopiaVariant (affiliation = "University of Tartu", author = "Bogdan S", email = "bogdan89@ut.ee")
	@PluginVariant (variantLabel = "Filter out branches, dialog", requiredParameterLabels = { 0 })
	public Object[] mineGuiLogs(PluginContext context, XLog log) {
		LogProcessor logProcessor = new LogProcessor( XLogReader.deepcopy( log ), System.out );
		logProcessor.pluginContext	= context;
		logProcessor.mine( );
		ArrayList<XLog> result = new ArrayList<>(  );
		result.add( logProcessor.log );
		result.addAll( logProcessor.getChildLogs( ).values( ) );

		for ( int i = result.size(); i < 17 ; i++ ) {
			result.add( null );
		}
		return result.toArray();
	}

	@Plugin (name = "Mine BPMN with Hybrid Miner", returnLabels = { "BPMN model" }, returnTypes = { BPMNDiagram.class }, parameterLabels = { "Log" }, userAccessible = true)
	@UITopiaVariant (affiliation = "University of Tartu", author = "Bogdan S", email = "bogdan89@ut.ee")
	@PluginVariant (variantLabel = "Mine a BPMN, dialog", requiredParameterLabels = { 0 })
	public BPMNDiagram mineGuiBPMN(PluginContext context, XLog log) {
		LogProcessor logProcessor = new LogProcessor( XLogReader.deepcopy( log ), System.out );
		logProcessor.pluginContext	= context;
		logProcessor.mine( );
		return ( BPMNDiagram ) new ProcessTree2BPMNConverter( ).convertToBPMN( logProcessor.toProcessTree( ), false )[ 0 ];
	}
}
