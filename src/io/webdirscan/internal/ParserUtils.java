package io.webdirscan.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.http.HttpResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class ParserUtils {
	private static Map<String, String> urlTarget;
	private static Pattern pattern;
	static {
		urlTarget = new HashMap<String, String>();
        
		urlTarget.put("link", "href");
		urlTarget.put("a", "href");
		
		urlTarget.put("img", "src");
		urlTarget.put("javascript", "src");
		urlTarget.put("video", "src");
		urlTarget.put("audio", "src");
		urlTarget.put("iframe", "src");
		urlTarget.put("source", "src");
		urlTarget.put("track", "src");
		
		urlTarget.put("object", "data");
		urlTarget.put("command", "icon");
		urlTarget.put("form", "action");
		
		pattern = Pattern.compile(Constants.URL_PATTERN);
	}

	public static List<String> fetchUrls(String context,HttpResponse response,byte[] content) {
		List<String> urls = new ArrayList<>();
		
	    if(response.getEntity().getContentType().getValue().contains(Constants.CONTENT_TYPE_HTML)) {
	    	Document document = Jsoup.parse(new String(content));
	    	urlTarget.forEach((k,v)->{
	    		 document.getElementsByTag(k).forEach((element)->{
	    			 urls.add(fixUrl(context,element.attr(v)));
	    		 });
	    	});
	    }
	    return urls.stream().filter((url)->!"".equals(url.trim())).distinct().collect(Collectors.toList());
	}
	/**
	 * @param url
	 * url格式：http://.../#/#...
	 * @return
	 */
	private static final String fixUrl(String context,String url) {
		if(pattern.matcher(url).matches()||"".equals(url.trim()))return url.trim();
		if(url.trim().startsWith("#"))return "";
		if(url.trim().contains("javascript:"))return "";
		if(url.trim().startsWith("//"))return "http:"+url.trim();
		if(url.trim().startsWith("/"))return context+url.trim();
		return context+"/"+url.trim();
	}
}
