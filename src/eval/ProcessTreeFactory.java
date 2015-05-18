package eval;

import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.impl.ProcessTreeImpl;
import org.processmining.processtree.ptml.Ptml;
import org.processmining.processtree.ptml.exporting.PtmlExportTree;
import org.processmining.processtree.ptml.importing.PtmlImportTree;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class ProcessTreeFactory {
	public static ProcessTree ReadProcessTreeFromFile( String filePath ) throws Exception {
		Ptml ptml  = new PtmlImportTree( )
				.importPtmlFromStream(
						org.processmining.plugins.PBMiner.Main.pluginContext
						, new FileInputStream( filePath )
						, ""
						, 0
				);

		ProcessTree psTree = new ProcessTreeImpl( ptml.getId(), ptml.getName() );
		ptml.unmarshall( psTree );

		return psTree;
	}

	public static void SaveProcessTreeToFile( String filepath, ProcessTree psTree ) throws IOException {
		new PtmlExportTree().exportDefault(
				org.processmining.plugins.PBMiner.Main.pluginContext
				, psTree, new File( filepath )
		);
	}
}
