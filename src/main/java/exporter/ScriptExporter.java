package exporter;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class ScriptExporter {
	private HttpServer httpServer;
	private static final String context = "/metrics";
	private static int scriptExporterUp = 1;
	public static final Logger logger = LogManager.getLogger();
	private static ArrayList<HealthCheck> allChecks;

	@SuppressWarnings("restriction")
	public ScriptExporter(String jsonFile, int port) throws IOException {
		httpServer = HttpServer.create(new InetSocketAddress(port), 0);
		parseJson(jsonFile);
		httpServer.createContext("/", new PageHandler());
		httpServer.createContext(context, new ScriptHandler());
		httpServer.start();
		logger.info("Service running at " + httpServer.getAddress());
		logger.info("Type [CTRL]+[C] to quit!");
	}

	private void parseJson(String jsonFile) {
		JsonReader jsonReader = null;
		try {
			jsonReader = new JsonReader(new FileReader(jsonFile));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.fatal("File not found, please make sure " + jsonFile + " exist.");
		}
		java.lang.reflect.Type listType = new TypeToken<ArrayList<HealthCheck>>() {
		}.getType();
		allChecks = new Gson().fromJson(jsonReader, listType);
	}

	private static class PageHandler implements HttpHandler {
		@SuppressWarnings("restriction")
		public void handle(HttpExchange t) throws IOException {
			StringBuilder response = new StringBuilder();
			t.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
			response.append(
					"<html>\n<head><title>Scritp Exporter</title></head>\n<body><h1>Scritp Exporter</h1>\n<p><a href=\"/metrics\">Metrics</a></p>\n</body>\n</html>\n");
			t.getResponseHeaders().add("Content-Length", String.valueOf(response.length()));
			t.sendResponseHeaders(200, response.length());

			// clean up
			OutputStream os = t.getResponseBody();
			os.write(response.toString().getBytes());
			os.close();
		}
	}

	private static class ScriptHandler implements HttpHandler {
		@SuppressWarnings("restriction")
		public void handle(HttpExchange t) throws IOException {
			StringBuilder response = new StringBuilder();
			response.append("# script's exitStatus, 0 means exit without error\n");
			scriptExporterUp = 1;
//			response.append("up " + scriptExporterUp + "\n");
			response.append("script_exporter_up " + scriptExporterUp + "\n");
			for (HealthCheck healthCheck : allChecks) {
				response.append("script_exitStatus{script_name=\"" + healthCheck.getName() + "\"} "
						+ healthCheck.getResult() + "\n");
			}

			t.getResponseHeaders().add("Content-Type", "text/plain; version=0.0.4");
			t.getResponseHeaders().add("Content-Length", String.valueOf(response.length()));
			t.sendResponseHeaders(200, response.length());
			// clean up
			OutputStream os = t.getResponseBody();
			os.write(response.toString().getBytes());
			os.close();
		}
	}

	public static void main(String[] args) {
		if (args.length < 1) {
			logger.error("Pleas follow syntax: java -ScriptExporter <configFile> <port>");
			return;
		}
		ScriptExporter scriptExporter;
		try {
			scriptExporter = new ScriptExporter(args[0], Integer.parseInt(args[1]));
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			logger.error("Faile to crate server on " + Integer.parseInt(args[1]));
			e.printStackTrace();
		}
		return;

	}
}
