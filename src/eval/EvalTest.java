package eval;

import generation.CompareTrees;
import generation.GenerateLog;
import generation.GenerateLogParameters;
import org.deckfour.xes.extension.std.XExtendedEvent;
import org.deckfour.xes.model.XLog;
import org.processmining.plugins.InductiveMiner.conversion.ReduceTree;
import org.processmining.plugins.InductiveMiner.plugins.IMProcessTree;
import org.processmining.plugins.PBMiner.LogProcessor;
import org.processmining.plugins.PBMiner.XLogReader;
import org.processmining.processtree.ProcessTree;

import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class EvalTest {

	public int samples = 100;
	public int tracesPerLog = 100;
	public int totalDistinctTraces;

	public ProcessTree psTree;
	public static PrintStream nullPrintStream = new NullPrintStream( );

	public double avgLogCompleteness;
	public int PBMinerEqualTrees, IMMinerEqualTrees;

	public int IMandPBEqualTrees, IMnorPBEqualTrees, IMnotPBEqualTrees, PBnotIMEqualTrees;
	public long avgMiningTimeIM, avgMiningTimePB;
	public int[] distinctCuts;

	public EvalTest( ProcessTree targetPsTree ) {
		this.psTree = targetPsTree;
	}

	public EvalTest( ProcessTree psTree, int tracesPerLog ) {
		this( psTree );
		if ( tracesPerLog > 0 )
			this.tracesPerLog = tracesPerLog;
	}

	public EvalTest( ProcessTree psTree, int tracesPerLog, int randomLogs ) {
		this( psTree, tracesPerLog );
		if ( randomLogs > 0 )
			this.samples = randomLogs;
	}

	public XLog generateRandomLog( ) throws Exception {
		GenerateLogParameters genParams = new GenerateLogParameters( this.tracesPerLog, System.nanoTime( ) );
		return new GenerateLog().generateLog( this.psTree, genParams );
	}

	public static long calcDistinctTraces( XLog log ) {
		return log.parallelStream( ).map( trace ->
						trace.stream( ).map( event -> {
									XExtendedEvent xe = new XExtendedEvent( event );
									return String.format( "%s+%s", xe.getName( ), xe.getTransition( ) );
								}
						).collect( Collectors.joining( "," ) )
		)
		.distinct( ).count( );
	}

	public static long calcDistinctTraces( XLog log, int from, int to ) {
		return log.parallelStream( ).map( trace ->
						trace.stream( ).skip( from ).limit( to - from ).map( event -> {
									XExtendedEvent xe = new XExtendedEvent( event );
									return String.format( "%s+%s", xe.getName( ), xe.getTransition( ) );
								}
						).collect( Collectors.joining( "," ) )
		)
		.distinct( ).count( );
	}

	public static long calcDistinctTraces( XLog log, int ...cutPos ) {
		if ( cutPos == null || cutPos.length == 0 )
			return calcDistinctTraces( log );

		List<Long> result = new LinkedList<>();
		for ( int i = 0, prevPos = 0, pos; i < cutPos.length; i ++ ) {
			pos = cutPos[ i ];
			result.add( calcDistinctTraces( log, prevPos, pos ) );
			prevPos = pos;
		}

		return result.stream().mapToLong( x -> x ).max().getAsLong();
	}

	public void reset() {
		this.avgLogCompleteness = 0;
		this.PBMinerEqualTrees = 0;
	}

	public void run() {
		GenerateLog logGenerator = new GenerateLog( );
		PrintStream printStream = System.out;

		List< EvalTestResult > calcResult = IntStream.range( 0, this.samples ).parallel( ).mapToObj(
				i -> {
					System.setOut( nullPrintStream );
					try {
						XLog log = logGenerator.generateLog( this.psTree
								, new GenerateLogParameters( this.tracesPerLog, System.nanoTime( ) )
						);

						LogProcessor lp = new LogProcessor( XLogReader.deepcopy( log ), nullPrintStream );
						long startTimeNs = System.nanoTime( );
						lp.mine( );
						ProcessTree PBTree = lp.toProcessTree( );
						ReduceTree.reduceTree( PBTree );

						long elapsedPB = System.nanoTime() - startTimeNs;
						startTimeNs = System.nanoTime( );

						ProcessTree IMTree = IMProcessTree.mineProcessTree( log, lp.inductiveMinerParams );
						long elapsedIM = System.nanoTime() - startTimeNs;

						return new EvalTestResult(
								( int ) calcDistinctTraces( log, this.distinctCuts )
								, CompareTrees.isLanguageEqual( this.psTree, PBTree )
								, CompareTrees.isLanguageEqual( this.psTree, IMTree )
								, elapsedPB, elapsedIM
						);

					} catch ( Exception e ) {
//						System.setOut( printStream );
						e.printStackTrace( );
					}
					return new EvalTestResult( );
				}
		).collect( Collectors.toList( ) );
		System.setOut( printStream );

		this.avgLogCompleteness = calcResult.parallelStream().mapToInt(
				EvalTestResult::getDistinctTraces
		).average().getAsDouble();

		this.PBMinerEqualTrees = calcResult.parallelStream().mapToInt( EvalTestResult::PBMinerResult ).sum();
		this.IMMinerEqualTrees = calcResult.parallelStream().mapToInt( EvalTestResult::IMMinerResult ).sum();
		this.avgMiningTimePB = ( long )( calcResult.parallelStream().mapToLong( o -> o.PBMiningTimeNs ).average().getAsDouble() / 1e6 );
		this.avgMiningTimeIM = ( long )( calcResult.parallelStream().mapToLong( o -> o.IMMiningTimeNs ).average().getAsDouble() / 1e6 );

		this.IMandPBEqualTrees = calcResult.parallelStream().mapToInt( o -> o.IMMinerResult && o.PBMinerResult ? 1 : 0 ).sum();
		this.IMnorPBEqualTrees = calcResult.parallelStream().mapToInt( o -> ! o.IMMinerResult && ! o.PBMinerResult ? 1 : 0 ).sum();
		this.IMnotPBEqualTrees = calcResult.parallelStream().mapToInt( o -> o.IMMinerResult && ! o.PBMinerResult ? 1 : 0 ).sum();
		this.PBnotIMEqualTrees = calcResult.parallelStream().mapToInt( o -> o.PBMinerResult && ! o.IMMinerResult ? 1 : 0 ).sum();
	}

	public String toString( ) {
		return String.format( "\n[ %d x %d = %d ]"
				, this.tracesPerLog, this.samples, this.tracesPerLog * this.samples
		) + String.format( "\nPB-Miner equal trees: %d / %d = %.1f%%\tavg mining time: %dms"
				, this.PBMinerEqualTrees, this.samples, 100.0 * this.PBMinerEqualTrees / this.samples, this.avgMiningTimePB
		) + String.format( "\nIM Miner equal trees: %d / %d = %.1f%%\tavg mining time: %dms"
				, this.IMMinerEqualTrees, this.samples, 100.0 * this.IMMinerEqualTrees / this.samples, this.avgMiningTimeIM
		) + String.format( "\nAvg log completeness: %.2f / %d = %.1f%%"
				, this.avgLogCompleteness, this.totalDistinctTraces, 100.0 * this.avgLogCompleteness / this.totalDistinctTraces
		) + String.format( "\n\nIM\\PB\tT\tF\nTRUE\t%d\t%d\nFALSE\t%d\t%d"
				, this.IMandPBEqualTrees, this.IMnotPBEqualTrees
				, this.PBnotIMEqualTrees, this.IMnorPBEqualTrees
		);
	}
}

