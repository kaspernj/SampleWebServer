import static org.junit.Assert.*;

import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;


public class TestStartSample {

	@Test
	public void test() {
		HashMap<String, String> opts = new HashMap<String, String>();
		opts.put("DocumentRoot", "/home/kaspernj/Dev/Java/Eclipse/SampleWebServer/html");
		
		try {
			SampleWebServer smws = new SampleWebServer(opts);
			smws.start();
			smws.join();
		} catch (Exception e) {
			fail(e.getMessage());
		}
		
		
	}

}
