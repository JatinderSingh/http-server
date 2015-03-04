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
    private static final long startTime = System.currentTimeMillis();

	public Map<String, Number> getStatistics() {
		Map<String, Number> stats = new LinkedHashMap<String, Number>();
		Runtime rt = Runtime.getRuntime();
		stats.put("NumProcessors", rt.availableProcessors());
		stats.put("FreeMemory", rt.freeMemory());
		stats.put("MaxMemory", rt.maxMemory());
		stats.put("TotalMemory", rt.totalMemory());
		stats.put("CurrentTimeinmillis", System.currentTimeMillis());
		stats.put("StartTimeinmillis", startTime);
		stats.put("UpTimeinmillis", System.currentTimeMillis()-startTime);
		return stats;
	}

    public boolean isDisplayed() {
        return true;
    }

}
