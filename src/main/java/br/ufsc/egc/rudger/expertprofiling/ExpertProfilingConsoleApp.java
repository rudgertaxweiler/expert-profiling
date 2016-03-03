package br.ufsc.egc.rudger.expertprofiling;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.uima.UIMAException;

public class ExpertProfilingConsoleApp {
    
    public static void main(final String[] args) throws UIMAException, IOException, URISyntaxException {
        ExpertProfilingPipeline.Configuration config = ExpertProfilingUtil.createConfig();
        config.setUserCode("User demonstration");
        config.setUserName("User demonstration");
        config.setSourceLocation("content/docs-examples");
        config.setUseXmiDumper(true);
        config.setUseHeidelTime(true);
        
        ExpertProfilingPipeline epp = new ExpertProfilingPipeline();
        epp.run(config);
    }

}
