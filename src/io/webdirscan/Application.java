package io.webdirscan;

import static io.webdirscan.internal.Constants.LOGGER;

import java.io.FileOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import io.webdirscan.internal.Constants;
import io.webdirscan.internal.EventBus;
import io.webdirscan.internal.ParserUtils;
import io.webdirscan.internal.EventBus.Subscribe;

/**
    *     存在页面相互引用问题
 */
public class Application {
	private static ExecutorService servicePool = Executors.newCachedThreadPool();
	private static EventBus eventBus = EventBus.getInstance();
	private static CloseableHttpClient httpClient = HttpClientBuilder.create().build();

	public static void main(String[] args) throws Exception {
		try {
			if (args.length != 1) {
				System.out.println("usage: java -jar webdirScan.jar <url>");
				return;
			}
			eventBus.register(Constants.TAG_CONSOLE, ConsoleSubscriber.class);
			eventBus.register(Constants.TAG_FILESYSTEM, FileSystemSubscriber.class);
			servicePool.submit(new UrlFetcher(args[0]));
		} catch (Exception e) {
			LOGGER.warning(e.getMessage());
		}
	}

	private static class UrlFetcher implements Runnable {
		private String url;

		public UrlFetcher(String url) {
			this.url = url;
		}

		@Override
		public void run() {
			try {
				HttpGet get = new HttpGet(url);
				get.addHeader("Referer", url);
				get.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36 Edg/87.0.664.66");
				HttpResponse response = httpClient.execute(get);
				StringBuilder consoleMsg = new StringBuilder();
				consoleMsg.append(url);
				consoleMsg.append(" " + response.getStatusLine());
				consoleMsg.append(" " + response.getEntity().getContentType());
				consoleMsg.append(" " + response.getEntity().getContentLength());
				eventBus.notifyByTag(Constants.TAG_CONSOLE, consoleMsg.toString());
				byte[] content = IOUtils.toByteArray(response.getEntity().getContent());
				eventBus.notifyByTag(Constants.TAG_FILESYSTEM, new FileSystem(url, content));
				ParserUtils.fetchUrls(url,response,content).forEach((url)->{
					servicePool.submit(new UrlFetcher(url));
				});
			} catch (Exception e) {
				LOGGER.warning(url + " fetch error " + e.getMessage());
			}
		}
	}

	public static class ConsoleSubscriber {
		@Subscribe
		public void consoleLog(Object msg) {
			System.out.println(msg);
		}
	}

	public static class FileSystem {
		String path;
		byte[] data;

		public FileSystem() {

		}

		public FileSystem(String path, byte[] data) {
			super();
			this.path = path;
			this.data = data;
		}

		/**
		 * @return the path
		 */
		public String getPath() {
			return path;
		}

		/**
		 * @param path the path to set
		 */
		public void setPath(String path) {
			this.path = path;
		}

		/**
		 * @return the data
		 */
		public byte[] getData() {
			return data;
		}

		/**
		 * @param data the data to set
		 */
		public void setData(byte[] data) {
			this.data = data;
		}
        public void save() {
        	try {
        	String [] paths = path.split("/");
        	FileOutputStream file = new FileOutputStream(paths[paths.length-1]);
        	IOUtils.write(data, file);
        	IOUtils.closeQuietly(file);
        	}catch (Exception e) {
        	 LOGGER.warning(path + " saved fail " + e.getMessage());
			}
        }
	}

	public static class FileSystemSubscriber {
		@Subscribe
		public void fileSystemSave(Object msg) {
			FileSystem.class.cast(msg).save();
		}
	}
}
