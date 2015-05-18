package eval;

import org.processmining.plugins.InductiveMiner.conversion.ReduceTree;
import org.processmining.processtree.ProcessTree;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class EvalSuite
{
	public ProcessTree psTree;
	public int totalDistinctTraces, randomLogs;
	public Collection< EvalTest > results;
	public int[] distinctCuts;

	public EvalSuite( ProcessTree psTree, int totalDistinctTraces, int randomLogs ) {
		this.psTree = psTree;
		ReduceTree.reduceTree( this.psTree );
		this.randomLogs = randomLogs;
		this.totalDistinctTraces = totalDistinctTraces;
		this.results = new LinkedList<>(  );
	}

	public void run( int startTraces, int endTraces, int step ) {
		List< Integer > tests = new LinkedList<>( );
		for ( int i = startTraces; i <= endTraces; i += step ) {
			tests.add( i );
		}
		this.results = tests.stream( ).map( i -> {
					EvalTest test = new EvalTest( psTree, i, this.randomLogs );
					if ( i % 20 == 0 )
						System.out.println( "." );
					else
						System.out.print( "." );

					test.distinctCuts = this.distinctCuts;
					test.run( );
					test.totalDistinctTraces = this.totalDistinctTraces;
					return test;
				}
		).collect( Collectors.toList() );
	}

	public String toString() {
		return String.format( "Process Tree: %s\n%s\n\nNumber of tests ran: %d\nTest output format: [ Traces x Samples ( Random logs ) = Total traces ]\n"
				, this.psTree.getName( ), this.psTree, this.results.size()
		) + this.results.stream( ).map( Object::toString ).collect( Collectors.joining( "\n" ) );
	}

	public void toCSV( String filePath ) throws IOException {
		PrintStream csvFile = new PrintStream( new FileOutputStream( filePath ), true );
		List< String > header = Arrays.asList(
				"total_traces", "avg_distinct_traces", "avg_log_completeness_percent"
				, "PB_discovery_percent", "IM_discovery_percent"
				, "avg_PB_mining_time_ms", "avg_IM_mining_time_ms"
		);
		csvFile.println( header.stream().collect( Collectors.joining(",") ) );
		csvFile.println(
				this.results.stream( ).map( x ->
					String.format( "%d,%.1f,%.1f,%.1f,%.1f,%d,%d"
							, x.tracesPerLog
							, x.avgLogCompleteness
							, 100.0 * x.avgLogCompleteness / x.totalDistinctTraces

							, 100.0 * x.PBMinerEqualTrees / x.samples
							, 100.0 * x.IMMinerEqualTrees / x.samples

							, x.avgMiningTimePB, x.avgMiningTimeIM
					)
			).collect( Collectors.joining( "\n" ) )
		);

		csvFile.flush();
		csvFile.close( );
	}
}