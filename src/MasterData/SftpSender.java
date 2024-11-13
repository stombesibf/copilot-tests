// camel-k: language=java
// camel-k: dependency=camel-file 
// camel-k: dependency=camel:csv
// camel-k: dependency=camel-jackson

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.Exchange;
import org.apache.camel.AggregationStrategy;

import org.apache.camel.model.dataformat.CsvDataFormat;
import org.apache.camel.model.dataformat.JsonLibrary;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.Date;
import java.text.SimpleDateFormat;

import com.danone.utils.*;

public class SftpSender extends RouteBuilder {
	@Override
	public void configure() throws Exception {

		from("kafka:{{danone.kafka.topic.bs}}?brokers={{danone.kafka.broker}}&maxPollIntervalMs=30000000")
			// identifico la ruta //
			.routeId("sftpSender")

			// defino el FunctionalEntity a partir de los parametros //
			// .setHeader("FunctionalEntity").simple("${headers.X-MessageType}")

			// cargo el MDC //
			.process(new HeaderToMDC())

			// empiezo a loggear //
			.log(LoggingLevel.INFO, "*** S T A R T  S E R V I C E ***")

			.log("START ${body}")
			.process(exchange -> {
        		ObjectMapper mapper = new ObjectMapper();
        		String body = exchange.getMessage().getBody(String.class);
            	JsonNode node = mapper.readTree(body);
            	exchange.getMessage().setHeader("X-MessageType", node.get("messageType").asText());
            	exchange.getMessage().setHeader("X-FilePath", node.get("path").asText());
            	exchange.getMessage().setHeader("X-FileName", node.get("filename").asText());

            	JsonNode fulltree = mapper.readTree(new File("/sdata/resources/sftpinfo.json"));
            	JsonNode sftpinfo = fulltree;
	            for(JsonNode element : fulltree.get(node.get("messageType").asText())) {
	            	if (element.get("destination").asText().equals(node.get("destination").asText()))
			            sftpinfo = element.get("serverinfo");
	            }            	
            	exchange.getMessage().setHeader("X-SFTP-Domain", sftpinfo.get("domain").asText());
            	exchange.getMessage().setHeader("X-SFTP-Port", sftpinfo.get("port").asText());
            	exchange.getMessage().setHeader("X-SFTP-Path", sftpinfo.get("path").asText());
            	exchange.getMessage().setHeader("X-SFTP-Username", sftpinfo.get("username").asText());
            	exchange.getMessage().setHeader("X-SFTP-Password", sftpinfo.get("password").asText());

            	if (sftpinfo.get("filename") != null)
            		exchange.getMessage().setHeader("X-OutputFileName", sftpinfo.get("filename").asText());
            	else
            		exchange.getMessage().setHeader("X-OutputFileName", "'" + exchange.getMessage().getHeader("X-FileName").toString() + "'");

			})
			// .log("FIRST ${headers}")
		    .pollEnrich().simple("file:/sdata/Outgoing/?fileName=${headers.X-FileName}&noop=true")
				.aggregationStrategy( new AggregationStrategy() { 
				    public Exchange aggregate( Exchange oldExchange, Exchange newExchange ) 
				    { 
				        newExchange.getIn().setHeader( "X-SFTP-Domain", oldExchange.getIn().getHeader( "X-SFTP-Domain" ) );
				        newExchange.getIn().setHeader( "X-SFTP-Port", oldExchange.getIn().getHeader( "X-SFTP-Port" ) );
				        newExchange.getIn().setHeader( "X-SFTP-Path", oldExchange.getIn().getHeader( "X-SFTP-Path" ) );
				        newExchange.getIn().setHeader( "X-SFTP-Username", oldExchange.getIn().getHeader( "X-SFTP-Username" ) );
				        newExchange.getIn().setHeader( "X-SFTP-Password", oldExchange.getIn().getHeader( "X-SFTP-Password" ) );
				        newExchange.getIn().setHeader( "X-OutputFileName", oldExchange.getIn().getHeader( "X-OutputFileName" ) );
				        newExchange.setProperty( "MyProperty", oldExchange.getProperty( "MyProperty" ) );
				        return newExchange; 
				    } } )		    		    
				// .toD("sftp:${headers.X-SFTP-Domain}:${headers.X-SFTP-Port}${headers.X-SFTP-Path}?username=${headers.X-SFTP-Username}&password=${headers.X-SFTP-Password}&useUserKnownHostsFile=false&serverHostKeys=ssh-dss")
				.process(exchange -> {
					String filenameExpr = exchange.getIn().getHeader( "X-OutputFileName" ).toString();
					String filename = new SimpleDateFormat(filenameExpr).format(new Date());
	            	exchange.getMessage().setHeader("CamelFileName", filename);           
				})
				// ORIGINAL // .toD("sftp:${headers.X-SFTP-Domain}:${headers.X-SFTP-Port}${headers.X-SFTP-Path}?username=${headers.X-SFTP-Username}&password=${headers.X-SFTP-Password}&useUserKnownHostsFile=false&serverHostKeys=ssh-dss")
				// CON EXTRA // .toD("sftp:${headers.X-SFTP-Domain}:${headers.X-SFTP-Port}${headers.X-SFTP-Path}?username=${headers.X-SFTP-Username}&password=${headers.X-SFTP-Password}&useUserKnownHostsFile=false")
				.toD("sftp:${headers.X-SFTP-Domain}:${headers.X-SFTP-Port}${headers.X-SFTP-Path}?username=${headers.X-SFTP-Username}&password=${headers.X-SFTP-Password}&useUserKnownHostsFile=false&serverHostKeys={{danone.sftp.serverHostKeys}}")
		    .end()
		    .log("END ${headers}")

			// elimino los headers al terminar //
			.process(new HeaderRemove())

			// termino de loggear //
			.log(LoggingLevel.INFO, "*** E N D  S E R V I C E ***")

		    ;

		from("file:/etc/camel/resources?noop=true")
			.to("file:/sdata/resources");

	    from("platform-http:/healthcheck")
	    	/*
	        .log("---IN ${headers}")
	        .to("log:info")
	        */
	        .setBody().constant("OK");

	}

}
