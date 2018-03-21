package com.ip2location;

import static com.ip2location.HttpHeader.CF_CONNECTING_IP;
import static com.ip2location.HttpHeader.HTTP_CF_CONNECTING_IP;
import static com.ip2location.HttpHeader.HTTP_X_AB_FORWARD;
import static com.ip2location.HttpHeader.HTTP_X_CF_CONNECTING_IP;
import static com.ip2location.HttpHeader.HTTP_X_CLUSTER_CLIENT_IP;
import static com.ip2location.HttpHeader.HTTP_X_FORWARDED_FOR;
import static com.ip2location.HttpHeader.INF_SIMULATED_IP;
import static com.ip2location.HttpHeader.X_CF_CONNECTING_IP;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.ip2proxy.IP2Proxy;
import com.ip2proxy.ProxyResult;
import com.zeus.ZXTMServlet.ZXTMHttpServletRequest;

public class IP2LocationServlet extends HttpServlet {

	private static String OfficeIp = "192.208.232.130";
	private static final String UNKNOWN = "unknown";
	private static IP2Location Location;
	static {
		try {
			Location = new IP2Location();
			Location.IPDatabasePath = "/usr/share/ip2/IPV6-COUNTRY-REGION-CITY-LATITUDE-LONGITUDE-ISP-DOMAIN-MOBILE-USAGETYPE.SAMPLE.BIN";
			Location.UseMemoryMappedFile = true;
			// Location.IPLicensePath = "%%__PATH_TO_LICENSE_FILE__%%";
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static IP2Proxy Proxy;
	static {
		try {
			Proxy = new IP2Proxy();
			Proxy.Open("/usr/share/ip2/IP2PROXY-LITE-PX2.BIN", IP2Proxy.IOModes.IP2PROXY_MEMORY_MAPPED);
		} catch (IOException e) {
			Proxy.Close();
			e.printStackTrace();
		}
	}

	private static String IPInfo(String IP) throws IOException {
		Response response = new Response();
		IPResult rec = Location.IPQuery(IP);
		if ("OK".equals(rec.getStatus())) {

			response.AreaCode = rec.getAreaCode();
			response.City = rec.getCity();
			response.Country = rec.getCountryLong();
			response.CountryCode = rec.getCountryShort();
			response.Domain = rec.getDomain();
			response.Elevation = rec.getElevation();
			response.IDD = rec.getIDDCode();
			response.ISP = rec.getISP();
			response.Latitude = rec.getLatitude();
			response.Longitude = rec.getLongitude();
			response.MCC = rec.getMCC();
			response.MNC = rec.getMNC();
			response.MobileBrand = rec.getMobileBrand();
			response.NetSpeed = rec.getNetSpeed();
			response.Region = rec.getRegion();
			response.TimeZone = rec.getTimeZone();
			response.UsageType = rec.getUsageType();
			response.ZipCode = rec.getZipCode();
		}

		ProxyResult All = Proxy.GetAll(IP);
		response.IsProxy = All.Is_Proxy;
		response.ProxyType = All.Proxy_Type;

		Gson gson = new Gson();
		return gson.toJson(response);
	}

	private static boolean isIpFound(String ip) {
		return ip != null && ip.length() > 0 && !UNKNOWN.equalsIgnoreCase(ip);
	}

	private static String getClientIp(HttpServletRequest request) {
		String ip = null;
		int tryCount = 1;

		while (!isIpFound(ip) && tryCount <= 9) {
			switch (tryCount) {
			case 1:
				ip = request.getHeader(INF_SIMULATED_IP.key());
				break;
			case 2:
				ip = request.getHeader(HTTP_X_AB_FORWARD.key());
				break;
			case 3:
				ip = request.getHeader(HTTP_CF_CONNECTING_IP.key());
				break;
			case 4:
				ip = request.getHeader(CF_CONNECTING_IP.key());
				break;
			case 5:
				ip = request.getHeader(X_CF_CONNECTING_IP.key());
				break;
			case 6:
				ip = request.getHeader(HTTP_X_CF_CONNECTING_IP.key());
				break;
			case 7:
				ip = request.getHeader(HTTP_X_FORWARDED_FOR.key());
				break;
			case 8:
				ip = request.getHeader(HTTP_X_CLUSTER_CLIENT_IP.key());
				break;
			default:
				ip = request.getRemoteAddr();
			}

			tryCount++;
		}

		if (ip != null) {
			ip = ip.replace(" ", "").split(",")[0];
		}

		if (ip == "::1" || ip == "127.0.0.1") {
			ip = OfficeIp;
		}

		return ip;
	}

	public static void main(String[] args) throws IOException {
		try {
			String result = IPInfo(args[0]);
			System.out.println(result);
		} catch (Exception e) {
			Proxy.Close();

			// Save stack trace as a string and print to the log
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			System.out.println(sw.toString());
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			String IP = getClientIp(req);
			String result = new String();

			if (isIpFound(IP)) {
				result = IPInfo(IP);
			}

			((ZXTMHttpServletRequest) req).setConnectionData("ip2location", result);
		} catch (Exception e) {
			Proxy.Close();

			// Save stack trace as a string and print to the log
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			log(sw.toString());
		}
	}

	private static class Response {
		public String AreaCode;
		public String City;
		public String Country;
		public String CountryCode;
		public String Domain;
		public float Elevation;
		public String IDD;
		public String ISP;
		public float Latitude;
		public float Longitude;
		public String MCC;
		public String MNC;
		public String MobileBrand;
		public String NetSpeed;
		public String Region;
		public String TimeZone;
		public String UsageType;
		public String ZipCode;
		public int IsProxy;
		public String ProxyType;
	}
}
