package eval;

import org.processmining.processtree.ProcessTree;

import java.io.PrintStream;

public class Main {

	public static void main( String args[] ) throws Exception {
		String outputPath = "./output/eval/";
		PrintStream printStream = System.out;
//		PrintStream printStream = new FileInputStream( outputPath + "output.txt" );

		String dataPath = "C:/Users/Alle/Study/4/m/docs/data/Eval/s2/";

		ProcessTree psTree = ProcessTreeFactory.ReadProcessTreeFromFile( dataPath + "pstree.ptml" );
//		ReduceTree.reduceTree( psTree );
//		SaveProcessTreeToFile( dataPath + "s4.ptml", psTree );

		/*GenerateLogParameters genParams = new GenerateLogParameters( 20, System.nanoTime( ) );
		XLog log = new GenerateLog( ).generateLog( psTree, genParams );

		System.setOut( nullPrintStream );
		LogProcessor lp = new LogProcessor( XLogReader.deepcopy( log ));
		lp.mine( );
		ProcessTree mTree = lp.toProcessTree( );
		System.setOut( printStream );

		printStream.println( "\nMined PS Tree:" );
		printStream.println( mTree );

		printStream.println( String.format( "\nAre equal? - %s",
				CompareTrees.isLanguageEqual( psTree, mTree )
		) );
		*/

		printStream = new PrintStream( outputPath + "s2_eval_suite.txt" );
		EvalSuite s2_test = new EvalSuite( psTree, 840, 10 );
		s2_test.run( 10, 1000, 10 );
		printStream.println( s2_test );
		s2_test.toCSV( outputPath + "s2_eval_suite.csv" );

//		printStream = new PrintStream( outputPath + "s4_v4_eval_suite.txt" );
//		EvalSuite s4_test = new EvalSuite( psTree, 280, 100 );
//		s4_test.run( 10, 1000, 10 );
//		printStream.println( s4_test );
//		s4_test.toCSV( outputPath + "s4_v4_eval_suite.csv" );
	}

	/*
	Traces = 10 - 10,000
	Samples / random logs = x30-x50 (x100)
	Lvl of incompleteness = Unique traces: 280 / 10000
			( 280 + ... ) / ( 280 x100 )
			10 / 280

			L1: 73 / 280 - false
			L2: 76 / 280 - true
			L3: 75 / 280 - true
			...

			( 73 + 76 ) / ( 2x280 ) =
				74 / 280 = 26.4%

						2 / 3 = 75%

Equal trees: 33 / 100
Avg completeness: 69.830000 / 280

Equal trees: 39 / 100
Avg completeness: 70.500000 / 280

PB Equal trees: 399 / 1000
IM Equal trees: 870 / 1000
Avg completeness: 70.154000 / 280

PB Equal trees: 49 / 100
IM Equal trees: 84 / 100
Avg log completeness: 70.200000 / 280

PB Equal trees: 713 / 1000
IM Equal trees: 993 / 1000
Avg log completeness: 87.818000 / 280

IM\PB	T	F
TRUE	708	285
FALSE	5	2
	*/
}
