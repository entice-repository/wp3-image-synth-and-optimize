package hu.mta.sztaki.lpds.entice.virtualimagecomposer.rest;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.ClientResponse.Status;

import static hu.mta.sztaki.lpds.entice.virtualimagecomposer.rest.CustomHTTPHeaders.*;

@Path("/scripts") 
public class VirtualImageComposer {
	private static final Logger log = LoggerFactory.getLogger(VirtualImageComposer.class); 
	private void logRequest(final String method, final HttpHeaders httpHeaders, final HttpServletRequest request) {
		log.info("" + method);
	}

	private static final String PRE_ASSEMBLY_SCRIPT_FILE = ".delta-pre.sh";
	private static final String DELETIONS_SCRIPT_FILE = ".delta-delete.sh";
	private static final String INIT_SCRIPT_FILE = ".delta-init.sh";
	private static final String DELTA_PACKAGE_FILE = "delta-package.tar.gz";

	private static final String DELTA_LOG_FILE = "/var/log/image-assembly.log";

	@GET @Path("{id}") @Produces("application/x-shellscript")
	public Response getInstallScript(
			@Context HttpHeaders headers,
			@Context HttpServletRequest request,
			@HeaderParam(INIT_HEADER) String init,
			@PathParam("id") String id) {
		logRequest("GET", headers, request);		
		return getScript(id, null, !"false".equals(init));
	}	
	
	@GET @Path("{id}/{cloud}") @Produces("application/x-shellscript")
	public Response getInstallScriptCloud(
			@Context HttpHeaders headers,
			@Context HttpServletRequest request,
			@HeaderParam(INIT_HEADER) String init,
			@PathParam("id") String id,
			@PathParam("cloud") String cloud) {
		logRequest("GET CLOUD", headers, request);		
		return getScript(id, null, !"false".equals(init));
	} 	 

	private Response getScript(String id, String cloud, boolean init) {
		log.debug("Header " + INIT_HEADER + ": " + init);
		Client client = null;
		ClientResponse response;
		try {
			client = Client.create();
			String service = Configuration.virtualImageManagerRestUrl + "/virtualimages/" + id + "/fragments";
			log.debug("Calling service: " + service);
			WebResource webResource = client.resource(service);
			response = webResource.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
			if (response.getStatus() != 200) return Response.status(Status.BAD_REQUEST).entity("Cannot get fragments from Virtual Image Manager. HTTP error code: " + response.getStatus() + " " + response.getEntity(String.class)).build(); 
		} catch (Exception x) {
			return Response.status(Status.BAD_REQUEST).entity("Cannot get fragments from Virtual Image Manager. Exception: " + x.getMessage()).build(); 
		} finally {
			if (client != null) client.destroy();
		}
		// parse JSON array
		JSONArray fragmentUrlsJson;
		try { fragmentUrlsJson = new JSONArray(new JSONTokener(response.getEntity(String.class))); }
		catch (JSONException e) { 
			return Response.status(Status.BAD_REQUEST).entity("Invalid JSON from Virtual Image Manager").build(); 
		}
		log.debug("Got " + fragmentUrlsJson.length() + " fragments");
		
		// create response script
		StringBuilder sb = new StringBuilder();
		sb.append("#!/bin/sh\n");
		sb.append("START_TIME=`date +\"%s\"`\n");
		boolean withInit = !"no".equals(init);
		for (int i = 0; i < fragmentUrlsJson.length(); i++) {
			String fragmentUrls = fragmentUrlsJson.getString(i);
			if (!"".equals(fragmentUrls)) sb.append(getFragmentInstaller(fragmentUrls, cloud, withInit));
		}
		sb.append("echo Overall assembly time: $((`date +\"%s\"` - ${START_TIME}))s >> " + DELTA_LOG_FILE + "\n");
		return Response.status(Status.OK).entity(sb.toString())
//						.header("Content-Disposition", "attachment; filename=\"" + id + ".sh" + "\"" )
						.build();
	}

	private String getFragmentInstaller(String fragmentUrl, String cloud, boolean init) {
		StringBuilder sb = new StringBuilder();
		// note: fragment id is a URL
		String cloudPostfix = "";
		if (cloud != null) {
			cloudPostfix = cloud;
			if (fragmentUrl == null || !fragmentUrl.endsWith("/")) cloudPostfix = "/" + cloudPostfix;
		}
		sb.append("echo \"Merging fragment: " + fragmentUrl + cloudPostfix + "\" >> " + DELTA_LOG_FILE + "\n");
		sb.append("DOWNLOAD_TIME=`date +\"%s%3N\"`\n");
		sb.append("wget --tries=3 -q -O " + DELTA_PACKAGE_FILE + " " + fragmentUrl + cloudPostfix + " || echo 'Cannot download fragment' >> " + DELTA_LOG_FILE); sb.append("\n");
		sb.append("echo '  'Fragment download time: $((`date +\"%s%3N\"` - ${DOWNLOAD_TIME}))ms >> " + DELTA_LOG_FILE + "\n");
		sb.append("FRAGMENT_ASSEMBLY_TIME=`date +\"%s%3N\"`\n");
		sb.append("tar -xf " + DELTA_PACKAGE_FILE + " || echo 'Cannot unpack fragment' >> " + DELTA_LOG_FILE); sb.append("\n");
		sb.append("rm -f " + DELTA_PACKAGE_FILE); sb.append("\n");
		sb.append("[ -f "+ PRE_ASSEMBLY_SCRIPT_FILE + " ] && sh " + PRE_ASSEMBLY_SCRIPT_FILE); sb.append("\n");
		sb.append("[ -f "+ DELETIONS_SCRIPT_FILE + " ] && sh " + DELETIONS_SCRIPT_FILE); sb.append("\n");
		sb.append("rm -f " + DELETIONS_SCRIPT_FILE); sb.append("\n"); // optional
		sb.append("echo '  'Fragment assembly time: $((`date +\"%s%3N\"` - ${FRAGMENT_ASSEMBLY_TIME}))ms >> " + DELTA_LOG_FILE + "\n");
		sb.append("FRAGMENT_INIT_TIME=`date +\"%s%3N\"`\n");
		if (init) sb.append("[ -f "+ INIT_SCRIPT_FILE + " ] && sh " + INIT_SCRIPT_FILE); sb.append("\n");
		sb.append("rm -f " + INIT_SCRIPT_FILE); sb.append("\n"); // optional
		sb.append("echo '  'Fragment init time: $((`date +\"%s%3N\"` - ${FRAGMENT_INIT_TIME}))ms >> " + DELTA_LOG_FILE + "\n");
		return sb.toString();
	}
}