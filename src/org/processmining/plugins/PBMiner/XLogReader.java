package org.processmining.plugins.PBMiner;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XExtendedEvent;
import org.deckfour.xes.factory.XFactory;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.in.*;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

import java.io.File;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class XLogReader {
	public static XFactory factory = XFactoryRegistry.instance( ).currentDefault( );
	public static XConceptExtension xConceptExtentionInstance = XConceptExtension.instance( );

	public static XLog openLog( String inputLogFileName ) throws Exception {
		XLog log = null;
		XParser parser;
		File inputFile = new File( inputLogFileName );

		if ( inputLogFileName.toLowerCase( ).contains( "mxml.gz" ) ) {
			parser = new XMxmlGZIPParser( );
		} else if ( inputLogFileName.toLowerCase( ).contains( "mxml" ) || inputLogFileName.toLowerCase( ).contains( "xml" ) ) {
			parser = new XMxmlParser( );
		} else if ( inputLogFileName.toLowerCase( ).contains( "xes.gz" ) ) {
			parser = new XesXmlGZIPParser( );
		} else if ( inputLogFileName.toLowerCase( ).contains( "xes" ) ) {
			parser = new XesXmlParser( );
		} else {
			throw new Exception( "Unknown log format" );
		}
		if ( parser.canParse( inputFile ) )
			try {
				log = parser.parse( inputFile ).get( 0 );
			} catch ( Exception e ) {
				e.printStackTrace( );
			}
		if ( log == null )
			throw new Exception( "Couldn't read log file" );
		return log;
	}

	public static XLog sliceLastN( XLog log, int n ) {
		List< XTrace > tmp_sub_log = log.subList( log.size( ) - n, log.size( ) );

		log = factory.createLog( ( XAttributeMap ) log.getAttributes( ).clone() );
		log.addAll( tmp_sub_log );

		return log;
	}

	public static XLog filterEventsFn( XLog log, Predicate<XEvent> fn ) {
		XLog filteredLog	= factory.createLog( ( XAttributeMap ) log.getAttributes( ).clone( ) );
		filteredLog.addAll(
				log.parallelStream()
						.map( trace -> {
							XTrace filteredTrace = factory.createTrace( ( XAttributeMap ) trace.getAttributes( ).clone( ) );
							filteredTrace.addAll(
									trace.stream( ).filter( fn )
											.map( event -> factory.createEvent( ( XAttributeMap ) event.getAttributes( ).clone( ) ) )
											.collect( Collectors.toList( ) )
							);
							return filteredTrace;
						} )
						.filter( filteredTrace -> filteredTrace.size( ) > 0 )
						.collect( Collectors.toList( ) )
		);
		return filteredLog;
	}

	public static XLog filterByEvents( XLog log, Set< String > targetEvents ) {
		XLogInfo logInfo	= XLogInfoImpl.create( log, LogProcessor.defaultXEventClassifier );
		return filterEventsFn( log, e -> targetEvents.contains( fetchName( e, logInfo ) ) );
	}

	public static XLog filterByEvents( XLog log, String eventNamePrefix ) {
		XLogInfo logInfo	= XLogInfoImpl.create( log, LogProcessor.defaultXEventClassifier );
		return filterEventsFn( log, event -> fetchName( event, logInfo ).startsWith( eventNamePrefix ) );
	}

	public static XLog filterSkipEvents( XLog log, Set< String > targetEvents ) {
		XLogInfo logInfo	= XLogInfoImpl.create( log, LogProcessor.defaultXEventClassifier );
		return filterEventsFn( log, event -> ! targetEvents.contains( fetchName( event, logInfo ) ) );
	}

	public static XLog filterTracesWithoutEvents( XLog log, Set< String > targetEvents ) {
		XLog filteredLog	= factory.createLog( ( XAttributeMap ) log.getAttributes().clone() );
		XLogInfo logInfo	= XLogInfoImpl.create( log, LogProcessor.defaultXEventClassifier );
		XTrace filteredTrace;

		for ( XTrace trace : log ) {
			filteredTrace	= factory.createTrace ( ( XAttributeMap ) trace.getAttributes ().clone() );
			boolean addTrace	= true;

			for ( XEvent event : trace ) {
				if ( targetEvents.contains( fetchName( event, logInfo ) ) ) {
					addTrace	= false;
					break;
				}
				filteredTrace.add( event );
			}
			addTrace	= addTrace && ( filteredTrace.size() > 0 );
			if ( addTrace )
				filteredLog.add( filteredTrace );
		}
		return filteredLog;
	}

	public static XLog filterContainingEvents( XLog log, Set< String > targetEvents ) {
		XLog filteredLog	= factory.createLog( ( XAttributeMap ) log.getAttributes().clone() );
		XLogInfo logInfo	= XLogInfoImpl.create( log, LogProcessor.defaultXEventClassifier );
		XTrace filteredTrace;

		for ( XTrace trace : log ) {
			filteredTrace	= factory.createTrace ( ( XAttributeMap ) trace.getAttributes ().clone() );
			boolean addTrace	= false;

			for ( XEvent event : trace ) {
				addTrace	= addTrace || ( targetEvents.contains( fetchName( event, logInfo ) ) );
				filteredTrace.add( event );
			}
			addTrace	= addTrace && ( filteredTrace.size() > 0 );
			if ( addTrace )
				filteredLog.add( filteredTrace );
		}
		return filteredLog;
	}

	public static XLog deepcopy( XLog log ) {
		Stream< XTrace > data = log.parallelStream( )
			.map( trace -> {
				XTrace traceCopy = factory.createTrace( ( XAttributeMap ) trace.getAttributes().clone() );
				traceCopy.addAll(
						trace.stream( )
							.map( event -> factory.createEvent( ( XAttributeMap ) event.getAttributes().clone() ))
							.collect( Collectors.toList() )
				);
				return traceCopy;
			})
			.filter( trace -> trace.size() > 0 );

		XLog logCopy	= factory.createLog( ( XAttributeMap ) log.getAttributes().clone() );
		logCopy.addAll( data.collect( Collectors.toList() ) );
		return logCopy;
	}

	public static XLog filterRemoveByEvents( XLog log, Set< String > targetEvents, String eventNameReplacement ) {
		XLog filteredLog	= factory.createLog( ( XAttributeMap ) log.getAttributes().clone() );
		XLogInfo logInfo	= XLogInfoImpl.create( log, LogProcessor.defaultXEventClassifier );
		XTrace filteredTrace;

		for ( XTrace trace : log ) {
			filteredTrace	= factory.createTrace ( ( XAttributeMap ) trace.getAttributes ().clone() );
			LinkedList< Integer > indicesToRemove = new LinkedList<>( );

			for ( int i = 0, traceSize = trace.size( ) ; i < traceSize ; i++ ) {
				XEvent event = trace.get( i );

				if ( targetEvents.contains( fetchName( event, logInfo ) ) )
					if ( filteredTrace.isEmpty( ) ) {
						filteredTrace.add( ( XEvent ) event.clone( ) );
						event = ( XEvent ) event.clone( );
						XExtendedEvent.wrap( event ).setName( eventNameReplacement );
						XExtendedEvent.wrap( event ).setTransition( "complete" );
						trace.set( i, event );
					} else {
						filteredTrace.add( event );
						indicesToRemove.add( i );
					}
			}
			if ( filteredTrace.size() > 0 ) {
				filteredLog.add( filteredTrace );
				Collections.reverse( indicesToRemove );
				for ( int i : indicesToRemove )
					trace.remove( i );
			}
		}
		return filteredLog;
	}

	public static XLog logWithoutRepeatEvents( XLog log, String eventNamePrefix, String loopPlaceholderName ) {
		XLog filteredLog	= factory.createLog( ( XAttributeMap ) log.getAttributes( ).clone( ) );
		XLogInfo logInfo	= XLogInfoImpl.create( log, LogProcessor.defaultXEventClassifier );
		XTrace filteredTrace;

		for ( XTrace trace : log ) {
			filteredTrace	= factory.createTrace ( ( XAttributeMap ) trace.getAttributes ().clone() );
			HashSet< String > eventsToRemove = new HashSet<>(  );
			boolean isPlaceholderUsed = false;

			for ( int i = 0, traceSize = trace.size( ) ; i < traceSize ; i++ ) {
				XEvent event = trace.get( i );
				String eventName = fetchName( event, logInfo );

				if ( eventName.startsWith( eventNamePrefix ) ) {
					if ( eventsToRemove.contains( eventName ) ) {
						if ( isPlaceholderUsed )
							continue;
						event = ( XEvent ) event.clone( );
						XExtendedEvent.wrap( event ).setName( loopPlaceholderName );
						XExtendedEvent.wrap( event ).setTransition( "complete" );
						isPlaceholderUsed = true;
					}
					else
						eventsToRemove.add( eventName );
				}
				filteredTrace.add( event );
			}
			if ( filteredTrace.size() > 0 )
				filteredLog.add( filteredTrace );
		}
		return filteredLog;
	}

	public static void findSameTimestampEvents( XLog log ) {
		Map< List< String >, Long > result =
			log.stream( ).flatMap(
				trace ->
						trace.stream( )
								.collect( Collectors.groupingBy( e -> XExtendedEvent.wrap( e ).getTimestamp( ) ) )
								.entrySet( ).stream( )
								.filter( e -> e.getValue( ).size( ) > 1 )
								.map(
										eventList -> eventList.getValue( )
												.stream( )
												.map( e -> XExtendedEvent.wrap( e ).getName( ) )
												.collect( Collectors.toList( ) )
								)
//							.collect( Collectors.toList( ) )
		).collect(
				Collectors.groupingBy( x -> x, Collectors.counting( ) )
		);

		result.entrySet().forEach( es ->
				System.out.println( String.format( "%s\t%s", es.getValue(), es.getKey( ) ) )
		);
	}

	public static XLog fixOLogTimingOrder( XLog log ) {
//		Collectors.groupingBy( ( i, e ) ->  )
		XLog filteredLog	= factory.createLog( ( XAttributeMap ) log.getAttributes( ).clone( ) );
		XLogInfo logInfo	= XLogInfoImpl.create( log, LogProcessor.defaultXEventClassifier );
		XTrace filteredTrace;

		for ( XTrace trace : log ) {
			filteredTrace	= factory.createTrace ( ( XAttributeMap ) trace.getAttributes ().clone() );
			XEvent prevEvent = null;

			for ( int i = 0, traceSize = trace.size( ) ; i < traceSize ; i++ ) {
				XEvent event = trace.get( i );

				if ( fetchName( event, logInfo ).startsWith( "O_CANCELLED" ) &&
						fetchName( prevEvent, logInfo ).startsWith( "O_SELECTED" ) ) {
					filteredTrace.add( i-1, event );
				}
				else
					filteredTrace.add( event );

				prevEvent	= event;
			}
			if ( filteredTrace.size() > 0 )
				filteredLog.add( filteredTrace );
		}
		return filteredLog;
	}

	public static String fetchName( XEvent event, XLogInfo logInfo ) {
		return logInfo.getEventClasses().getClassOf( event ).toString();
	}

	public static List< List< List< String > > > divideLog( List< XTrace > log, Set< String > structured_events, Set< String > unstructured_events ) {
		LinkedList< List< String > > structured_seq = new LinkedList<>( );
		LinkedList< List< String > > unstructured_seq = new LinkedList<>( );

		boolean is_structured_seq = true;
		ArrayList< String > curr_seq = new ArrayList<>( );

		for ( XTrace trace : log ) {
			for ( int i = 0, size = trace.size( ) ; i < size ; i++ ) {

				XExtendedEvent event = XExtendedEvent.wrap( trace.get( i ) );
				String event_name = event.getName( );

				if ( structured_events.contains( event_name ) ) {
					if ( is_structured_seq ) {
						curr_seq.add( event_name );
					} else {
						unstructured_seq.add( curr_seq );
						curr_seq = new ArrayList<>( );
						is_structured_seq = true;
					}
				} else {
					if ( ! is_structured_seq ) {
						curr_seq.add( event_name );
					} else {
						structured_seq.add( curr_seq );
						curr_seq = new ArrayList<>( );
						is_structured_seq = false;
					}
				}
			}
			if ( is_structured_seq ) {
				structured_seq.add( curr_seq );
			} else {
				unstructured_seq.add( curr_seq );
			}
		}
		List< List< List< String > > > result = new LinkedList<>( );
		result.add( structured_seq );
		result.add( unstructured_seq );

		return result;
	}
}
