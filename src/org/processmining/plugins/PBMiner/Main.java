package org.processmining.plugins.PBMiner;

import org.deckfour.xes.model.XLog;
import org.processmining.contexts.cli.CLIContext;
import org.processmining.contexts.cli.CLIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.packages.PackageDescriptor;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.plugins.PBMiner.loglist.LogMap;
import org.processmining.plugins.bpmn.plugins.BpmnExportPlugin;
import org.processmining.processtree.ProcessTree;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;


public class Main {

	public static PluginContext pluginContext = new CLIPluginContext( new CLIContext( ), "PB-Miner" );
	public static PackageDescriptor pkg = new PackageDescriptor( "CLI PBMiner", "0.1", PackageDescriptor.OS.ALL, "", "UT",
			"BS", "", "", "", "", false, false, new LinkedList< String >( ), new LinkedList< String >( )
	);

	public static void main ( String args[] ) {
		String	outputPath	= "./output/";
		PrintStream	printStream;
		XLog log;

		try {
			if ( args.length == 0 )
//				log = XLogReader.openLog( "data/l1.mxml" );
				log = XLogReader.openLog ( "../docs/data/limitations/se.xes" );
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
			name = "Mine a ProcessTree with PB-Miner"
			, parameterLabels = { "Log" }
			, returnLabels = { "ProcessTree" }
			, returnTypes = { ProcessTree.class }
			, userAccessible = true
			, help = "Mine complex parallel structures with Parallel Branch Miner, produces the ProcessTree"
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
			name = "Filter out branch logs as LogMap with PB-Miner"
			, parameterLabels = { "Log" }
			, returnLabels = { "Root log", "Log Map"
			}
			, returnTypes = { XLog.class, LogMap.class }
			, userAccessible = true
			, help = "Mine complex parallel structures with Parallel Branch Miner, produces the sub-logs"
	)
	@UITopiaVariant (affiliation = "University of Tartu", author = "Bogdan S", email = "bogdan89@ut.ee")
	@PluginVariant (variantLabel = "Filter out branches, dialog", requiredParameterLabels = { 0 })
	public Object[] mineGuiLogMap(PluginContext context, XLog log) {
		LogProcessor logProcessor = new LogProcessor( XLogReader.deepcopy( log ), System.out );
		logProcessor.pluginContext	= context;
		logProcessor.mine( );

		LogMap x = new LogMap ();
		x.put( "Root", logProcessor.log );
		x.putAll( logProcessor.getChildLogs( ) );
		return new Object[] { logProcessor.log, x };
	}

	@Plugin (
			name = "Filter out branch logs with PB-Miner"
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
			, help = "Mine complex parallel structures with Parallel Branch Miner, produces the sub-logs"
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

	@Plugin (name = "Mine BPMN with PB-Miner", returnLabels = { "BPMN model" }, returnTypes = { BPMNDiagram.class }, parameterLabels = { "Log" }, userAccessible = true
		, help = "Mine complex parallel structures with Parallel Branch Miner, produces the BPMN model"
	)
	@UITopiaVariant (affiliation = "University of Tartu", author = "Bogdan S", email = "bogdan89@ut.ee")
	@PluginVariant (variantLabel = "Mine a BPMN, dialog", requiredParameterLabels = { 0 })
	public BPMNDiagram mineGuiBPMN(PluginContext context, XLog log) {
		LogProcessor logProcessor = new LogProcessor( XLogReader.deepcopy( log ), System.out );
		logProcessor.pluginContext	= context;
		logProcessor.mine( );
		return ( BPMNDiagram ) new ProcessTree2BPMNConverter( ).convertToBPMN( logProcessor.toProcessTree( ), false )[ 0 ];
	}
}
