package org.processmining.plugins.PBMiner;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.cli.CLIContext;
import org.processmining.contexts.cli.CLIPluginContext;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.plugins.InductiveMiner.mining.MiningParameters;
import org.processmining.plugins.InductiveMiner.mining.MiningParametersIMin;
import org.processmining.plugins.InductiveMiner.plugins.IMProcessTree;
import org.processmining.plugins.PBMiner.classifiers.HeuristicEventsClassifier;

import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;

public class BranchProcessor {
	public XLog log;
	public PrintStream printStream;
	public PluginContext pluginContext;
	public XEventClassifier xEventClassifier;
	public MiningParameters inductiveMinerParams;

	public ContextAnalysis contextAnalysis;
	public List< Set< String > > parallelBranches;
	public Map< String, Set< Integer > > eventToBranch;

	public BranchProcessor( XLog xLog ) {
		this.log = xLog;
		this.printStream = System.out;
		this.pluginContext = new CLIPluginContext( new CLIContext( ), "PB-Miner" );
		this.xEventClassifier = LogProcessor.defaultXEventClassifier;

		//	Inductive Miner - incompleteness
		this.inductiveMinerParams = new MiningParametersIMin( );
		this.inductiveMinerParams.setClassifier( this.xEventClassifier );
		this.inductiveMinerParams.setUseMultithreading( false );
	}

	public BranchProcessor( XLog xLog, PrintStream ps ) {
		this( xLog );
		if ( ps != null )
			this.printStream = ps;
	}

	public List< Set< String > > ExtractParallelBranches() {
		this.contextAnalysis = new ContextAnalysis( this.log );
		this.contextAnalysis.AnalyzeSuccPred( );

//		Find start events
		this.parallelBranches = BlockProcessor.ExtractParallelBranches(
				IMProcessTree.mineProcessTree(
						XLogReader.filterByEvents(
								this.log
								, this.contextAnalysis.successors.get( this.contextAnalysis.traceStartPseudoEvent )
						), this.inductiveMinerParams
				).getRoot( )
		);

//		Merge into 1 branch & mark Declarative
		if ( this.parallelBranches.size() > LogProcessor.DeclarativeBranchesThreshold ) {
			Set< String > result = new HashSet<>(  );
			result.add( LogProcessor.DeclarativePseudoEvent );

			for ( Set< String > events : this.parallelBranches )
				result.addAll( events );

			this.parallelBranches.clear();
			this.parallelBranches.add( result );
		}

		return this.ExtendAllBranchEvents( this.parallelBranches );
	}

	public List< Set< String > > ExtendAllBranchEvents( List< Set< String > > branchStartEvents ) {
		if ( this.contextAnalysis == null ) {
			this.contextAnalysis = new ContextAnalysis( this.log );
			this.contextAnalysis.AnalyzeSuccPred( );
		}
		this.eventToBranch = new HashMap<>( );

//		Fill start events
		for ( int i = 0, size = branchStartEvents.size( ) ; i < size ; i++ )
			for ( String event : branchStartEvents.get( i ) )
				eventToBranch.put( event, new HashSet<>( Arrays.asList( i ) ) );

		XLog blockLog = XLogReader.filterByEvents( this.log, FindBlockEvents( eventToBranch.keySet(), this.contextAnalysis.predecessors ) );
		Set<String> blockStartEvents = branchStartEvents.stream().flatMap( Collection:: stream ).collect( Collectors.toSet() );

//		Mark events with a single branch opened
		blockLog.stream()
			.forEach( trace -> {
				Set< Integer > openedBranches = new HashSet<>( );
				trace.stream( ).forEach( event -> {
					String eventName = this.contextAnalysis.fetchName( event );

					if ( ! eventToBranch.containsKey( eventName ) ) {
						eventToBranch.put( eventName, new HashSet<>( openedBranches ) );
						return;
					}
					if ( blockStartEvents.contains( eventName ) ) {
						openedBranches.addAll( eventToBranch.get( eventName ) );
						return;
					}

					eventToBranch.get( eventName ).retainAll( openedBranches );
				});
			});

//		Put events, with empty branch candidates, into a separate branch
		Set< String > eventsEmptyBranchCandidates = eventToBranch.entrySet( ).stream( )
				.filter( entry -> entry.getValue( ).isEmpty( ) )
				.map( Map.Entry:: getKey )
				.collect( Collectors.toSet( ) );

		if ( ! eventsEmptyBranchCandidates.isEmpty() ) {
			int newBranchIndex = branchStartEvents.size();
			branchStartEvents.add( eventsEmptyBranchCandidates );
			eventsEmptyBranchCandidates.stream( ).forEach( x -> eventToBranch.get( x ).add( newBranchIndex ) );
		}

		printStream.println( "Events to branches:" );
		printStream.println( eventToBranch );

//		Tail events - belong to all branches
		processTailEvents( branchStartEvents.size() );

		for ( String event : eventToBranch.keySet() )
			branchStartEvents.get( eventToBranch.get( event ).iterator().next() ).add( event );

		return branchStartEvents;
	}

