package org.processmining.plugins.HybridMiner.loglist;

import org.processmining.contexts.uitopia.annotations.UIExportPlugin;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.plugins.HybridMiner.XLogWriter;

import java.io.File;
import java.io.IOException;

@Plugin (name = "Export LogMap",
		parameterLabels = { "LogMap", "Directory" },
		returnLabels = {},
		returnTypes = {})
@UIExportPlugin (description = "Save list of logs to a dir",
		extension = "txt", pack = "test" )
public class ExportLogListPlugin {
	@UITopiaVariant (affiliation = "University of Tartu",
			author = "Bogdan S.",
			email = "bogdan89@ut.ee")
	@PluginVariant (requiredParameterLabels = { 0, 1 })
	public void export(PluginContext context,
					   LogMap logMap,
					   File file) throws IOException {
		String outputPath = file.getParent() + "/";
		logMap.forEach( ( name, log ) -> XLogWriter.saveXesGz( log, outputPath + name ) );
	}
}
