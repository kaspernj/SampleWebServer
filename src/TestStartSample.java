import static org.junit.Assert.*;

import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;


public class TestStartSample {

	@Test
	public void test() {
		HashMap<String, String> opts = new HashMap<String, String>();
		opts.put("DocumentRoot", "/home/kaspernj/Dev/Java/Eclipse/SampleWebServer/");
		
		try {
			SampleWebServer smws = new SampleWebServer();
		} catch (IOException e) {
			fail(e.getMessage());
		}
	}

}
