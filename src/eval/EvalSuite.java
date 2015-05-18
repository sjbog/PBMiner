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
import java.util.stream.IntStream;

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
		this.run( IntStream.iterate( startTraces, i -> i + step ).limit( 1 + ( endTraces - startTraces ) / step ) );
	}

	public void run( IntStream intStream ) {
		this.results = intStream.mapToObj( i -> {
					System.out.println( i );
					EvalTest test = new EvalTest( psTree, i, this.randomLogs );
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
				, "min_PB_mining_time_ms", "min_IM_mining_time_ms"
				, "max_PB_mining_time_ms", "max_IM_mining_time_ms"
		);
		csvFile.println( header.stream().collect( Collectors.joining(",") ) );
		csvFile.println(
				this.results.stream( ).map( x ->
					String.format( "%d,%.1f,%.1f,%.1f,%.1f,%d,%d,%d,%d,%d,%d"
							, x.tracesPerLog
							, x.avgLogCompleteness
							, 100.0 * x.avgLogCompleteness / x.totalDistinctTraces

							, 100.0 * x.PBMinerEqualTrees / x.samples
							, 100.0 * x.IMMinerEqualTrees / x.samples

							, x.avgMiningTimePB, x.avgMiningTimeIM
							, x.minMiningTimePB, x.minMiningTimeIM
							, x.maxMiningTimePB, x.maxMiningTimeIM
					)
			).collect( Collectors.joining( "\n" ) )
		);

		csvFile.flush();
		csvFile.close( );
	}
}