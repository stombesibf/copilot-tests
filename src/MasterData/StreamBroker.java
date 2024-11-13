// camel-k: language=java
// camel-k: dependency=camel-netty-http
// camel-k: dependency=camel-jackson

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.Exchange;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import com.danone.utils.*;

public class StreamBroker extends RouteBuilder {
	@Override
	public void configure() throws Exception {

		// from("netty-http:http://0.0.0.0:8080/masterdata?chunkedMaxContentLength=1000000000&disableStreamCache=true")
		from("netty-http:http://0.0.0.0:8080/masterdata?chunkedMaxContentLength=1000000000&maxChunkSize=65536&disableStreamCache=true")
			// identifico la ruta //
			.routeId("streamBroker")

			// defino el FunctionalEntity a partir de los parametros //
			.setHeader("FunctionalEntity").simple("${headers.X-MessageType}")

			// cargo el MDC //
			.process(new HeaderToMDC())

			// empiezo a loggear //
			.log(LoggingLevel.INFO, "*** S T A R T  S E R V I C E ***")

			.log("START ${headers.X-MessageType}")
			.log("${headers}")
			.doTry()
				.to("direct:validate")
				.setHeader("CamelFileName", simple("${headers.X-MessageType}.${date-with-timezone:now:America/Argentina/Buenos_Aires:yyyyMMddHHmmss}.csv"))
				.to("direct:processFile")
				.to("direct:sendtopic")
				.to("direct:response")
			.doCatch(Exception.class)
				/* .setHeader("X-ErrorMessage").simple("${exception.message}") */
	            .process(new Processor() {
	                @Override
	                public void process(Exchange exchange) throws Exception {	                	
	                    final Exception ex = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
	                    if (ex instanceof ExtendedException) {
		                    exchange.getIn().setHeader("X-ErrorMessage", ex.getMessage());
		                    exchange.getIn().setHeader("X-ErrorStatus", ((ExtendedException)ex).getStatus());
		                    exchange.getIn().setHeader("X-ErrorCode", ((ExtendedException)ex).getCode());	                    	
	                    } else {
		                    exchange.getIn().setHeader("X-ErrorMessage", "Se produjo un error interno");
		                    exchange.getIn().setHeader("X-ErrorStatus", 500);
		                    exchange.getIn().setHeader("X-ErrorCode", "500x000");	                    		                    	
	                    }
	                }
	            })				
				.to("direct:responseerror")
			.endDoTry()

			// elimino los headers al terminar //
			.process(new HeaderRemove())

			// termino de loggear //
			.log(LoggingLevel.INFO, "*** E N D  S E R V I C E ***")

			;

		from("direct:validate")
			.choice()
				.when(simple("${headers.CamelHttpMethod} != 'POST'"))
					.throwException(new ExtendedException("Metodo POST obligatorio", 405, "405x001"))
				.when(simple("${headers.Content-Length} == 0"))
					.throwException(new ExtendedException("Archivo no recibido", 411, "411x002"))
				.when(simple("${headers.X-MessageType} == null"))
					.throwException(new ExtendedException("MessageType obligatorio", 400, "400x003"))
			.endChoice();

		from("direct:processFile")
			.choice()
				.when(simple("${headers.Content-Type} == 'application/octet-stream'"))
					.to("direct:writefullfile")
				.when(simple("${headers.Content-Type} startsWith 'multipart/form-data'"))
					.to("direct:writefile")
				.otherwise()
					.throwException(new ExtendedException("Content-Type incorrecto", 405, "405x002"))
			.endChoice()
			;

		from("direct:writefullfile")
			.to("file:/sdata/Incoming?fileExist=Append&charset=utf-8&appendChars=\\n");

		from("direct:writefile")
			.doTry()
				.to("direct:split")
			.doCatch(Exception.class)
				.log("CATCH")
			.end();

		from("direct:split")
			.split().tokenize("\r\n|\n").streaming()
				.stopOnException()
				// .log("${body}")
				.choice()
					.when(simple("${body.length} == 1 && ${exchangeProperty.CamelSplitIndex} > 3"))
						.throwException(new Exception("FINISH"))
					.when(simple("${bodyAs(String)} startsWith '--------------------------' && ${exchangeProperty.CamelSplitIndex} > 3"))
						.throwException(new Exception("FINISH"))
				.end()
				.filter(simple("${exchangeProperty.CamelSplitIndex} > 3"))
					.process(new DetectBomCharacter())
					.to("file:/sdata/Incoming?fileExist=Append&charset=utf-8&appendChars=\\n")
				.end()
			.end();

		from("direct:sendtopic")
			.process(new ProcessDestinations())
			.split().body()
				.log("END ${body}")
				.to("kafka:{{danone.kafka.topic.lb}}?brokers={{danone.kafka.broker}}")
			.end();

		from("direct:response")
			.setHeader(Exchange.HTTP_RESPONSE_CODE).constant(200)		
			.setBody().constant("{ \"success\": [ { \"status\": \"ok\" } ] }");

		from("direct:responseerror")
			.setHeader(Exchange.HTTP_RESPONSE_CODE).simple("${headers.X-ErrorStatus}")		
			.setBody().simple("{ \"errors\": [ { \"code\": \"${headers.X-ErrorCode}\", \"message\": \"${headers.X-ErrorMessage}\" } ] }")
			.removeHeaders("X-Error*");

		from("file:/etc/camel/resources?noop=true")
			.to("file:/sdata/resources");

	}

	public class ProcessDestinations implements Processor {
	    public ProcessDestinations() {
	    }

	    public void process(final Exchange exchange) throws Exception {

			String messageType = exchange.getMessage().getHeader("X-MessageType").toString();
			String path = "/sdata/Incoming";
			String filename = exchange.getMessage().getHeader("CamelFileName").toString();

	        List<String> ongoingMessages = new ArrayList<>();

        	ObjectMapper mapper = new ObjectMapper();

            JsonNode fulltree = mapper.readTree(new File("/sdata/resources/mapinfo.json"));
            JsonNode mtDefinition = fulltree.get(messageType);
            if (mtDefinition == null) {
            	throw(new ExtendedException("MessageType desconocido", 400, "400x004"));
            }
            for(JsonNode element : mtDefinition) {
            	String destination = element.get("destination").asText();

			    String body = "{ \"messageType\": \"" + messageType + "\", \"destination\": \"" + destination + "\", \"path\": \"" + path + "\", \"filename\": \"" + filename + "\" }";

			    ongoingMessages.add(body);  	        
            }            	

	        exchange.getIn().setBody(ongoingMessages);
	    }
	}

	public class ExtendedException extends Exception {

		private static final long serialVersionUID = 7718428512143223534L;
		
		public final int status;
		public final String code;

		public ExtendedException(String code) {
			super();
			this.code = code;
			this.status = 0;
		}

		public ExtendedException(String message, Throwable cause, String code) {
			super(message, cause);
			this.code = code;
			this.status = 0;
		}

		public ExtendedException(String message, int status, String code) {
			super(message);
			this.status = status;
			this.code = code;
		}

		public ExtendedException(Throwable cause, String code) {
			super(cause);
			this.code = code;
			this.status = 0;
		}
		
		public int getStatus() {
			return this.status;
		}

		public String getCode() {
			return this.code;
		}
	}	

	public static class DetectBomCharacter implements Processor {
		
		@Override
		public void process(Exchange exchange) throws Exception {
			String newBody = ((String)exchange.getIn().getBody()).replaceAll("\\uFEFF", "");
			exchange.getMessage().setBody((newBody)); 
		}

	}
}
