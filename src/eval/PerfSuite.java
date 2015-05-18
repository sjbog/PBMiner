package eval;

import com.google.common.base.Strings;
import org.processmining.processtree.ProcessTree;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PerfSuite
{
	public Collection< ProcessTree > psTrees;
	public int randomLogs;
	public Collection< EvalSuite > results;
	public Collection< Integer > tracePoints;

	public PerfSuite( int randomLogs, Collection< ProcessTree > psTrees  ) {
		this.psTrees = psTrees;
		this.randomLogs = randomLogs;
		this.results = new LinkedList<>(  );
	}

	public void run( IntStream intStream ) {
		this.run( intStream.boxed().collect( Collectors.toList() ) );
	}

	public void run( Collection< Integer > tracePoints ) {
		this.tracePoints = tracePoints;
		this.results = this.psTrees.stream().map( tree -> {
					EvalSuite testSuite = new EvalSuite( tree, 100, this.randomLogs );
					testSuite.run( tracePoints.stream( ).mapToInt( x -> x ) );
					return testSuite;
				}
		).collect( Collectors.toList() );
	}

//	public String toString() {
//		return String.format( "Process Tree: %s\n%s\n\nNumber of tests ran: %d\nTest output format: [ Traces x Samples ( Random logs ) = Total traces ]\n"
//				, this.psTrees.getName( ), this.psTrees, this.results.size()
//		) + this.results.stream( ).map( Object::toString ).collect( Collectors.joining( "\n" ) );
//	}

	public void toCSV( String filePath ) throws IOException {
		PrintStream csvFile = new PrintStream( new FileOutputStream( filePath ), true );
		List< String > header = this.psTrees.stream().map( x -> String.format( "avg_%s_mining_time_ms", x.getName( ).replaceAll( " ", "_" ) ) ).collect( Collectors.toList( ) );

		String linePlaceholder = "%s" + Strings.repeat( ",%d", header.size( ) );
		header.add( 0, "traces" );

		csvFile.println( header.stream().collect( Collectors.joining(",") ) );

		LinkedList< LinkedList< Long > > times = this.results.stream()
				.map( x -> x.results.stream( ).mapToLong( r -> r.avgMiningTimePB ).boxed( ).collect( Collectors.toCollection( LinkedList::new ) ) )
				.collect( Collectors.toCollection( LinkedList::new ) )
				;

		csvFile.println(
				this.tracePoints.stream( ).map( x ->
								x + "," + times.stream( ).map( v -> v.pollFirst( ).toString( ) ).collect( Collectors.joining( "," ) )
				).collect( Collectors.joining( "\n" ) )
		);

		csvFile.flush();
		csvFile.close( );
	}
}