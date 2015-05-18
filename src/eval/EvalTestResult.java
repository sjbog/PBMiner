package eval;

public class EvalTestResult {
	public int distinctTraces;
	public boolean PBMinerResult;
	public boolean IMMinerResult;
	public long PBMiningTimeNs, IMMiningTimeNs;

	public EvalTestResult( ) {

	}
	public EvalTestResult( int distinctTraces, boolean PBMinerResult, boolean IMMinerResult, long PBMiningTimeNs, long IMMiningTimeNs ) {
		this.distinctTraces = distinctTraces;
		this.PBMinerResult = PBMinerResult;
		this.IMMinerResult = IMMinerResult;
		this.PBMiningTimeNs = PBMiningTimeNs;
		this.IMMiningTimeNs = IMMiningTimeNs;
	}

	public static int getDistinctTraces( EvalTestResult evalTestResult ) {
		return evalTestResult.distinctTraces;
	}
	public static int PBMinerResult( EvalTestResult evalTestResult ) {
		return evalTestResult.PBMinerResult ? 1 : 0;
	}
	public static int IMMinerResult( EvalTestResult evalTestResult ) {
		return evalTestResult.IMMinerResult ? 1 : 0;
	}
}
