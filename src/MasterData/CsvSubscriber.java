// camel-k: language=java
// camel-k: dependency=camel-file 
// camel-k: dependency=camel:csv
// camel-k: dependency=camel-jackson
// camel-k: dependency=camel-dataformat

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

import com.danone.mapping.JsonMapping;
import com.danone.mapping.JsonTransform;

import java.lang.Number;

import java.io.File;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.net.URLDecoder;

import java.lang.reflect.Method;

import com.danone.utils.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CsvSubscriber extends RouteBuilder {

	private String getDelimiter(String definition) {
		if (definition.startsWith("%"))
			return URLDecoder.decode(definition);
		return definition;
	}

	@Override
	public void configure() throws Exception {

		/*
		CsvDataFormat csvInput = new CsvDataFormat();
		csvInput.setLazyLoad("true");
		csvInput.setUseOrderedMaps("true");
		csvInput.setQuoteMode("NON_NUMERIC");
		*/

		from("kafka:{{danone.kafka.topic.lb}}?brokers={{danone.kafka.broker}}&maxPollIntervalMs=30000000")
			// identifico la ruta //
			.routeId("csvSubscriber")

			// defino el FunctionalEntity a partir de los parametros //
			// .setHeader("FunctionalEntity").simple("${headers.X-MessageType}")

			// cargo el MDC //
			.process(new HeaderToMDC())

			// empiezo a loggear //
			.log(LoggingLevel.INFO, "*** S T A R T  S E R V I C E ***")

			/* .threads(5) */
			.to("log:START_SERVICE?multiline=true")
			.process(exchange -> {
        		ObjectMapper mapper = new ObjectMapper();
        		String body = exchange.getMessage().getBody(String.class);
            	JsonNode node = mapper.readTree(body);

            	String[] aFileName = node.get("filename").asText().split("\\.");
            	String outputFileName = aFileName[0] + "." + node.get("destination").asText() + "." + aFileName[1] + "." + aFileName[2];

            	exchange.getMessage().setHeader("X-MessageType", node.get("messageType").asText());
            	exchange.getMessage().setHeader("X-FilePath", node.get("path").asText());
            	exchange.getMessage().setHeader("X-FileName", node.get("filename").asText());
            	exchange.getMessage().setHeader("CamelFileName", node.get("filename").asText());
            	exchange.getMessage().setHeader("X-Destination", node.get("destination").asText());
            	exchange.getMessage().setHeader("X-OutputFileName", outputFileName);

				String messageType = exchange.getMessage().getHeader("X-MessageType").toString();
				String destination = exchange.getMessage().getHeader("X-Destination").toString();

				JsonMapping jsonMapping = new JsonMapping("/sdata/resources/mapinfo.json", messageType, destination);
				Map<String, Object> reply = jsonMapping.getHeaderMap();

	            exchange.getMessage().setBody(reply);

				JsonNode input = jsonMapping.getInput();
	            exchange.getMessage().setHeader(
	            	"X-InputDelimiter", 
					(input != null && input.get("delimiter") != null) ? input.get("delimiter").asText() : ","
	        	);
	            exchange.getMessage().setHeader(
	            	"X-InputQuotes", 
					(input != null && input.get("quotes") != null) ? input.get("quotes").asText() : "NON_NUMERIC"
	        	);

				JsonNode output = jsonMapping.getOutput();
	            exchange.getMessage().setHeader(
	            	"X-OutputDelimiter", 
					(output != null && output.get("delimiter") != null) ? output.get("delimiter").asText() : ","
	        	);
	            exchange.getMessage().setHeader(
	            	"X-OutputQuotes", 
					(output != null && output.get("quotes") != null) ? output.get("quotes").asText() : "NON_NUMERIC"
	        	);
	            exchange.getMessage().setHeader(
	            	"X-OutputHeaderQuotes", 
					(output != null && output.get("headerquotes") != null) ? output.get("headerquotes").asText() : "NON_NUMERIC"
	        	);
			})
			.process(new HeaderProcessor())
			.log("${headers}")
			.toD("dataformat:csv:marshal?escape=%5C&quotemode=${headers.X-OutputHeaderQuotes}&delimiter=${headers.X-OutputDelimiter}")
			// .marshal(csvOutputHeader)
		    .to("file:/sdata/Outgoing/?fileName=${headers.X-OutputFileName}&fileExist=Override")	
		    .pollEnrich().simple("file:/sdata/Incoming/?fileName=${headers.X-FileName}&noop=true&idempotent=false")
				.aggregationStrategy( new AggregationStrategy() { 
				    public Exchange aggregate( Exchange oldExchange, Exchange newExchange ) 
				    { 
				        newExchange.getIn().setHeader( "X-MessageType", oldExchange.getIn().getHeader( "X-MessageType" ) );
				        newExchange.getIn().setHeader( "X-Destination", oldExchange.getIn().getHeader( "X-Destination" ) );
				        newExchange.getIn().setHeader( "X-OutputFileName", oldExchange.getIn().getHeader( "X-OutputFileName" ) );

				        newExchange.getIn().setHeader( "X-InputDelimiter", oldExchange.getIn().getHeader( "X-InputDelimiter" ) );
				        newExchange.getIn().setHeader( "X-InputQuotes", oldExchange.getIn().getHeader( "X-InputQuotes" ) );
				        newExchange.getIn().setHeader( "X-OutputDelimiter", oldExchange.getIn().getHeader( "X-OutputDelimiter" ) );
				        newExchange.getIn().setHeader( "X-OutputQuotes", oldExchange.getIn().getHeader( "X-OutputQuotes" ) );

				        newExchange.setProperty( "mapping", oldExchange.getProperty( "mapping" ) );
				        return newExchange; 
				    } } )		    
				.toD("dataformat:csv:unmarshal?lazyload=true&useorderedmaps=true&escape=%5C&quotemode=${headers.X-InputQuotes}&delimiter=${headers.X-InputDelimiter}")
			    // .unmarshal(csvInput)
			    .split(body()).streaming()
			    	.marshal().json(JsonLibrary.Jackson)
					.process(new LineProcessor())
					.toD("dataformat:csv:marshal?escape=%5C&quotemode=${headers.X-OutputQuotes}&delimiter=${headers.X-OutputDelimiter}")
					// .marshal(csvOutput)
				    .to("file:/sdata/Outgoing/?fileName=${headers.X-OutputFileName}&fileExist=Append")	
			    .end()
		    .end()
		    .to("direct:sendtopic")

			// elimino los headers al terminar //
			.process(new HeaderRemove())

			// termino de loggear //
			.log(LoggingLevel.INFO, "*** E N D  S E R V I C E ***")

			;		    

		from("direct:sendtopic")
			.setBody().simple("{ \"messageType\": \"${headers.X-MessageType}\", \"path\": \"/sdata/Outgoing\", \"filename\": \"${headers.X-OutputFileName}\", \"destination\": \"${headers.X-Destination}\" }")
	    	.to("log:END_SERVICE?multiline=true")			
			.to("kafka:{{danone.kafka.topic.bs}}?brokers={{danone.kafka.broker}}");

		from("file:/etc/camel/resources?noop=true")
			.to("file:/sdata/resources");

	    from("platform-http:/healthcheck")
	    	/*
	        .log("---IN ${headers}")
	        .to("log:info")
	        */
	        .setBody().constant("OK");

	}

	public static class HeaderProcessor implements Processor {

		@Override
		public void process(Exchange exchange) throws Exception {
			String messageType = exchange.getMessage().getHeader("X-MessageType").toString();
			String destination = exchange.getMessage().getHeader("X-Destination").toString();

			JsonMapping jsonMapping = new JsonMapping("/sdata/resources/mapinfo.json", messageType, destination);
			Map<String, Object> reply = jsonMapping.getHeaderMap();

            exchange.getMessage().setBody(reply);
            exchange.setProperty("mapping", jsonMapping);
            
            // exchange.getMessage().setHeader("X-Delimiter", jsonMapping.getDelimiter());
		}

	}

	public static class LineProcessor implements Processor {


		private static Logger logger = LoggerFactory.getLogger(LineProcessor.class);

		@Override
		public void process(Exchange exchange) throws Exception {
			String messageType = exchange.getMessage().getHeader("X-MessageType").toString();
			String destination = exchange.getMessage().getHeader("X-Destination").toString();

			JsonMapping jsonMapping = (JsonMapping)exchange.getProperty("mapping");
			if (jsonMapping == null) {
				jsonMapping = new JsonMapping("/sdata/resources/mapinfo.json", messageType, destination);
			}

            String body = exchange.getMessage().getBody(String.class);
			
			JsonNode newnode = jsonMapping.json2json(body);

            Map<String, Object> reply = jsonMapping.json2map(newnode);

            exchange.getMessage().setBody(reply);
            exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
		}

	}

	/*************************************************************************/
	/****************************** JsonMapping ******************************/
	/*************************************************************************/

/***

	public static class JsonMapping {

		private String filename;
		private JsonNode mapinfo; 
        private ObjectMapper mapper;
        private String delimiter;

		public JsonMapping(String filename, String messageType, String destination) throws Exception {
        	this.mapper = new ObjectMapper();

			this.filename = filename;
            JsonNode fulltree = mapper.readTree(new File(filename));
            for(JsonNode element : fulltree.get(messageType)) {
            	if (element.get("destination").asText().equals(destination))
		            this.mapinfo = element.get("mapping");
		            this.delimiter = element.get("delimiter").asText();
            }
		} 

		public void setFilename(String filename) throws Exception {
			this.filename = filename;
            this.mapinfo = mapper.readTree(new File(filename));
		}

		public JsonNode json2json(JsonNode node) throws Exception {
            JsonNode newnode = mapper.createObjectNode();
            JsonTransform transform = new JsonTransform();

            for(JsonNode mapping : this.mapinfo) {            	

            	String methodName = (mapping.get("transform") != null ? mapping.get("transform").asText() : "defaultcopy");
				Method method = transform.getClass().getDeclaredMethod(methodName, JsonNode.class, JsonNode.class);
            	Object value = method.invoke(transform, node, mapping);
				JsonNode valueNode = mapper.valueToTree(value);
	            ((ObjectNode) newnode).set(mapping.get("to").asText(), valueNode);
            }

            return newnode;
		}

		public JsonNode json2json(String body) throws Exception {
            JsonNode node = mapper.readTree(body);
            return json2json(node);
        }

        public Map<String, Object> json2map(JsonNode newnode) {
            Map<String, Object> reply = new LinkedHashMap<String, Object>(); 

            for(JsonNode mapping : this.mapinfo) {       
            	String name = mapping.get("to").asText();
            	JsonNode elem = newnode.get(name);
				Object value = mapper.convertValue(elem, Object.class);        		
        		reply.put(name, value);
        	}

        	return reply;
        }

        public Map<String, Object> getHeaderMap() {
            Map<String, Object> reply = new LinkedHashMap<String, Object>(); 

            for(JsonNode mapping : this.mapinfo) {       
            	String name = mapping.get("to").asText();
    			reply.put(name, name);
        	}

        	return reply;
        }

        public String getDelimiter() {
        	return this.delimiter;
        }

	}

***/

	/*************************************************************************/
	/***************************** JsonTransform *****************************/
	/*************************************************************************/

/*** 

	public static class JsonTransform {

        public Object defaultcopy(JsonNode node, JsonNode mapping) {
            JsonNode elem = node.get(mapping.get("from").asText());
    		ObjectMapper mapper = new ObjectMapper();
			Object value = mapper.convertValue(elem, Object.class);        		
        	return value;
        }

        public Object string2long(JsonNode node, JsonNode mapping) {
            Object value = new Long(node.get(mapping.get("from").asText()).asLong());
        	return value;
        }

        public Object fixedvalue(JsonNode node, JsonNode mapping) {
            Object value = mapping.get("value");
        	return value;
        }
	}
	
***/

}
