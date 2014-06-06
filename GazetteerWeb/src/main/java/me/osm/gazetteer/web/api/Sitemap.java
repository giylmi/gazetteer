package me.osm.gazetteer.web.api;

import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.osm.gazetteer.web.Configuration;
import me.osm.gazetteer.web.ESNodeHodel;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.restexpress.Request;
import org.restexpress.Response;

public class Sitemap {
	private static final int PAGE_SIZE = 45000;
	private String featureUrlTemplate;
	
	Pattern p = Pattern.compile(".*sitemap([0-9]+)\\.xml(\\.gz)?");
	
	public Sitemap(Configuration config) {
		this.featureUrlTemplate = config.getSiteXMLFeatureURL();
	}
	
	public void read(Request req, Response res)	{
		
		try	{
			
			String path = req.getPath();
			
			if(path.endsWith ("sitemap_index.xml")) {
				readIndex(req, res);
			}
			
			else {
				Matcher matcher = p.matcher(path);
				if(matcher.matches()) {
					readPage(Integer.valueOf(matcher.group(1)), req, res);
				}
			}
			
		}
		catch (Exception e) {
			res.setException(e);
		} 
	}

	private void readPage(int page, Request req, Response res) throws UnsupportedEncodingException {

		Client client = ESNodeHodel.getClient();
		
		SearchRequestBuilder searchQ = client.prepareSearch("gazetteer")
				.setSearchType(SearchType.QUERY_AND_FETCH)
				.setNoFields()
				.setQuery(QueryBuilders.matchAllQuery())
				.setExplain(false);
		
		searchQ.setSize(PAGE_SIZE);
		searchQ.setFrom(page * PAGE_SIZE);
		
		SearchResponse searchResponse = searchQ.get();
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		sb.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\" \n" + 
				  "        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n" + 
				  "        xsi:schemaLocation=\"http://www.sitemaps.org/schemas/sitemap/0.9 http://www.sitemaps.org/schemas/sitemap/0.9/sitemap.xsd\">\n");
		
		int i = 0;
		for(SearchHit hit : searchResponse.getHits().getHits()) {
			sb.append("    <url>\n");
			sb.append("        <loc>").append(
					StringUtils.replace(featureUrlTemplate, "{id}", hit.getId())).append("</loc>");
			sb.append("    </url>\n");
			i++;
		}
		System.out.print(i);
		
		sb.append("</urlset>");
		
		ChannelBuffer body = ChannelBuffers.wrappedBuffer(sb.toString().getBytes("UTF-8"));
		res.setBody(body);
		res.setContentType("text/xml");
	}

	private void readIndex(Request req, Response res) throws UnsupportedEncodingException {
		Client client = ESNodeHodel.getClient();
		
		CountResponse countResponse = client.prepareCount("gazetteer").setQuery(QueryBuilders.matchAllQuery()).get();
		
		long count = countResponse.getCount();
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		sb.append("<sitemapindex xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
		
		for(int i = 0; i <= count / PAGE_SIZE; i++) {
			sb.append("    <sitemap>\n");
			sb.append("        <loc>").append("/sitemap").append(i).append(".xml").append("</loc>");
			sb.append("    </sitemap>\n");
		}
		
		sb.append("</sitemapindex>");
		
		res.setBody(ChannelBuffers.wrappedBuffer(sb.toString().getBytes("UTF-8")));
		res.setContentType("text/xml");
	}
	
}