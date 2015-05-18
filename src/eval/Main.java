package eval;

import org.processmining.processtree.ProcessTree;

import java.io.PrintStream;

public class Main {

	public static void main( String args[] ) throws Exception {
		/*
		ProcessTree psTree;
		psTree = ProcessTreeFactory.ReadProcessTreeFromFile( dataPath + "pstree.ptml" );
//		ReduceTree.reduceTree( psTree );
//		SaveProcessTreeToFile( dataPath + "s4.ptml", psTree );

		GenerateLogParameters genParams = new GenerateLogParameters( 20, System.nanoTime( ) );
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
//		runTest( "../docs/data/Eval/s1/", 20, 1000, 2, 50, 1, new int[]{ 6, 15 } );
//		runTest( "../docs/data/Eval/s2/", 2240, 100, 10, 1000, 10, new int[]{ 6, 15 } );
//		runTest( "../docs/data/Eval/s4/", 280, 100, 10, 1000, 10, null );
	}

	public static void runTest( String dataDirPath, int totalDistinctTraces, int samples, int start, int stop, int step, int ...distinctCuts ) throws Exception {
		ProcessTree psTree = ProcessTreeFactory.ReadProcessTreeFromFile( dataDirPath + "/pstree.ptml" );
		PrintStream printStream = new PrintStream( dataDirPath + "/eval_suite.txt" );

		EvalSuite test = new EvalSuite( psTree, totalDistinctTraces, samples );
		test.distinctCuts = distinctCuts;
		test.run( start, stop, step );

		printStream.println( test );
		test.toCSV( dataDirPath + "/eval_suite.csv" );
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

Process Tree: S1
Seq(S1, And(Seq(B1, B2, B3), Seq(A1, A2, A3)), S2, Xor(C, D), End)

Number of tests ran: 49
Test output format: [ Traces x Samples ( Random logs ) = Total traces ]

[ 2 x 1000 = 2000 ]
PB-Miner equal trees: 14 / 1000 = 1.4%	avg mining time: 8ms
IM Miner equal trees: 0 / 1000 = 0.0%	avg mining time: 6ms
Avg log completeness: 1.97 / 20 = 9.9%

IM\PB	T	F
TRUE	0	0
FALSE	14	986

[ 3 x 1000 = 3000 ]
PB-Miner equal trees: 96 / 1000 = 9.6%	avg mining time: 6ms
IM Miner equal trees: 15 / 1000 = 1.5%	avg mining time: 5ms
Avg log completeness: 2.88 / 20 = 14.4%

IM\PB	T	F
TRUE	2	13
FALSE	94	891	*/
}