	public void processTailEvents( int branchesSize ) {
		Set< String > tailEvents = new HashSet<>( );

//		Remove events which belong to all parallelBranches or belong to none
		for ( String event : new HashSet<>( eventToBranch.keySet( ) ) ) {
			if ( eventToBranch.get( event ).size( ) == branchesSize ) {
				tailEvents.add( event );
				eventToBranch.remove( event );
			}
			else if ( eventToBranch.get( event ).isEmpty( ) )
				eventToBranch.remove( event );
		}

		eventToBranch.putAll(
				new HeuristicEventsClassifier( xEventClassifier, pluginContext )
						.mapEventsToBranches(
								log, eventToBranch
								, findParallelEvents( eventToBranch, tailEvents, this.contextAnalysis.predecessors )
						)
		);

//		tail events which have all the predecessors in the same branch should also belong to that branch
		for ( String event : tailEvents ) {
			if ( eventToBranch.containsKey( event ))
				continue;

			Set< Integer >predecessorBranches	= new HashSet<>(  );
			for ( String predecessor : this.contextAnalysis.predecessors.getOrDefault( event, new HashSet<String>(  ) ))
				predecessorBranches.addAll( eventToBranch.getOrDefault( predecessor, new HashSet< Integer>(   ) ) );

			if ( predecessorBranches.size() == 1 )
				eventToBranch.put( event, predecessorBranches );
		}
	}

	/**
	 * Find all predecessor events of known branched events - those are also branched events
	 */
	public static Set< String > findParallelEvents( Map< String, Set< Integer > > eventToBranch, Set< String > tailEvents, Map< String, Set< String > > predecessors ) {
		Set< String > processedEvents = new HashSet<>();
		LinkedList< String > fringe = new LinkedList<>( eventToBranch.keySet( ) );

		while ( ! fringe.isEmpty( ) ) {
			String event = fringe.pop( );
			Set< String > predecessorEvents = predecessors.getOrDefault( event, new HashSet< String >( ) );

			processedEvents.add( event );
			predecessorEvents.removeAll( processedEvents );
			fringe.addAll( predecessorEvents );
		}
		processedEvents.retainAll( tailEvents );
		return processedEvents;
	}


	public static Set< String > FindBlockEvents( Set< String > startEvents, Map< String, Set< String > > predecessors ) {
		Set< String > processedEvents = new HashSet<>();
		LinkedList< String > fringe = new LinkedList<>( startEvents );

		while ( ! fringe.isEmpty( ) ) {
			String event = fringe.pop( );
			Set< String > predecessorEvents = predecessors.getOrDefault( event, new HashSet< String >( ) );

			processedEvents.add( event );
			predecessorEvents.removeAll( processedEvents );
			fringe.addAll( predecessorEvents );
		}
		return processedEvents;
	}
}
