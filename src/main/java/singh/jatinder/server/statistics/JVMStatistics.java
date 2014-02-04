/**
 * 
 */
package singh.jatinder.server.statistics;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Jatinder
 *
 */
public class JVMStatistics implements ICollector {

	public Map<String, Number> getStatistics() {
		Map<String, Number> stats = new LinkedHashMap<String, Number>();
		Runtime rt = Runtime.getRuntime();
		stats.put("Num Processors", rt.availableProcessors());
		stats.put("Free Memory", rt.freeMemory());
		stats.put("Max Memory", rt.maxMemory());
		stats.put("Total Memory", rt.totalMemory());
		stats.put("Current Time in millis", System.currentTimeMillis());
		return stats;
	}

}
